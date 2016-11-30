package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.backend.konan.llvm.KonanPlatform
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LazyClassReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltinOperatorDescriptorBase
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBinaryPrimitiveImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetterCallImpl
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperclassesWithoutAny
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isNullableNothing
import org.jetbrains.kotlin.types.typeUtil.isUnit

fun emitLLVM(module: IrModuleFragment, runtimeFile: String, outFile: String) {
    val llvmModule = LLVMModuleCreateWithName("out")!! // TODO: dispose
    val runtime = Runtime(runtimeFile) // TODO: dispose
    LLVMSetDataLayout(llvmModule, runtime.dataLayout)
    LLVMSetTarget(llvmModule, runtime.target)

    val context = Context(module, runtime, llvmModule) // TODO: dispose

    module.acceptVoid(RTTIGeneratorVisitor(context))
    println("\n--- Generate bitcode ------------------------------------------------------\n")
    module.acceptVoid(CodeGeneratorVisitor(context))
    verifyModule(llvmModule)
    LLVMWriteBitcodeToFile(llvmModule, outFile)
}

private fun verifyModule(llvmModule: LLVMModuleRef) {
    memScoped {
        val errorRef = allocPointerTo<CInt8Var>()
        // TODO: use LLVMDisposeMessage() on errorRef, once possible in interop.
        if (LLVMVerifyModule(
                llvmModule, LLVMVerifierFailureAction.LLVMPrintMessageAction, errorRef.ptr) == 1) {
            LLVMDumpModule(llvmModule)
            throw Error("Invalid module");
        }
    }
}

internal class RTTIGeneratorVisitor(context: Context) : IrElementVisitorVoid {
    val generator = RTTIGenerator(context)

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        super.visitClass(declaration)

        if (declaration.descriptor.kind == ClassKind.ANNOTATION_CLASS) {
            // do not generate any RTTI for annotation classes as a workaround for link errors
            return
        }

        if (declaration.descriptor.isIntrinsic) {
            // do not generate any code for intrinsic classes as they require special handling
            return
        }

        generator.generate(declaration.descriptor)
    }

}

//-------------------------------------------------------------------------//

internal class CodeGeneratorVisitor(val context: Context) : IrElementVisitorVoid {

    val codegen = CodeGenerator(context)
    val logger = Logger(codegen, context)
    val metadator = MetadataGenerator(context)

    //-------------------------------------------------------------------------//

    // TODO: consider eliminating mutable state
    private var currentCodeContext: CodeContext = TopLevelCodeContext

    /**
     * Defines how to generate context-dependent operations.
     */
    private interface CodeContext {

        fun genReturn(target: CallableDescriptor, value: LLVMValueRef?)

        fun genBreak(loop: IrLoop)

        fun genContinue(loop: IrLoop)

        fun genCall(function: LLVMValueRef, args: List<LLVMValueRef>, result: String?): LLVMValueRef

        fun genThrow(exception: LLVMValueRef)
    }

    /**
     * Fake [CodeContext] that doesn't support any operation.
     *
     * During function code generation [FunctionScope] should be set up.
     */
    private object TopLevelCodeContext : CodeContext {

        override fun genReturn(target: CallableDescriptor, value: LLVMValueRef?) = throw UnsupportedOperationException()

        override fun genBreak(loop: IrLoop) = throw UnsupportedOperationException()

        override fun genContinue(loop: IrLoop) = throw UnsupportedOperationException()

        override fun genCall(function: LLVMValueRef, args: List<LLVMValueRef>, result: String?)
                = throw UnsupportedOperationException()

        override fun genThrow(exception: LLVMValueRef) = throw UnsupportedOperationException()
    }

    /**
     * The [CodeContext] which can define some operations and delegate other ones to [outerContext]
     */
    private abstract class InnerScope(val outerContext: CodeContext) : CodeContext by outerContext

    /**
     * Convenient [InnerScope] implementation that is bound to the [currentCodeContext].
     */
    private abstract inner class InnerScopeImpl : InnerScope(currentCodeContext)

    /**
     * Executes [block] with [codeContext] substituted as [currentCodeContext].
     */
    private inline fun <R> using(codeContext: CodeContext?, block: () -> R): R {
        val oldCodeContext = currentCodeContext
        if (codeContext != null) {
            currentCodeContext = codeContext
        }
        try {
            return block()
        } finally {
            currentCodeContext = oldCodeContext
        }
    }

    //-------------------------------------------------------------------------//

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    //-------------------------------------------------------------------------//
    override fun visitModuleFragment(module: IrModuleFragment) {
        logger.log("visitModule                  : ${ir2string(module)}")
        module.acceptChildrenVoid(this)
        metadator.endModule(module)
    }

    //-------------------------------------------------------------------------//

    override fun visitWhen(expression: IrWhen) {
        logger.log("visitWhen                  : ${ir2string(expression)}")
        evaluateWhen("", expression)
    }

    //-------------------------------------------------------------------------//

    override fun visitLoop(loop: IrLoop) {
        TODO("${ir2string(loop)}")
    }

    //-------------------------------------------------------------------------//

    override fun visitWhileLoop(loop: IrWhileLoop) {
        val loopEnter = codegen.basicBlock()
        val loopBody  = codegen.basicBlock()
        val loopExit  = codegen.basicBlock()

        codegen.br(loopEnter)

        codegen.positionAtEnd(loopEnter)
        val condition = evaluateExpression(codegen.newVar(), loop.condition)
        codegen.condBr(condition, loopBody, loopExit)

        codegen.positionAtEnd(loopBody)
        evaluateExpression(codegen.newVar(), loop.body)

        codegen.br(loopEnter)
        codegen.positionAtEnd(loopExit)
    }

    //-------------------------------------------------------------------------//

    override fun visitDoWhileLoop(loop: IrDoWhileLoop) {
        val loopBody  = codegen.basicBlock()
        val loopCheck = codegen.basicBlock()
        val loopExit  = codegen.basicBlock()

        codegen.br(loopBody)

        codegen.positionAtEnd(loopBody)
        evaluateExpression(codegen.newVar(), loop.body)
        codegen.br(loopCheck)

        codegen.positionAtEnd(loopCheck)
        val condition = evaluateExpression(codegen.newVar(), loop.condition)
        codegen.condBr(condition, loopBody, loopExit)

        codegen.positionAtEnd(loopExit)
    }

    //-------------------------------------------------------------------------//

