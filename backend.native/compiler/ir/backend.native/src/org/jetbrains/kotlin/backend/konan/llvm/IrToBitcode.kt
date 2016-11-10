package org.jetbrains.kotlin.backend.konan.llvm

import kotlin_native.interop.*
import llvm.*
import org.jetbrains.kotlin.backend.konan.isArray
import org.jetbrains.kotlin.backend.konan.isInterface
import org.jetbrains.kotlin.backend.konan.isIntrinsic
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LazyClassReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBinaryPrimitiveImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetVariableImpl
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.Name

fun emitLLVM(module: IrModuleFragment, runtimeFile: String, outFile: String) {
    val llvmModule = LLVMModuleCreateWithName("out")!! // TODO: dispose
    val runtime = Runtime(runtimeFile) // TODO: dispose
    LLVMSetDataLayout(llvmModule, runtime.dataLayout)
    LLVMSetTarget(llvmModule, runtime.target)

    val context = Context(module, runtime, llvmModule) // TODO: dispose

    module.acceptVoid(RTTIGeneratorVisitor(context))
    println("\n--- Generate bitcode ------------------------------------------------------\n")
    module.acceptVoid(CodeGeneratorVisitor(context))
    memScoped {
        val errorRef = alloc(Int8Box.ref)
        // TODO: use LLVMDisposeMessage() on errorRef, once possible in interop.
        if (LLVMVerifyModule(
                llvmModule, LLVMVerifierFailureAction.LLVMPrintMessageAction, errorRef) == 1) {
            LLVMDumpModule(llvmModule)
            throw Error("Invalid module");
        }
    }
    LLVMWriteBitcodeToFile(llvmModule, outFile)
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

internal class CodeGeneratorVisitor(val context: Context) : IrElementVisitorVoid {

    val generator = CodeGenerator(context)
    val logger = Logger(generator, context)
    val metadator = MetadataGenerator(context)

    //-------------------------------------------------------------------------//

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    //-------------------------------------------------------------------------//

    override fun visitWhen(expression: IrWhen) {
        logger.log("visitWhen                  : ${ir2string(expression)}")
        var bbExit:LLVMOpaqueBasicBlock? = null             // By default "when" does not have "exit"
        if (!KotlinBuiltIns.isNothing(expression.type))     // If "when" has "exit".
            bbExit = generator.basicBlock()                 // Create basic block to process "exit".

        expression.branches.forEach {                       // Iterate through "when" branches (clauses).
            var bbNext = bbExit                             // For last clause bbNext coincides with bbExit.
            if (it != expression.branches.last())           // If it is not last clause.
                bbNext = generator.basicBlock()             // Create new basic block for next clause.
            generateWhenCase(it, bbNext, bbExit)            // Generate code for current clause.
        }
    }

    //-------------------------------------------------------------------------//

    override fun visitLoop(loop: IrLoop) {
        TODO()
    }

    //-------------------------------------------------------------------------//

    override fun visitWhileLoop(loop: IrWhileLoop) {
        val loopEnter = generator.basicBlock()
        val loopBody  = generator.basicBlock()
        val loopExit  = generator.basicBlock()

        LLVMBuildBr(context.llvmBuilder, loopEnter)

        LLVMPositionBuilderAtEnd(context.llvmBuilder, loopEnter)
        val condition = evaluateExpression(generator.tmpVariable(), loop.condition)
        LLVMBuildCondBr(context.llvmBuilder, condition, loopBody, loopExit)

        LLVMPositionBuilderAtEnd(context.llvmBuilder, loopBody)
        evaluateExpression(generator.tmpVariable(), loop.body)

        LLVMBuildBr(context.llvmBuilder, loopEnter)
        LLVMPositionBuilderAtEnd(context.llvmBuilder, loopExit)
    }

    //-------------------------------------------------------------------------//

    override fun visitDoWhileLoop(loop: IrDoWhileLoop) {
        val loopBody  = generator.basicBlock()
        val loopCheck = generator.basicBlock()
        val loopExit  = generator.basicBlock()

        LLVMBuildBr(context.llvmBuilder, loopBody)

        LLVMPositionBuilderAtEnd(context.llvmBuilder, loopBody)
        evaluateExpression(generator.tmpVariable(), loop.body)
        LLVMBuildBr(context.llvmBuilder, loopCheck)

        LLVMPositionBuilderAtEnd(context.llvmBuilder, loopCheck)
        val condition = evaluateExpression(generator.tmpVariable(), loop.condition)
        LLVMBuildCondBr(context.llvmBuilder, condition, loopBody, loopExit)

        LLVMPositionBuilderAtEnd(context.llvmBuilder, loopExit)
    }

    //-------------------------------------------------------------------------//

    override fun visitConstructor(declaration: IrConstructor) {
        generator.initFunction(declaration)
        val thisValue = generator.variable("this")
        //super.visitConstructor(declaration)
        /**
         *   %this = alloca i8*
         *   store i8* %0, i8** %this <- prolog
         *
         *   %tmp0 = load i8*, i8** %this <- epilog
         *   ret i8* %tmp0
         */
        LLVMBuildRet(context.llvmBuilder, generator.load(thisValue!!, generator.tmpVariable()))
        logger.log("visitConstructor           : ${ir2string(declaration)}")
    }

    //-------------------------------------------------------------------------//

    override fun visitBlockBody(body: IrBlockBody) {
        super.visitBlockBody(body)
        if (KotlinBuiltIns.isUnit(generator.currentFunction!!.returnType!!)) {
            LLVMBuildRet(context.llvmBuilder, null)
        }
        logger.log("visitBlockBody             : ${ir2string(body)}")
    }

    //-------------------------------------------------------------------------//

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) {
        evaluateCall(generator.tmpVariable(), expression)
    }

   //-------------------------------------------------------------------------//

    override fun visitCall(expression: IrCall) {
        logger.log("visitCall ${ir2string(expression)}")
        val isUnit = KotlinBuiltIns.isUnit(expression.descriptor.returnType!!)
        val tmpVariable = if (isUnit) "" else generator.tmpVariable()
        evaluateExpression(tmpVariable, expression)
    }

    //-------------------------------------------------------------------------//

    override fun visitFunction(declaration: IrFunction) {
        generator.function(declaration)
        metadator.function(declaration)
        declaration.acceptChildrenVoid(this)
    }

    //-------------------------------------------------------------------------//

    override fun visitClass(declaration: IrClass) {
        if (declaration.descriptor.kind == ClassKind.ANNOTATION_CLASS) {
            // do not generate any code for annotation classes as a workaround for NotImplementedError
            return
        }

        if (declaration.descriptor.isIntrinsic) {
            // do not generate any code for intrinsic classes as they require special handling
            return
        }

        if (declaration.descriptor.isInterface) {
            // Do not generate any code for interfaces.
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
        val newVariable  = generator.alloca(variableType, variableName)        // Create LLVM variable.
        generator.registerVariable(variableName, newVariable)                  // Map variableName -> LLVM variable.
        val value = evaluateExpression(variableName, declaration.initializer)  // Generate initialization code.
        generator.store(value!!, generator.variable(variableName)!!)           // Store init result in the variable
    }

    //-------------------------------------------------------------------------//

    override fun visitReturn(expression: IrReturn) {
        logger.log("visitReturn                : ${ir2string(expression)}")
        val tmpVarName = generator.tmpVariable()                            // Generate new tmp name.
        val value      = evaluateExpression(tmpVarName, expression.value)   //
        LLVMBuildRet(context.llvmBuilder, value)
    }

    //-------------------------------------------------------------------------//

    override fun visitSetVariable(expression: IrSetVariable) {
        logger.log("visitSetVariable           : ${ir2string(expression)}")
        val value = evaluateExpression(generator.tmpVariable(), expression.value)
        generator.store(value!!, generator.variable(expression.descriptor.name.asString())!!)
    }

    //-------------------------------------------------------------------------//

    private fun evaluateExpression(tmpVariableName: String, value: IrElement?): LLVMOpaqueValue? {
        when (value) {
            is IrCall            -> return evaluateCall           (tmpVariableName, value)
            is IrGetValue        -> return evaluateGetValue       (tmpVariableName, value)
            is IrSetVariable     -> return evaluateSetVariable    (                 value)
            is IrVariable        -> return evaluateVariable       (                 value)
            is IrGetField        -> return evaluateGetField       (                 value)
            is IrConst<*>        -> return evaluateConst          (                 value)
            is IrReturn          -> return evaluateReturn         (                 value)
            is IrBlock           -> return evaluateBlock          (                 value)
            null                 -> return null
            else                 -> {
                TODO()
            }
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateCall(tmpVariableName: String, value: IrMemberAccessExpression?): LLVMOpaqueValue? {
        logger.log("evaluateCall               : $tmpVariableName = ${ir2string(value)}")
        val args = mutableListOf<LLVMOpaqueValue?>()                            // Create list of function args.
        value!!.acceptChildrenVoid(object: IrElementVisitorVoid {               // Iterate args of the function.
            override fun visitElement(element: IrElement) {                     // Visit arg.
                val tmp = generator.tmpVariable()                               // Create variable representing the arg in generator
                args.add(evaluateExpression(tmp, element as IrExpression))      // Evaluate expression and get LLVM arg
            }
        })

        when {
            value.descriptor is FunctionDescriptor -> return evaluateFunctionCall(tmpVariableName, value as IrCall, args)
            value is IrDelegatingConstructorCall -> return superCall(tmpVariableName, value.descriptor, args)
            else -> {
                TODO()
            }
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateGetValue(tmpVariableName: String, value: IrGetValue): LLVMOpaqueValue {
        logger.log("evaluateGetValue           : $tmpVariableName = ${ir2string(value)}")
        when (value.descriptor) {
            is LocalVariableDescriptor,
            is ValueParameterDescriptor,
            is IrTemporaryVariableDescriptor -> {
                val variable = generator.variable(value.descriptor.name.asString())
                return generator.load(variable!!, tmpVariableName)
            }
            is LazyClassReceiverParameterDescriptor -> {
                if (value.descriptor.name.asString() == "<this>") {
                    return generator.load(generator.thisVariable(), tmpVariableName)
                }
                TODO()
            }
            else -> {
                TODO()
            }
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateSetVariable(value: IrSetVariable): LLVMOpaqueValue {
        logger.log("evaluateSetVariable        : ${ir2string(value)}")
        val ret = evaluateExpression(generator.tmpVariable(), value.value)
        return generator.store(ret!!, generator.variable(value.descriptor.name.asString())!!)
    }

    //-------------------------------------------------------------------------//

    private fun evaluateVariable(value: IrVariable): LLVMOpaqueValue {
        logger.log("evaluateVariable           : ${ir2string(value)}")
        val ret = evaluateExpression(generator.tmpVariable(), value.initializer)
        val variableName = value.descriptor.name.asString()
        val variable = generator.alloca(LLVMTypeOf(ret), variableName)
        generator.registerVariable(variableName, variable)
        return generator.store(ret!!, variable)
    }

    //-------------------------------------------------------------------------//

    private fun evaluateGetField(value: IrGetField): LLVMOpaqueValue {
        logger.log("evaluateGetField           : ${ir2string(value)}")
        if (value.descriptor.dispatchReceiverParameter != null) {
            if (value.descriptor.getter != null) {
                val tmpThis = generator.load(generator.thisVariable(), generator.tmpVariable())
                return evaluateSimpleFunctionCall(generator.tmpVariable(), value.descriptor.getter!!.original, mutableListOf(tmpThis))!!
            } else {
                val thisPtr = generator.load(generator.thisVariable(), generator.tmpVariable())
                val typedPtr = generator.bitcast(pointerType(generator.classType(generator.currentClass!!)), thisPtr, generator.tmpVariable())
                val fieldPtr = LLVMBuildStructGEP(generator.context.llvmBuilder, typedPtr, generator.indexInClass(value.descriptor), generator.tmpVariable())
                return generator.load(fieldPtr!!, generator.tmpVariable())
            }
        }
        TODO()
    }

    //-------------------------------------------------------------------------//

    private fun evaluateConst(value: IrConst<*>): LLVMOpaqueValue? {
        logger.log("evaluateConst              : ${ir2string(value)}")
        when (value.kind) {
            IrConstKind.Null -> TODO() // LLVMConstPointerNull
            IrConstKind.Boolean -> when (value.value) {
                true  -> return LLVMConstInt(LLVMInt1Type(), 1, 1)
                false -> return LLVMConstInt(LLVMInt1Type(), 0, 1)
            }
            IrConstKind.Char   -> return LLVMConstInt(LLVMInt16Type(), (value.value as Char).toLong(), 0)
            IrConstKind.Byte   -> return LLVMConstInt(LLVMInt32Type(), (value.value as Byte).toLong(), 1)
            IrConstKind.Short  -> return LLVMConstInt(LLVMInt32Type(), (value.value as Short).toLong(), 1)
            IrConstKind.Int    -> return LLVMConstInt(LLVMInt32Type(), (value.value as Int).toLong(), 1)
            IrConstKind.Long   -> return LLVMConstInt(LLVMInt64Type(), value.value as Long, 1)
            IrConstKind.String ->
                return context.staticData.createStringLiteral(value as IrConst<String>).getLlvmValue()
            IrConstKind.Float  -> return LLVMConstRealOfString(LLVMFloatType(), (value.value as Float).toString())
            IrConstKind.Double -> return LLVMConstRealOfString(LLVMDoubleType(), (value.value as Double).toString())
        }
        TODO()
    }

    //-------------------------------------------------------------------------//

    private fun evaluateReturn(value: IrReturn): LLVMOpaqueValue? {
        logger.log("evaluateReturn             : ${ir2string(value)}")
        val ret = evaluateExpression(generator.tmpVariable(), value.value)
        return LLVMBuildRet(context.llvmBuilder, ret)
    }

    //-------------------------------------------------------------------------//

    private fun evaluateBlock(value: IrBlock): LLVMOpaqueValue? {
        logger.log("evaluateBlock              : ${ir2string(value)}")
        value.statements.dropLast(1).forEach {
            evaluateExpression(generator.tmpVariable(), it)
        }
        return evaluateExpression(generator.tmpVariable(), value.statements.lastOrNull())
    }

    //-------------------------------------------------------------------------//

    private fun evaluateSimpleFunctionCall(tmpVariableName: String, descriptor: FunctionDescriptor, args: MutableList<LLVMOpaqueValue?>): LLVMOpaqueValue? {
        //logger.log("evaluateSimpleFunctionCall : $tmpVariableName = ${ir2string(value)}")
        if (descriptor.isOverridable)
            return callVirtual(descriptor, args, tmpVariableName)
        else
            return callDirect(descriptor, args, tmpVariableName)
    }

    //-------------------------------------------------------------------------//

    private fun evaluateFunctionCall(tmpVariableName: String, callee: IrCall, args: MutableList<LLVMOpaqueValue?>): LLVMOpaqueValue? {
        val descriptor:FunctionDescriptor = callee.descriptor as FunctionDescriptor
        when (descriptor) {
            is IrBuiltinOperatorDescriptorBase -> return evaluateOperatorCall      (tmpVariableName, callee, args)
            is ClassConstructorDescriptor      -> return evaluateConstructorCall   (tmpVariableName, callee,     args)
            else                               -> return evaluateSimpleFunctionCall(tmpVariableName, descriptor, args)
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateConstructorCall(variableName: String, callee: IrCall, args: MutableList<LLVMOpaqueValue?>): LLVMOpaqueValue? {
        logger.log("evaluateConstructorCall    : $variableName = ${ir2string(callee)}")
        memScoped {
            val containingClass = (callee.descriptor as ClassConstructorDescriptor).containingDeclaration
            val typeInfo = generator.typeInfoValue(containingClass)
            val allocHint = Int32(1).getLlvmValue()
            val thisValue = if (containingClass.isArray) {
                assert(args.size == 1 && LLVMTypeOf(args[0]) == LLVMInt32Type())
                val size = args[0]
                val params = allocNativeArrayOf(LLVMOpaqueValue, typeInfo, allocHint, size)
                LLVMBuildCall(
                        context.llvmBuilder, context.allocArrayFunction, params[0], 3, variableName)
            } else {
                val params = allocNativeArrayOf(LLVMOpaqueValue, typeInfo, allocHint)
                LLVMBuildCall(
                        context.llvmBuilder, context.allocInstanceFunction, params[0], 2, variableName)
            }
            val constructorParams: MutableList<LLVMOpaqueValue?> = mutableListOf()
            constructorParams += thisValue
            constructorParams += args
            return evaluateSimpleFunctionCall(variableName, callee.descriptor as FunctionDescriptor, constructorParams)
        }
    }

    //-------------------------------------------------------------------------//
    private val EQEQ = Name.identifier("EQEQ")

    private fun evaluateOperatorCall(tmpVariableName: String, callee: IrCall, args: MutableList<LLVMOpaqueValue?>): LLVMOpaqueValue {
        logger.log("evaluateCall $tmpVariableName origin:$callee")
        val descriptor = callee.descriptor
        when (descriptor.name) {
            EQEQ -> return evaluateOperatorEqeq(callee as IrBinaryPrimitiveImpl, args[0]!!, args[1]!!, tmpVariableName)
            else -> {
                TODO()
            }
        }
    }

    private fun evaluateOperatorEqeq(callee: IrBinaryPrimitiveImpl, arg0: LLVMOpaqueValue, arg1: LLVMOpaqueValue, tmpVariableName: String):LLVMOpaqueValue {
        val operandType = callee.argument0.type
        assert(operandType == callee.argument1.type)
        when {
            KotlinBuiltIns.isByte  (operandType) -> return generator.icmpEq(arg0, arg1, tmpVariableName)
            KotlinBuiltIns.isShort (operandType) -> return generator.icmpEq(arg0, arg1, tmpVariableName)
            KotlinBuiltIns.isInt   (operandType) -> return generator.icmpEq(arg0, arg1, tmpVariableName)
            KotlinBuiltIns.isLong  (operandType) -> return generator.icmpEq(arg0, arg1, tmpVariableName)
            KotlinBuiltIns.isFloat (operandType) -> return generator.fcmpEq(arg0, arg1, tmpVariableName)
            KotlinBuiltIns.isDouble(operandType) -> return generator.fcmpEq(arg0, arg1, tmpVariableName)
            else                                 -> TODO("ComplexType")
        }
    }

    //-------------------------------------------------------------------------//

    private fun generateWhenCase(branch: IrBranch, bbNext: LLVMOpaqueBasicBlock?, bbExit: LLVMOpaqueBasicBlock?) {
        if (isUnconditional(branch)) {                                                      // It is the "else" clause.
            evaluateExpression(generator.tmpVariable(), branch.result)                      // Generate clause body.
            if (bbExit == null) return                                                      // If "when" does not have exit - return.
            LLVMBuildBr(context.llvmBuilder, bbExit)                                        // Generate branch to bbExit.
            LLVMPositionBuilderAtEnd(context.llvmBuilder, bbExit)                           // Switch generation to bbExit.
        } else {                                                                            // It is conditional clause.
            val bbCurr = generator.basicBlock()                                             // Create block for clause body.
            val condition = evaluateExpression(generator.tmpVariable(), branch.condition)   // Generate cmp instruction.
            LLVMBuildCondBr(context.llvmBuilder, condition, bbCurr, bbNext)                 // Conditional branch depending on cmp result.
            LLVMPositionBuilderAtEnd(context.llvmBuilder, bbCurr)                           // Switch generation to block for clause body.
            evaluateExpression(generator.tmpVariable(), branch.result)                      // Generate clause body.
            if (!KotlinBuiltIns.isNothing(branch.result.type))                              // If clause code does not contain "return".
                LLVMBuildBr(context.llvmBuilder, bbExit)                                    // Generate branch to bbExit.
            LLVMPositionBuilderAtEnd(context.llvmBuilder, bbNext)                           // Switch generation to bbNextClause.
        }
    }

    //-------------------------------------------------------------------------//
    // Checks if the branch is unconditional

    private fun isUnconditional(branch: IrBranch): Boolean =
        branch.condition is IrConst<*>                            // If branch condition is constant.
            && (branch.condition as IrConst<*>).value as Boolean  // If condition is "true"

    //-------------------------------------------------------------------------//

    fun callDirect(descriptor: FunctionDescriptor, args: MutableList<LLVMOpaqueValue?>, result: String?): LLVMOpaqueValue? {
        val llvmFunction = generator.functionLlvmValue(descriptor)
        return generator.call(llvmFunction, args, result)
    }

    //-------------------------------------------------------------------------//

    /* Runtime constant */
    private val kTypeInfo = LLVMGetTypeByName(context.llvmModule, "struct.TypeInfo")!!
    private val kTypeInfoPtr = pointerType(kTypeInfo)
    private val kInt8Ptr = pointerType(LLVMInt8Type())
    private val kInt8PtrPtr = pointerType(kInt8Ptr)

    //-------------------------------------------------------------------------//

    fun callVirtual(descriptor: FunctionDescriptor, args: MutableList<LLVMOpaqueValue?>, result: String?): LLVMOpaqueValue? {
        val thisI8PtrPtr    = generator.bitcast(kInt8PtrPtr, args[0]!!, generator.tmpVariable())        // Cast "this (i8*)" to i8**.
        val typeInfoI8Ptr   = generator.load(thisI8PtrPtr!!, generator.tmpVariable())                   // Load TypeInfo address.
        val typeInfoPtr     = generator.bitcast(kTypeInfoPtr, typeInfoI8Ptr, generator.tmpVariable())   // Cast TypeInfo (i8*) to TypeInfo*.
        val methodHash      = generator.functionHash(descriptor)                                        // Calculate hash of the method to be invoked
        val lookupArgs      = mutableListOf(typeInfoPtr, methodHash)                                    // Prepare args for lookup
        val llvmMethod      = generator.call(
                              context.lookupOpenMethodFunction,
                              lookupArgs,
                              generator.tmpVariable())                                                  // Get method ptr to be invoked

        val functionPtrType = pointerType(getLlvmFunctionType(descriptor))                              // Construct type of the method to be invoked
        val function        = generator.bitcast(functionPtrType, llvmMethod!!, generator.tmpVariable()) // Cast method address to the type
        return generator.call(function, args, result)                                                   // Invoke the method
    }

    //-------------------------------------------------------------------------//

    fun  superCall(result:String, descriptor:ClassConstructorDescriptor, args:MutableList<LLVMOpaqueValue?> ):LLVMOpaqueValue? {
        val tmp = generator.load(generator.thisVariable(), generator.tmpVariable())
        var rargs:MutableList<LLVMOpaqueValue?>? = null
        if (args.size != 0)
            rargs = mutableListOf<LLVMOpaqueValue?>(tmp, *args.toTypedArray())
        else
            rargs = mutableListOf<LLVMOpaqueValue?>(tmp)
        return callDirect(descriptor, rargs, result)
    }

}