    override fun visitConstructor(constructorDeclaration: IrConstructor) {
        codegen.prologue(constructorDeclaration)
        val constructorDescriptor = constructorDeclaration.descriptor
        val classDescriptor = DescriptorUtils.getContainingClass(constructorDescriptor)

        using(FunctionScope(constructorDeclaration)) {

            /**
             * IR for kotlin.Any is:
             * BLOCK_BODY
             *   DELEGATING_CONSTRUCTOR_CALL 'constructor Any()'
             *   INSTANCE_INITIALIZER_CALL classDescriptor='Any'
             *
             *   to avoid possible recursion we manually reject body generation for Any.
             */

            if (!skipConstructorBodyGeneration(constructorDeclaration))
                constructorDeclaration.acceptChildrenVoid(this)

            if (constructorDescriptor.isPrimary && !DescriptorUtils.isCompanionObject(constructorDescriptor.constructedClass)) {
                val irOfCurrentClass = context.moduleIndex.classes[classDescriptor!!.classId]
                irOfCurrentClass!!.acceptChildrenVoid(object : IrElementVisitorVoid {
                    override fun visitElement(element: IrElement) {
                        element.acceptChildrenVoid(this)
                    }

                    override fun visitField(fieldDeclaration: IrField) {

                        val fieldDescriptor = fieldDeclaration.descriptor
                        val fieldPtr = fieldPtrOfClass(codegen.thisVariable(), fieldDescriptor)
                        var value: LLVMValueRef? = null
                        if (constructorDescriptor.valueParameters.any { it.name == fieldDescriptor.name }) {
                            val variable = codegen.variable(fieldDescriptor.name.asString())
                            value = codegen.load(variable!!, codegen.newVar())
                        } else {
                            value = evaluateExpression(codegen.newVar(), fieldDeclaration.initializer)
                        }
                        if (value != null)
                            codegen.store(value, fieldPtr!!)
                    }

                    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) {
                        evaluateExpression("", declaration.body)
                    }

                    override fun visitClass(declaration: IrClass) {
                        return
                    }

                    override fun visitConstructor(declaration: IrConstructor) {
                        return
                    }
                })
            }
        }

        if (KotlinBuiltIns.isArray(constructorDescriptor.returnType)) {
            val arrayPtr = codegen.bitcast(codegen.kArrayHeaderPtr, codegen.thisVariable(), codegen.newVar())
            codegen.ret(arrayPtr)
        } else {
            codegen.ret(codegen.thisVariable())
        }
        codegen.epilogue(constructorDeclaration)
        logger.log("visitConstructor            : ${ir2string(constructorDeclaration)}")
    }

    //-------------------------------------------------------------------------//

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) {
        logger.log("visitAnonymousInitializer  : ${ir2string(declaration)}")
    }

    //-------------------------------------------------------------------------//

    override fun visitBlockBody(body: IrBlockBody) {
        super.visitBlockBody(body)
        val function = codegen.currentFunction!!
        if (function !is ConstructorDescriptor && !codegen.isAfterTerminator()) {
            if (function.returnType!!.isUnit()) {
                codegen.ret(null)
            } else {
                codegen.unreachable()
            }
        }
        logger.log("visitBlockBody             : ${ir2string(body)}")
    }

    //-------------------------------------------------------------------------//

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) {
        evaluateCall(codegen.newVar(), expression)
    }

   //-------------------------------------------------------------------------//

    override fun visitCall(expression: IrCall) {
        logger.log("visitCall                  : ${ir2string(expression)}")
        val isUnit = KotlinBuiltIns.isUnit(expression.descriptor.returnType!!)
        val tmpVariable = if (isUnit) "" else codegen.newVar()
        evaluateExpression(tmpVariable, expression)
    }

    //-------------------------------------------------------------------------//

    override fun visitThrow(expression: IrThrow) {
        logger.log("visitThrow                 : ${ir2string(expression)}")
        evaluateExpression("", expression)
    }

    //-------------------------------------------------------------------------//

    override fun visitTry(expression: IrTry) {
        logger.log("visitTry                   : ${ir2string(expression)}")
        evaluateExpression("", expression)
    }

    //-------------------------------------------------------------------------//

    /**
     * The [CodeContext] enclosing the entire function body.
     */
    private inner class FunctionScope(val declaration: IrFunction) : InnerScopeImpl() {
        override fun genReturn(target: CallableDescriptor, value: LLVMValueRef?) {
            if (target == declaration.descriptor) {
                codegen.ret(value)
            } else {
                super.genReturn(target, value)
            }
        }

        override fun genCall(function: LLVMValueRef, args: List<LLVMValueRef>, result: String?)
                = codegen.call(function, args, result)

        override fun genThrow(exception: LLVMValueRef) {
            val objHeaderPtr = codegen.bitcast(codegen.kObjHeaderPtr, exception, codegen.newVar())
            val args = listOf(objHeaderPtr)
            codegen.call(context.throwExceptionFunction, args, "")
            codegen.unreachable()
        }
    }

    override fun visitFunction(declaration: IrFunction) {
        logger.log("visitFunction                  : ${ir2string(declaration)}")
        if (declaration.descriptor.modality == Modality.ABSTRACT || declaration.body == null)
            return

        codegen.prologue(declaration)
        metadator.function(declaration)

        using(FunctionScope(declaration)) {
            declaration.acceptChildrenVoid(this)
        }
        codegen.epilogue(declaration)

        verifyModule(context.llvmModule)
    }

    //-------------------------------------------------------------------------//

    override fun visitClass(declaration: IrClass) {
        logger.log("visitClass                  : ${ir2string(declaration)}")
        if (declaration.descriptor.kind == ClassKind.ANNOTATION_CLASS) {
            // do not generate any code for annotation classes as a workaround for NotImplementedError
            return
        }

        if (declaration.descriptor.isIntrinsic) {
            // do not generate any code for intrinsic classes as they require special handling
            return
        }

        super.visitClass(declaration)
    }

    //-------------------------------------------------------------------------//
    // Create new variable in LLVM ir

    override fun visitVariable(declaration: IrVariable) {
        logger.log("visitVariable              : ${ir2string(declaration)}")
        val variableName = declaration.descriptor.name.asString()
        val variableType = declaration.descriptor.type
        val value = evaluateExpression("", declaration.initializer)            // Generate initialization code.
        createVariable(declaration.descriptor, value)                          // Create and initialize variable.
    }

    private fun createVariable(descriptor: VariableDescriptor, value: LLVMValueRef? = null): LLVMValueRef {
        val name = descriptor.name.asString()
        val type = descriptor.type

        val newVariable = codegen.alloca(type, name)          // Create LLVM variable.
        codegen.registerVariable(name, newVariable)           // Map variableName -> LLVM variable.

        if (value != null) {
            codegen.store(value, newVariable)
        }
        return newVariable
    }

    //-------------------------------------------------------------------------//

    override fun visitReturn(expression: IrReturn) {
        logger.log("visitReturn                : ${ir2string(expression)}")
        evaluateExpression("", expression)
    }

    //-------------------------------------------------------------------------//

    override fun visitSetVariable(expression: IrSetVariable) {
        logger.log("visitSetVariable           : ${ir2string(expression)}")
        val value = evaluateExpression(codegen.newVar(), expression.value)
        codegen.store(value!!, codegen.variable(expression.descriptor.name.asString())!!)
    }

    //-------------------------------------------------------------------------//
    // top level setter for field
    override fun visitSetField(expression: IrSetField) {
        logger.log("visitSetField              : ${ir2string(expression)}")
        evaluateExpression("", expression)
    }

    //-------------------------------------------------------------------------//
    // top level getter for field:
    // normally GetField appears in following manner:
    //
    // PROPERTY private var globalValue: kotlin.Int
    //   FIELD PROPERTY_BACKING_FIELD private var globalValue: kotlin.Int
    //     EXPRESSION_BODY
    //       CONST Int type=kotlin.Int value='1'
    //   FUN DEFAULT_PROPERTY_ACCESSOR private fun <get-globalValue>(): kotlin.Int
    //     BLOCK_BODY
    //       RETURN type=kotlin.Nothing from='<get-globalValue>(): Int'
    //         GET_FIELD 'globalValue: Int' type=kotlin.Int origin=null
    //
    // and this case processed via evaluateExpression from RETURN processing,
    // where we process similar thing with generating temporal variable to return
    // alternatively top level IR, let's assume following IR:
    //
    // PROPERTY private var globalValue: kotlin.Int
    //   FUN DEFAULT_PROPERTY_ACCESSOR private fun <get-globalValue>(): kotlin.Int
    //     BLOCK_BODY
    //       GET_FIELD 'globalValue: Int' type=kotlin.Int origin=null
    //
    // in this situation our visitGetField will be charged, that why no
    // temporal variable is required.
    override fun visitGetField(expression: IrGetField) {
        logger.log("visitGetField              : ${ir2string(expression)}")
        evaluateExpression("", expression)
    }

    //-------------------------------------------------------------------------//

    override fun visitField(expression: IrField) {
        logger.log("visitField                 : ${ir2string(expression)}")
        val descriptor = expression.descriptor
        if (descriptor.containingDeclaration is PackageFragmentDescriptor) {
            val globalProperty = LLVMAddGlobal(context.llvmModule, codegen.getLLVMType(descriptor.type), descriptor.symbolName)
            LLVMSetInitializer(globalProperty, evaluateExpression("", expression.initializer))
            return
        }

        //super.visitField(expression)

    }

    //-------------------------------------------------------------------------//

    private fun evaluateExpression(tmpVariableName: String, value: IrElement?): LLVMValueRef? {
        when (value) {
            is IrSetterCallImpl      -> return evaluateSetterCall         (tmpVariableName, value)
            is IrTypeOperatorCall    -> return evaluateTypeOperator       (tmpVariableName, value)
            is IrCall                -> return evaluateCall               (tmpVariableName, value)
            is IrGetValue            -> return evaluateGetValue           (tmpVariableName, value)
            is IrSetVariable         -> return evaluateSetVariable        (                 value)
            is IrVariable            -> return evaluateVariable           (                 value)
            is IrGetField            -> return evaluateGetField           (                 value)
            is IrSetField            -> return evaluateSetField           (                 value)
            is IrConst<*>            -> return evaluateConst              (                 value)
            is IrReturn              -> return evaluateReturn             (                 value)
            is IrBlock               -> return evaluateBlock              (tmpVariableName, value)
            is IrExpressionBody      -> return evaluateExpression         (tmpVariableName, value.expression)
            is IrWhen                -> return evaluateWhen               (tmpVariableName, value)
            is IrThrow               -> return evaluateThrow              (tmpVariableName, value)
            is IrTry                 -> return evaluateTry                (                 value)
            is IrStringConcatenation -> return evaluateStringConcatenation(tmpVariableName, value)
            is IrBlockBody           -> return evaluateBlock              (tmpVariableName, value as IrStatementContainer)
            is IrWhileLoop           -> return evaluateWhileLoop          (                 value)
            null                     -> return null
            else                     -> {
                TODO("${ir2string(value)}")
            }
        }
    }

    private fun evaluateExpressionAndJump(expression: IrExpression, destination: ContinuationBlock) {
        val result = evaluateExpression("", expression)

        if (!codegen.isAfterTerminator()) {
            jump(destination, result)
        } else {
            // Workaround for unreachable code handling.
            // Note: cannot rely on checking that expression type is Nothing.
        }
    }

    //-------------------------------------------------------------------------//

    /**
     * Represents the basic block which may expect a value:
     * when generating a [jump] to this block, one should provide the value.
     * Inside the block that value is accessible as [valuePhi].
     *
     * This class is designed to be used to generate Kotlin expressions that have a value and require branching.
     *
     * [valuePhi] may be `null`, which would mean that no value is passed.
     */
    private data class ContinuationBlock(val block: LLVMBasicBlockRef, val valuePhi: LLVMValueRef?)

    /**
     * Jumps to [target] passing [value].
     */
    private fun jump(target: ContinuationBlock, value: LLVMValueRef?) {
        val entry = target.block
        codegen.br(entry)
        if (target.valuePhi != null) {
            codegen.assignPhis(target.valuePhi to value!!)
        }
    }

    /**
     * Creates new [ContinuationBlock] and generates [code] starting from its beginning.
     */
    private inline fun continuationBlock(type: LLVMTypeRef, code: (LLVMValueRef) -> Unit): ContinuationBlock {
        val entry = codegen.basicBlock()

        codegen.appendingTo(entry) {
            val valuePhi = codegen.phi(type, codegen.newVar())
            code(valuePhi)
            return ContinuationBlock(entry, valuePhi)
        }
    }

    /**
     * Creates new [ContinuationBlock] that don't receive the value and generates [code] starting from its beginning.
     */
    private inline fun continuationBlock(code: () -> Unit): ContinuationBlock {
        val block = codegen.basicBlock()

        codegen.appendingTo(block) {
            code()
        }

        return ContinuationBlock(block, null)
    }

    /**
     * Creates new [ContinuationBlock] that receives the value of type corresponding to given Kotlin type
     * and generates [code] starting from its beginning.
     */
    private fun continuationBlock(type: KotlinType, code: (LLVMValueRef?) -> Unit = {}): ContinuationBlock {
        return if (type.isUnitOrNothing()) {
            continuationBlock {
                code(null)
            }
        } else {
            continuationBlock(codegen.getLLVMType(type), code)
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateStringConcatenation(tmpVariableName: String, value: IrStringConcatenation): LLVMValueRef? {
        val stringPlus = KonanPlatform.builtIns.stringType.memberScope.getContributedFunctions(Name.identifier("plus"), NoLookupLocation.FROM_BACKEND).first()
        var res:LLVMValueRef? = null
        val strings:List<LLVMValueRef> = value.arguments.map {
            val descriptor = getToString(it.type)

            val args = listOf(evaluateExpression(codegen.newVar(), it)!!)
            return@map evaluateSimpleFunctionCall(codegen.newVar(), descriptor, args)
        }
        val concatResult = strings.take(1).fold(strings.first()) {res, it -> evaluateSimpleFunctionCall(codegen.newVar(), stringPlus, listOf(res, it))}
        return concatResult
    }


    //-------------------------------------------------------------------------//

    private fun evaluateThrow(tmpVariableName: String, expression: IrThrow): LLVMValueRef? {
        val exception = evaluateExpression(codegen.newVar(), expression.value)!!
        currentCodeContext.genThrow(exception)
        return null
    }

    //-------------------------------------------------------------------------//

    /**
     * The [CodeContext] that catches exceptions.
     */
    private inner abstract class CatchingScope : InnerScopeImpl() {

        /**
         * The LLVM `landingpad` such that if invoked function throws an exception,
         * then this exception is passed to [handler].
         */
        private val landingpad: LLVMBasicBlockRef by lazy {
            using(outerContext) {
                codegen.basicBlock("landingpad") {
                    genLandingpad()
                }
            }
        }

        /**
         * The Kotlin exception handler, i.e. the [ContinuationBlock] which gets started
         * when the exception is caught, receiving this exception as its value.
         */
        private val handler by lazy {
            using(outerContext) {
                continuationBlock(codegen.kObjHeaderPtr) { exception ->
                    genHandler(exception)
                }
            }
        }

        private fun jumpToHandler(exception: LLVMValueRef) {
            jump(this.handler, exception)
        }

        /**
         * Generates the LLVM `landingpad` that catches C++ exception with type `KotlinException`,
         * unwraps the Kotlin exception object and jumps to [handler].
         *
         * This method generates nearly the same code as `clang++` does for the following:
         * ```
         * catch (KotlinException& e) {
         *     KRef exception = e.exception_;
         *     return exception;
         * }
         * ```
         * except that our code doesn't check exception `typeid`.
         *
         * TODO: why does `clang++` check `typeid` even if there is only one catch clause?
         */
        private fun genLandingpad() {
            with(codegen) {
                // Type of `landingpad` instruction result (depends on personality function):
                val landingpadType = structType(int8TypePtr, int32Type)

                val personalityFunction = externalFunction("__gxx_personality_v0", functionType(int32Type, true))
                val personalityFunctionRaw = LLVMConstBitCast(personalityFunction, int8TypePtr)!!

                val numClauses = 1
                val landingpadResult =
                        LLVMBuildLandingPad(codegen.builder, landingpadType, personalityFunctionRaw, numClauses, "lp")

                // Configure landingpad to catch C++ exception with type `KotlinException`:
                LLVMAddClause(landingpadResult, externalGlobal("_ZTI15KotlinException", int8Type))

                // FIXME: properly handle C++ exceptions: currently C++ exception can be thrown out from try-finally
                // bypassing the finally block.

                val exceptionRecord = LLVMBuildExtractValue(codegen.builder, landingpadResult, 0, "er")

                // __cxa_begin_catch returns pointer to C++ exception object.
                val beginCatch = externalFunction("__cxa_begin_catch", functionType(int8TypePtr, false, int8TypePtr))
                val exceptionRawPtr = call(beginCatch, listOf(exceptionRecord), "")

                // Pointer to KotlinException instance:
                val exceptionPtrPtr = bitcast(codegen.kObjHeaderPtrPtr, exceptionRawPtr, "")

                // Pointer to Kotlin exception object:
                val exceptionPtr = load(exceptionPtrPtr, "exception")

                // __cxa_end_catch performs some C++ cleanup, including calling `KotlinException` class destructor.
                val endCatch = externalFunction("__cxa_end_catch", functionType(voidType, false))
                call(endCatch, listOf(), "")

                jumpToHandler(exceptionPtr)
            }
        }

        // The call inside [CatchingScope] must be configured to dispatch exception to the scope's handler.
        override fun genCall(function: LLVMValueRef, args: List<LLVMValueRef>, result: String?): LLVMValueRef {
            val then = codegen.basicBlock()
            val res = codegen.invoke(function, args, then, landingpad, result)
            codegen.positionAtEnd(then)
            return res
        }

        override fun genThrow(exception: LLVMValueRef) {
            jumpToHandler(exception)
        }

        protected abstract fun genHandler(exception: LLVMValueRef)
    }

    /**
     * The [CatchingScope] that handles exceptions using Kotlin `catch` clauses.
     *
     * @param success the block to be used when the exception is successfully handled;
     * expects `catch` expression result as its value.
     */
    private inner class CatchScope(private val catches: List<IrCatch>,
                                   private val success: ContinuationBlock) : CatchingScope() {

        override fun genHandler(exception: LLVMValueRef) {
            // TODO: optimize for `Throwable` clause.
            catches.forEach {
                val isInstance = genInstanceOf("", exception, it.parameter.type)
                val body = codegen.basicBlock("catch")
                val nextCheck = codegen.basicBlock("catchCheck")
                codegen.condBr(isInstance, body, nextCheck)

                codegen.appendingTo(body) {
                    createVariable(it.parameter, exception)
                    evaluateExpressionAndJump(it.result, success)
                }

                codegen.positionAtEnd(nextCheck)
            }
            // rethrow the exception if no clause can handle it.
            outerContext.genThrow(exception)
        }
    }

    /**
     * The [InnerScope] that includes code generated by [genFinalize] when leaving it
     * with `return`, `break`, `continue` or throwing exception.
     */
    private abstract inner class FinalizingScope() : CatchingScope() {

        /**
         * Cache for [genReturn].
         *
         * `returnBlocks[func]` contains the [ContinuationBlock] to be used for `return` from `func`;
         * the block expects return value as its value.
         */
        private val returnBlocks = mutableMapOf<CallableDescriptor, ContinuationBlock>()

        // Jump to finalize-and-return instead of simply returning.
        override fun genReturn(target: CallableDescriptor, value: LLVMValueRef?) {
            val block = returnBlocks.getOrPut(target) {
                using(outerContext) {
                    continuationBlock(target.returnType!!) { returnValue ->
                        genFinalize()
                        outerContext.genReturn(target, returnValue)
                    }
                }
            }

            jump(block, value)
        }

        override fun genBreak(loop: IrLoop) = TODO()
        override fun genContinue(loop: IrLoop) = TODO()

        // When an exception is caught, finalize the scope and rethrow the exception.
        override fun genHandler(exception: LLVMValueRef) {
            genFinalize()
            outerContext.genThrow(exception)
        }

        protected abstract fun genFinalize()
    }

    /**
     * Generates the code which gets "finalized" exactly once when completed either normally or abnormally.
     *
     * @param code generates the code to be post-dominated by cleanup.
     * It must jump to given [ContinuationBlock] with its result when completed normally.
     *
     * @param finalize generates the cleanup code that must be executed when code generated by [code] is completed.
     *
     * @param type Kotlin type of the result of generated code.
     *
     * @return the result of the generated code.
     */
    private fun genFinalizedBy(finalize: (() -> Unit)?,
                                   type: KotlinType, code: (ContinuationBlock) -> Unit): LLVMValueRef? {

        val scope = if (finalize == null) null else {
            object : FinalizingScope() {
                override fun genFinalize() {
                    finalize()
                }
            }
        }

        val continuation = continuationBlock(type)

        using(scope) {
            code(continuation)
        }
        codegen.positionAtEnd(continuation.block)
        finalize?.invoke()

        // TODO: finalize is duplicated many times (just as in C++, Java or Kotlin JVM);
        // it is very important to optimize this.

        return continuation.valuePhi
    }

    /**
     * Generates code that is "finalized" by given expression.
     */
    private fun genFinalizedBy(finalize: IrExpression?, type: KotlinType,
                                   code: (ContinuationBlock) -> Unit): LLVMValueRef? {

        return genFinalizedBy({ evaluateExpression("", finalize) }, type, code)
    }

    private fun KotlinType.isUnitOrNothing() = isUnit() || isNothing()

    private fun evaluateTry(expression: IrTry): LLVMValueRef? {
        // TODO: does basic block order influence machine code order?
        // If so, consider reordering blocks to reduce exception tables size.

        return genFinalizedBy(expression.finallyExpression, expression.type) { continuation ->

            val catchScope = if (expression.catches.isEmpty()) null else CatchScope(expression.catches, continuation)

            using(catchScope) {
                evaluateExpressionAndJump(expression.tryResult, continuation)
            }
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateWhen(tmpVariableName:String, expression: IrWhen): LLVMValueRef? {
        logger.log("evaluateWhen               : ${ir2string(expression)}")
        var bbExit: LLVMBasicBlockRef? = null             // By default "when" does not have "exit".
        if (!KotlinBuiltIns.isNothing(expression.type))     // If "when" has "exit".
            bbExit = codegen.basicBlock()                   // Create basic block to process "exit".

        val isForceUnit           = tmpVariableName.isEmpty()
        val isUnit                = KotlinBuiltIns.isUnit(expression.type) || isForceUnit
        val isNothing             = KotlinBuiltIns.isNothing(expression.type)
        val neitherUnitNorNothing = !isNothing && !isUnit
        val tmpVariable           = if (!isUnit) codegen.newVar() else null
        val tmpLlvmVariablePtr    = if (!isUnit) codegen.alloca(expression.type, tmpVariable!!) else null

        expression.branches.forEach {                       // Iterate through "when" branches (clauses).
            var bbNext = bbExit                             // For last clause bbNext coincides with bbExit.
            if (it != expression.branches.last())           // If it is not last clause.
                bbNext = codegen.basicBlock()               // Create new basic block for next clause.
            val isUnitBranch = KotlinBuiltIns.isUnit(it.result.type) || isForceUnit
            val isNothingBranch = KotlinBuiltIns.isNothing(it.result.type)
            generateWhenCase(isUnitBranch, isNothingBranch, tmpLlvmVariablePtr, it, bbNext, bbExit) // Generate code for current clause.
        }
        if  (neitherUnitNorNothing)                                      // If result hasn't Unit type and block doesn't end with return
            return codegen.load(tmpLlvmVariablePtr!!, tmpVariableName)   // load value from variable.
        return null
    }

    private fun evaluateWhileLoop(loop: IrWhileLoop): LLVMValueRef? {
        visitWhileLoop(loop)
        // TODO: incorrect!
        return null
    }

    //-------------------------------------------------------------------------//

    private fun evaluateGetValue(tmpVariableName: String, value: IrGetValue): LLVMValueRef {
        logger.log("evaluateGetValue           : $tmpVariableName = ${ir2string(value)}")
        when (value.descriptor) {
            is LocalVariableDescriptor,
            is IrTemporaryVariableDescriptor,
            is ValueParameterDescriptor -> {
                val variable = codegen.variable(value.descriptor.name.asString())
                return codegen.load(variable!!, tmpVariableName)
            }
            is LazyClassReceiverParameterDescriptor,
            is ReceiverParameterDescriptor -> {
                if (value.descriptor.name.asString() == "<this>") {
                    return codegen.thisVariable()
                }
                TODO("${ir2string(value)}")
            }
            else -> {
                TODO("${ir2string(value)}")
            }
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateSetVariable(value: IrSetVariable): LLVMValueRef {
        logger.log("evaluateSetVariable        : ${ir2string(value)}")
        val ret = evaluateExpression(codegen.newVar(), value.value)
        return codegen.store(ret!!, codegen.variable(value.descriptor.name.asString())!!)
    }

    //-------------------------------------------------------------------------//

    private fun evaluateVariable(value: IrVariable): LLVMValueRef {
        logger.log("evaluateVariable           : ${ir2string(value)}")
        val ret = evaluateExpression(codegen.newVar(), value.initializer)
        val variableName = value.descriptor.name.asString()
        val variable = codegen.alloca(LLVMTypeOf(ret), variableName)
        codegen.registerVariable(variableName, variable)
        return codegen.store(ret!!, variable)
    }

    //-------------------------------------------------------------------------//

    private fun evaluateTypeOperator(tmpVariableName: String, value: IrTypeOperatorCall): LLVMValueRef? {
        when (value.operator) {
            IrTypeOperator.CAST                      -> return evaluateCast(tmpVariableName, value)
            IrTypeOperator.IMPLICIT_CAST             -> return evaluateExpression(tmpVariableName, value.argument)
            IrTypeOperator.IMPLICIT_NOTNULL          -> TODO("${ir2string(value)}")
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> return evaluateExpression("", value.argument)
            IrTypeOperator.SAFE_CAST                 -> return evaluateSafeCast(tmpVariableName, value)
            IrTypeOperator.INSTANCEOF                -> return evaluateInstanceOf(tmpVariableName, value)
            IrTypeOperator.NOT_INSTANCEOF            -> return evaluateNotInstanceOf(tmpVariableName, value)
        }

    }

    //-------------------------------------------------------------------------//

    private fun evaluateSafeCast(tmpVariableName: String, value: IrTypeOperatorCall): LLVMValueRef? {
        val bbTrue  = codegen.basicBlock()!!
        val bbFalse = codegen.basicBlock()!!
        val bbExit  = codegen.basicBlock()!!
        val result  = codegen.alloca(codegen.getLLVMType(value.type), codegen.newVar())

        val condition = evaluateInstanceOf(codegen.newVar(), value)
        codegen.condBr(condition, bbTrue, bbFalse)

        codegen.positionAtEnd(bbTrue)
        val castResult = evaluateExpression(codegen.newVar(), value.argument)
        codegen.store(castResult!!, result)
        codegen.br(bbExit)

        codegen.positionAtEnd(bbFalse)
        val nullResult = if (KotlinBuiltIns.isArray(value.type)) codegen.kNullArrayHeaderPtr
                         else codegen.kNullObjHeaderPtr
        codegen.store(nullResult, result)
        codegen.br(bbExit)

        codegen.positionAtEnd(bbExit)
        return codegen.load(result, tmpVariableName)
    }

    //-------------------------------------------------------------------------//
    //   table of conversion with llvm for primitive types
    //   to be used in replacement fo primitive.toX() calls with
    //   translator intrinsics.
    //            | byte     short   int     long     float     double
    //------------|----------------------------------------------------
    //    byte    |   x       sext   sext    sext     sitofp    sitofp
    //    short   | trunc      x     sext    sext     sitofp    sitofp
    //    int     | trunc    trunc    x      sext     sitofp    sitofp
    //    long    | trunc    trunc   trunc     x      sitofp    sitofp
    //    float   | fptosi   fptosi  fptosi  fptosi      x      fpext
    //    double  | fptosi   fptosi  fptosi  fptosi   fptrunc      x

    private fun evaluateCast(tmpVariableName: String, value: IrTypeOperatorCall): LLVMValueRef {
        logger.log("evaluateCast               : ${ir2string(value)}")
        assert(!KotlinBuiltIns.isPrimitiveType(value.type) && !KotlinBuiltIns.isPrimitiveType(value.argument.type))

        val dstDescriptor = TypeUtils.getClassDescriptor(value.type)                   // Get class descriptor for dst type.
        val dstTypeInfo   = codegen.typeInfoValue(dstDescriptor!!)                     // Get TypeInfo for dst type.
        val srcArg        = evaluateExpression(codegen.newVar(), value.argument)!!     // Evaluate src expression.
        val srcObjInfoPtr = codegen.bitcast(codegen.kObjHeaderPtr, srcArg, codegen.newVar())   // Cast src to ObjInfoPtr.
        val args          = listOf(srcObjInfoPtr, dstTypeInfo)                         // Create arg list.
        currentCodeContext.genCall(context.checkInstanceFunction, args, "")            // Check if dst is subclass of src.
        return srcArg
    }

    //-------------------------------------------------------------------------//

    private fun evaluateInstanceOf(tmpVariableName: String, value: IrTypeOperatorCall): LLVMValueRef {
        logger.log("evaluateInstanceOf         : ${ir2string(value)}")

        val type     = value.typeOperand
        val srcArg   = evaluateExpression(codegen.newVar(), value.argument)!!     // Evaluate src expression.

        return genInstanceOf(tmpVariableName, srcArg, type)
    }

    private fun genInstanceOf(tmpVariableName: String, obj: LLVMValueRef, type: KotlinType): LLVMValueRef {
        val dstDescriptor = TypeUtils.getClassDescriptor(type)                         // Get class descriptor for dst type.
        val dstTypeInfo   = codegen.typeInfoValue(dstDescriptor!!)                     // Get TypeInfo for dst type.
        val srcObjInfoPtr = codegen.bitcast(codegen.kObjHeaderPtr, obj, codegen.newVar())   // Cast src to ObjInfoPtr.
        val args          = listOf(srcObjInfoPtr, dstTypeInfo)                         // Create arg list.

        val result = currentCodeContext.genCall(                                       // Check if dst is subclass of src.
                context.isInstanceFunction, args, codegen.newVar())

        return LLVMBuildTrunc(codegen.builder, result, kInt1, tmpVariableName)!!       // Truncate result to boolean
    }

    //-------------------------------------------------------------------------//

    private fun evaluateNotInstanceOf(tmpVariableName: String, value: IrTypeOperatorCall): LLVMValueRef {
        val instanceOfResult = evaluateInstanceOf(codegen.newVar(), value)
        return LLVMBuildNot(codegen.builder, instanceOfResult, tmpVariableName)!!
    }

    //-------------------------------------------------------------------------//

    private fun evaluateGetField(value: IrGetField): LLVMValueRef {
        logger.log("evaluateGetField           : ${ir2string(value)}")
        if (value.descriptor.dispatchReceiverParameter != null) {
            val thisPtr = codegen.thisVariable()
            return codegen.load(fieldPtrOfClass(thisPtr, value.descriptor)!!, codegen.newVar())
        }
        else {
            val ptr = LLVMGetNamedGlobal(context.llvmModule, value.descriptor.symbolName)!!
            return codegen.load(ptr, codegen.newVar())
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateSetField(value: IrSetField): LLVMValueRef {
        logger.log("evaluateSetField           : ${ir2string(value)}")
        val valueToAssign = evaluateExpression(codegen.newVar(), value.value)!!
        if (value.descriptor.dispatchReceiverParameter != null) {
            val thisPtr = codegen.thisVariable()
            return codegen.store(valueToAssign, fieldPtrOfClass(thisPtr, value.descriptor)!!)
        }
        else {
            val globalValue = LLVMGetNamedGlobal(context.llvmModule, value.descriptor.symbolName)
            return codegen.store(valueToAssign, globalValue!!)
        }
    }

    //-------------------------------------------------------------------------//

    /*
       in C:
       struct ObjHeader *header = (struct ObjHeader *)ptr;
       struct T* obj = (T*)&header[1];
       return &obj->fieldX;

       in llvm ir:
       %struct.ObjHeader = type { i32, i32 }
       %struct.Object = type { i32, i32 }

       ; Function Attrs: nounwind ssp uwtable
       define i32 @fooField2(i8*) #0 {
         %2 = alloca i8*, align 8
         %3 = alloca %struct.ObjHeader*, align 8
         %4 = alloca %struct.Object*, align 8
         store i8* %0, i8** %2, align 8
         %5 = load i8*, i8** %2, align 8
         %6 = bitcast i8* %5 to %struct.ObjHeader*
         store %struct.ObjHeader* %6, %struct.ObjHeader** %3, align 8
         %7 = load %struct.ObjHeader*, %struct.ObjHeader** %3, align 8

         %8 = getelementptr inbounds %struct.ObjHeader, %struct.ObjHeader* %7, i64 1; <- (T*)&header[1];

         %9 = bitcast %struct.ObjHeader* %8 to %struct.Object*
         store %struct.Object* %9, %struct.Object** %4, align 8
         %10 = load %struct.Object*, %struct.Object** %4, align 8
         %11 = getelementptr inbounds %struct.Object, %struct.Object* %10, i32 0, i32 0 <-  &obj->fieldX
         %12 = load i32, i32* %11, align 4
         ret i32 %12
       }

    */
    private fun fieldPtrOfClass(thisPtr: LLVMValueRef, value: PropertyDescriptor): LLVMValueRef? {
        val objHeaderPtr = codegen.bitcast(codegen.kObjHeaderPtr, thisPtr, codegen.newVar())
        val typePtr = pointerType(codegen.classType(value.containingDeclaration as ClassDescriptor))
        memScoped {
            val args = allocArrayOf(kImmOne)
            val objectPtr = LLVMBuildGEP(codegen.builder, objHeaderPtr,  args[0].ptr, 1, codegen.newVar())
            val typedObjPtr = codegen.bitcast(typePtr, objectPtr!!, codegen.newVar())
            val fieldPtr = LLVMBuildStructGEP(codegen.builder, typedObjPtr, codegen.indexInClass(value), codegen.newVar())
            return fieldPtr
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateConst(value: IrConst<*>): LLVMValueRef? {
        logger.log("evaluateConst              : ${ir2string(value)}")
        when (value.kind) {
            IrConstKind.Null    -> return codegen.kNullObjHeaderPtr
            IrConstKind.Boolean -> when (value.value) {
                true  -> return kTrue
                false -> return kFalse
            }
            IrConstKind.Char   -> return LLVMConstInt(LLVMInt16Type(), (value.value as Char).toLong(),  0)
            IrConstKind.Byte   -> return LLVMConstInt(LLVMInt8Type(),  (value.value as Byte).toLong(),  1)
            IrConstKind.Short  -> return LLVMConstInt(LLVMInt16Type(), (value.value as Short).toLong(), 1)
            IrConstKind.Int    -> return LLVMConstInt(LLVMInt32Type(), (value.value as Int).toLong(),   1)
            IrConstKind.Long   -> return LLVMConstInt(LLVMInt64Type(), value.value as Long,             1)
            IrConstKind.String ->
                return context.staticData.kotlinStringLiteral(value as IrConst<String>).getLlvmValue()
            IrConstKind.Float  -> return LLVMConstRealOfString(LLVMFloatType(), (value.value as Float).toString())
            IrConstKind.Double -> return LLVMConstRealOfString(LLVMDoubleType(), (value.value as Double).toString())
        }
        TODO("${ir2string(value)}")
    }

    //-------------------------------------------------------------------------//

    private fun evaluateReturn(expression: IrReturn): LLVMValueRef? {
        logger.log("evaluateReturn             : ${ir2string(expression)}")
        val value = expression.value

        val evaluated = if (value is IrGetObjectValue && value.type.isUnit()) {
            // hack to make "return without value" work
            null
        } else {
            evaluateExpression(codegen.newVar(), value)
        }

        val ret = if (!value.type.isUnit()) {
            evaluated
        } else {
            null // hack for bad Unit type handling
        }

        currentCodeContext.genReturn(expression.returnTarget, ret)
        return null
    }

    //-------------------------------------------------------------------------//

    private fun evaluateBlock(tmpVariableName: String, value: IrStatementContainer): LLVMValueRef? {
        logger.log("evaluateBlock              : ${value.statements.forEach { ir2string(it) }}")
        value.statements.dropLast(1).forEach {
            evaluateExpression(codegen.newVar(), it)
        }
        return evaluateExpression(tmpVariableName, value.statements.lastOrNull())
    }

    //-------------------------------------------------------------------------//

    private fun evaluateCall(tmpVariableName: String, value: IrMemberAccessExpression?): LLVMValueRef {
        logger.log("evaluateCall               : $tmpVariableName = ${ir2string(value)}")
        val args = mutableListOf<LLVMValueRef>()                            // Create list of function args.
        value!!.acceptChildrenVoid(object: IrElementVisitorVoid {               // Iterate args of the function.
            override fun visitElement(element: IrElement) {                     // Visit arg.
                val tmp = codegen.newVar()                                      // Create variable representing the arg in codegen
                args.add(evaluateExpression(tmp, element as IrExpression)!!)    // Evaluate expression and get LLVM arg
            }
        })

        when {
            value is IrDelegatingConstructorCall -> return superCall(tmpVariableName, value.descriptor, args)
            value.descriptor is FunctionDescriptor -> return evaluateFunctionCall(tmpVariableName, value as IrCall, args)
            else -> {
                TODO("${ir2string(value)}")
            }
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateFunctionCall(tmpVariableName: String, callee: IrCall, args: List<LLVMValueRef>): LLVMValueRef {
        val descriptor:FunctionDescriptor = callee.descriptor as FunctionDescriptor
        when (descriptor) {
            is IrBuiltinOperatorDescriptorBase -> return evaluateOperatorCall      (tmpVariableName, callee,     args)
            is ClassConstructorDescriptor      -> return evaluateConstructorCall   (tmpVariableName, callee,     args)
            else                               -> return evaluateSimpleFunctionCall(tmpVariableName, descriptor, args)
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateSimpleFunctionCall(tmpVariableName: String, descriptor: FunctionDescriptor, args: List<LLVMValueRef>): LLVMValueRef {
        //logger.log("evaluateSimpleFunctionCall : $tmpVariableName = ${ir2string(value)}")
        if (descriptor.isOverridable)
            return callVirtual(descriptor, args, tmpVariableName)
        else
            return callDirect(descriptor, args, tmpVariableName)
    }

    //-------------------------------------------------------------------------//

    private fun evaluateSetterCall(tmpVariableName: String, value: IrSetterCallImpl): LLVMValueRef? {
        val descriptor = value.descriptor as FunctionDescriptor
        val args       = mutableListOf<LLVMValueRef>()
        if (descriptor.dispatchReceiverParameter != null)
            args.add(evaluateExpression(codegen.newVar(), value.dispatchReceiver)!!)         //add this ptr
        args.add(evaluateExpression(codegen.newVar(), value.getValueArgument(0))!!)
        return evaluateSimpleFunctionCall(tmpVariableName, descriptor, args)
    }

    //-------------------------------------------------------------------------//

    private fun evaluateConstructorCall(variableName: String, callee: IrCall, args: List<LLVMValueRef>): LLVMValueRef {
        logger.log("evaluateConstructorCall    : $variableName = ${ir2string(callee)}")
        memScoped {
            val containingClass = (callee.descriptor as ClassConstructorDescriptor).containingDeclaration
            val typeInfo = codegen.typeInfoValue(containingClass)
            val allocHint = Int32(1).getLlvmValue()!!
            val thisValue = if (containingClass.isArray) {
                assert(args.size == 1 && LLVMTypeOf(args[0]) == LLVMInt32Type())
                val allocArrayInstanceArgs = listOf(typeInfo, allocHint, args[0])
                val ptr = currentCodeContext.genCall(context.allocArrayFunction, allocArrayInstanceArgs, variableName)
                codegen.bitcast(codegen.kObjHeaderPtr, ptr, codegen.newVar())
            } else {
                currentCodeContext.genCall(context.allocInstanceFunction, listOf(typeInfo, allocHint), variableName)
            }
            val constructorParams: MutableList<LLVMValueRef> = mutableListOf()
            constructorParams += thisValue
            constructorParams += args
            return evaluateSimpleFunctionCall(variableName, callee.descriptor as FunctionDescriptor, constructorParams)
        }
    }

    //-------------------------------------------------------------------------//
    private val kEqeq        = Name.identifier("EQEQ")
    private val ktEqeqeq     = Name.identifier("EQEQEQ")
    private val kGt0         = Name.identifier("GT0")
    private val kGteq0       = Name.identifier("GTEQ0")
    private val kLt0         = Name.identifier("LT0")
    private val kLteq0       = Name.identifier("LTEQ0")
    private val kNot         = Name.identifier("NOT")
    private val kImmZero     = LLVMConstInt(LLVMInt32Type(),  0, 1)!!
    private val kImmOne      = LLVMConstInt(LLVMInt32Type(),  1, 1)!!
    private val kTrue        = LLVMConstInt(LLVMInt1Type(),   1, 1)!!
    private val kFalse       = LLVMConstInt(LLVMInt1Type(),   0, 1)!!


    private fun evaluateOperatorCall(tmpVariableName: String, callee: IrCall, args: List<LLVMValueRef?>): LLVMValueRef {
        logger.log("evaluateCall               : $tmpVariableName origin:${ir2string(callee)}")
        val descriptor = callee.descriptor
        when (descriptor.name) {
            kEqeq    -> return evaluateOperatorEqeq  (callee as IrBinaryPrimitiveImpl, args[0]!!, args[1]!!, tmpVariableName)
            ktEqeqeq -> return evaluateOperatorEqeqeq(callee as IrBinaryPrimitiveImpl, args[0]!!, args[1]!!, tmpVariableName)
            kGt0     -> return codegen.icmpGt(args[0]!!, kImmZero, tmpVariableName)
            kGteq0   -> return codegen.icmpGe(args[0]!!, kImmZero, tmpVariableName)
            kLt0     -> return codegen.icmpLt(args[0]!!, kImmZero, tmpVariableName)
            kLteq0   -> return codegen.icmpLe(args[0]!!, kImmZero, tmpVariableName)
            kNot     -> return codegen.icmpNe(args[0]!!, kTrue,    tmpVariableName)
            else -> {
                TODO(descriptor.name.toString())
            }
        }
    }

    //-------------------------------------------------------------------------//

    private fun getEquals(type: KotlinType, methodName: String) : SimpleFunctionDescriptor {

        val name = Name.identifier(methodName)
        val descriptors = type.memberScope.getContributedFunctions(name, NoLookupLocation.FROM_BACKEND).filter {
            it.valueParameters.size == 1 && KotlinBuiltIns.isAnyOrNullableAny(it.valueParameters[0].type)
        }
        assert(descriptors.size == 1)
        val descriptor = descriptors.first()
        return descriptor
    }

    //-------------------------------------------------------------------------//

    private fun getToString(type: KotlinType): SimpleFunctionDescriptor {
        val name = Name.identifier("toString")
        val descriptor = type.memberScope.getContributedFunctions(name, NoLookupLocation.FROM_BACKEND).first()
        return descriptor
    }

    //-------------------------------------------------------------------------//

    private fun evaluateOperatorEqeq(callee: IrBinaryPrimitiveImpl, arg0: LLVMValueRef, arg1: LLVMValueRef, tmpVariableName: String): LLVMValueRef {

        val arg0Type = callee.argument0.type
        val isFp = KotlinBuiltIns.isDouble(arg0Type) || KotlinBuiltIns.isFloat(arg0Type)
        val isInt = KotlinBuiltIns.isPrimitiveType(arg0Type) && !isFp
        when {
            isFp  -> return codegen.fcmpEq(arg0, arg1, tmpVariableName)
            isInt -> return codegen.icmpEq(arg0, arg1, tmpVariableName)
            else  -> return generateEqeqForObjects(callee, arg0, arg1, tmpVariableName)
        }
    }

    //-------------------------------------------------------------------------//

    private fun generateEqeqForObjects(callee: IrBinaryPrimitiveImpl, arg0: LLVMValueRef, arg1: LLVMValueRef, tmpVariableName: String): LLVMValueRef {
        val arg0Type = callee.argument0.type
        val descriptor = getEquals(arg0Type, "equals")                          // Get descriptor for "arg0.equals".
        if (arg0Type.isMarkedNullable) {                                        // If arg0 is nullable.
            val bbEq2  = codegen.basicBlock()                                   // Block to process "eqeq".
            val bbEq3  = codegen.basicBlock()                                   // Block to process "eqeqeq".
            val bbExit = codegen.basicBlock()                                   // Exit block for feather generation.
            val result = codegen.alloca(codegen.getLLVMType(callee.type), codegen.newVar())

            val condition = codegen.icmpEq(arg0, codegen.kNullObjHeaderPtr, codegen.newVar())    // Compare arg0 with "null".
            codegen.condBr(condition, bbEq3, bbEq2)                             // If (arg0 == null) bbEq3 else bbEq2.

            codegen.positionAtEnd(bbEq3)                                        // Get generation to bbEq3.
            val resultIdentityEq = evaluateOperatorEqeqeq(callee, arg0, arg1, codegen.newVar())
            codegen.store(resultIdentityEq, result)                             // Store "eqeq" result in "result".
            codegen.br(bbExit)                                                  //

            codegen.positionAtEnd(bbEq2)                                        // Get generation to bbEq2.
            val resultEquals = evaluateSimpleFunctionCall(codegen.newVar(), descriptor, listOf(arg0, arg1))
            codegen.store(resultEquals, result)                                 // Store "eqeqeq" result in "result".
            codegen.br(bbExit)                                                  //

            codegen.positionAtEnd(bbExit)                                       // Get generation to bbExit.
            return codegen.load(result, tmpVariableName)                        // Load result in tmpVariable.
        } else {
            return evaluateSimpleFunctionCall(tmpVariableName, descriptor, listOf(arg0, arg1))
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateOperatorEqeqeq(callee: IrBinaryPrimitiveImpl,
                                       arg0: LLVMValueRef, arg1: LLVMValueRef,
                                       tmpVariableName: String): LLVMValueRef {

        val arg0Type = callee.argument0.type
        val arg1Type = callee.argument1.type

        return when {
            KotlinBuiltIns.isPrimitiveType(arg0Type) -> TODO("${ir2string(callee)}")
            else -> codegen.icmpEq(arg0, arg1, tmpVariableName)
        }
    }

    //-------------------------------------------------------------------------//

    private fun generateWhenCase(isUnit:Boolean, isNothing:Boolean, resultPtr: LLVMValueRef?, branch: IrBranch, bbNext: LLVMBasicBlockRef?, bbExit: LLVMBasicBlockRef?) {
        val neitherUnitNorNothing = !isNothing && !isUnit                            // If branches doesn't end with 'return' either result hasn't got 'unit' type.
        val branchResult = branch.result
        if (isUnconditional(branch)) {                                               // It is the "else" clause.
            val brResult = evaluateExpression(codegen.newVar(), branchResult)       // Generate clause body.
            if (neitherUnitNorNothing)                                               // If nor unit neither result ends with return
                codegen.store(brResult!!, resultPtr!!)                               // we store result to temporal variable.
            if (bbExit == null) return                                               // If 'bbExit' isn't defined just return
            codegen.br(bbExit)                                                       // Generate branch to bbExit.
            codegen.positionAtEnd(bbExit)                                            // Switch generation to bbExit.
        } else {                                                                     // It is conditional clause.
            val bbCurr = codegen.basicBlock()                                        // Create block for clause body.
            val condition = evaluateExpression(codegen.newVar(), branch.condition)   // Generate cmp instruction.
            codegen.condBr(condition, bbCurr, bbNext)                                // Conditional branch depending on cmp result.
            codegen.positionAtEnd(bbCurr)                                            // Switch generation to block for clause body.
            val brResult = evaluateExpression(codegen.newVar(), branch.result)       // Generate clause body.
            if (neitherUnitNorNothing)                                               // If nor unit neither result ends with return
                codegen.store(brResult!!, resultPtr!!)                               // we store result to temporal variable.
            if (!isNothing)                                                          // If basic block doesn't end with 'return'
                codegen.br(bbExit!!)                                                 // generate branch to bbExit.
            codegen.positionAtEnd(bbNext!!)                                          // Switch generation to bbNextClause.
        }
    }

    //-------------------------------------------------------------------------//
    // Checks if the branch is unconditional

    private fun isUnconditional(branch: IrBranch): Boolean =
        branch.condition is IrConst<*>                            // If branch condition is constant.
            && (branch.condition as IrConst<*>).value as Boolean  // If condition is "true"

    //-------------------------------------------------------------------------//

    fun callDirect(descriptor: FunctionDescriptor, args: List<LLVMValueRef>, result: String?): LLVMValueRef {
        val realDescriptor = DescriptorUtils.unwrapFakeOverride(descriptor)
        val llvmFunction = codegen.functionLlvmValue(realDescriptor)
        return call(descriptor, llvmFunction, args, result)
    }

    //-------------------------------------------------------------------------//

    fun callVirtual(descriptor: FunctionDescriptor, args: List<LLVMValueRef>, result: String?): LLVMValueRef {
        val thisI8PtrPtr    = codegen.bitcast(kInt8PtrPtr, args[0], codegen.newVar())          // Cast "this (i8*)" to i8**.
        val typeInfoI8Ptr   = codegen.load(thisI8PtrPtr, codegen.newVar())                     // Load TypeInfo address.
        val typeInfoPtr     = codegen.bitcast(codegen.kTypeInfoPtr, typeInfoI8Ptr, codegen.newVar())   // Cast TypeInfo (i8*) to TypeInfo*.
        val methodHash      = codegen.functionHash(descriptor)                                 // Calculate hash of the method to be invoked
        val lookupArgs      = listOf(typeInfoPtr, methodHash)                                  // Prepare args for lookup
        val llvmMethod      = currentCodeContext.genCall(
                              context.lookupOpenMethodFunction,
                              lookupArgs,
                              codegen.newVar())                                                 // Get method ptr to be invoked

        val functionPtrType = pointerType(codegen.getLlvmFunctionType(descriptor))              // Construct type of the method to be invoked
        val function        = codegen.bitcast(functionPtrType, llvmMethod, codegen.newVar())    // Cast method address to the type
        return call(descriptor, function, args, result)                                         // Invoke the method
    }

    //-------------------------------------------------------------------------//

    private fun call(descriptor: FunctionDescriptor, function: LLVMValueRef, args: List<LLVMValueRef>, result: String?): LLVMValueRef {
        return currentCodeContext.genCall(function, args,
                if (KotlinBuiltIns.isUnit(descriptor.returnType!!)) "" else result)
    }

    //-------------------------------------------------------------------------//

    fun  superCall(result:String, descriptor:ClassConstructorDescriptor, args:List<LLVMValueRef> ): LLVMValueRef {
        val rargs = listOf(codegen.thisVariable(), *args.toTypedArray())
        return callDirect(descriptor, rargs, result)
    }

    //-------------------------------------------------------------------------//

    private fun skipConstructorBodyGeneration(declaration: IrConstructor): Boolean {
        var  skipBody = false
        declaration.acceptChildrenVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) {
                skipBody = expression.descriptor == declaration.descriptor
            }
        })
        return skipBody
    }
}
