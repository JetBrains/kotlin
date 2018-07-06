/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.backend.common.ir.ir2string
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.backend.konan.ir.*
import org.jetbrains.kotlin.backend.konan.irasdescriptors.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExport
import org.jetbrains.kotlin.backend.konan.optimizations.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnableBlockImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.getArguments
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe

private val threadLocalAnnotationFqName = FqName("konan.ThreadLocal")

val IrClassSymbol.objectIsShared get() =
    !descriptor.annotations.hasAnnotation(threadLocalAnnotationFqName)

internal fun emitLLVM(context: Context) {
    val irModule = context.irModule!!

    // Note that we don't set module target explicitly.
    // It is determined by the target of runtime.bc
    // (see Llvm class in ContextUtils)
    // Which in turn is determined by the clang flags
    // used to compile runtime.bc.
    val llvmModule = LLVMModuleCreateWithName("out")!! // TODO: dispose
    context.llvmModule = llvmModule
    context.debugInfo.builder = DICreateBuilder(llvmModule)
    context.llvmDeclarations = createLlvmDeclarations(context)

    val phaser = PhaseManager(context)

    phaser.phase(KonanPhase.RTTI) {
        irModule.acceptVoid(RTTIGeneratorVisitor(context))
    }

    generateDebugInfoHeader(context)

    var moduleDFG: ModuleDFG? = null
    phaser.phase(KonanPhase.BUILD_DFG) {
        moduleDFG = ModuleDFGBuilder(context, irModule).build()
    }

    val lifetimes = mutableMapOf<IrElement, Lifetime>()
    val codegenVisitor = CodeGeneratorVisitor(context, lifetimes)

    @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
    var externalModulesDFG: ExternalModulesDFG? = null
    phaser.phase(KonanPhase.DESERIALIZE_DFG) {
        externalModulesDFG = DFGSerializer.deserialize(context, moduleDFG!!.symbolTable.privateTypeIndex, moduleDFG!!.symbolTable.privateFunIndex)
    }

    @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
    var devirtualizationAnalysisResult: Devirtualization.AnalysisResult? = null
    phaser.phase(KonanPhase.DEVIRTUALIZATION) {
        devirtualizationAnalysisResult = Devirtualization.run(irModule, context, moduleDFG!!, externalModulesDFG!!)

        val privateFunctions = moduleDFG!!.symbolTable.getPrivateFunctionsTableForExport()
        privateFunctions.forEachIndexed { index, it ->
            val function = codegenVisitor.codegen.llvmFunction(it.first)
            LLVMAddAlias(
                    context.llvmModule,
                    function.type,
                    function,
                    irModule.descriptor.privateFunctionSymbolName(index, it.second.name)
            )!!
        }
        context.privateFunctions = privateFunctions

        val privateClasses = moduleDFG!!.symbolTable.getPrivateClassesTableForExport()

        privateClasses.forEachIndexed { index, it ->
            val typeInfoPtr = codegenVisitor.codegen.typeInfoValue(it.first)
            LLVMAddAlias(
                    context.llvmModule,
                    typeInfoPtr.type,
                    typeInfoPtr,
                    irModule.descriptor.privateClassSymbolName(index, it.second.name)
            )!!
        }
        context.privateClasses = privateClasses
    }

    phaser.phase(KonanPhase.ESCAPE_ANALYSIS) {
        val callGraph = CallGraphBuilder(context, moduleDFG!!, externalModulesDFG!!, devirtualizationAnalysisResult, false).build()
        EscapeAnalysis.computeLifetimes(context, moduleDFG!!, externalModulesDFG!!, callGraph, lifetimes)
    }

    phaser.phase(KonanPhase.SERIALIZE_DFG) {
        DFGSerializer.serialize(context, moduleDFG!!)
    }

    phaser.phase(KonanPhase.CODEGEN) {
        irModule.acceptVoid(codegenVisitor)
    }

    if (context.shouldContainDebugInfo()) {
        DIFinalize(context.debugInfo.builder)
    }
}

internal fun verifyModule(llvmModule: LLVMModuleRef, current: String = "") {
    memScoped {
        val errorRef = allocPointerTo<ByteVar>()
        // TODO: use LLVMDisposeMessage() on errorRef, once possible in interop.
        if (LLVMVerifyModule(
                llvmModule, LLVMVerifierFailureAction.LLVMPrintMessageAction, errorRef.ptr) == 1) {
            if (current.isNotEmpty())
                println("Error in $current")
            LLVMDumpModule(llvmModule)
            throw Error("Invalid module")
        }
    }
}

internal class RTTIGeneratorVisitor(context: Context) : IrElementVisitorVoid {
    val generator = RTTIGenerator(context)

    val kotlinObjCClassInfoGenerator = KotlinObjCClassInfoGenerator(context)

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        super.visitClass(declaration)

        val descriptor = declaration
        if (descriptor.isIntrinsic) {
            // do not generate any code for intrinsic classes as they require special handling
            return
        }

        generator.generate(descriptor)

        if (descriptor.isKotlinObjCClass()) {
            kotlinObjCClassInfoGenerator.generate(declaration)
        }
    }

}

//-------------------------------------------------------------------------//


/**
 * Defines how to generate context-dependent operations.
 */
internal interface CodeContext {

    /**
     * Generates `return` [value] operation.
     *
     * @param value may be null iff target type is `Unit`.
     */
    fun genReturn(target: IrSymbolOwner, value: LLVMValueRef?)

    fun genBreak(destination: IrBreak)

    fun genContinue(destination: IrContinue)

    val exceptionHandler: ExceptionHandler

    fun genThrow(exception: LLVMValueRef)

    /**
     * Declares the variable.
     * @return index of declared variable.
     */
    fun genDeclareVariable(descriptor: VariableDescriptor, value: LLVMValueRef?, variableLocation: VariableDebugLocation?): Int

    /**
     * @return index of variable declared before, or -1 if no such variable has been declared yet.
     */
    fun getDeclaredVariable(descriptor: VariableDescriptor): Int

    /**
     * Generates the code to obtain a value available in this context.
     *
     * @return the requested value
     */
    fun genGetValue(descriptor: ValueDescriptor): LLVMValueRef

    /**
     * Returns owning function scope.
     *
     * @return the requested value
     */
    fun functionScope(): CodeContext?

    /**
     * Returns owning file scope.
     *
     * @return the requested value if in the file scope or null.
     */
    fun fileScope(): CodeContext?

    /**
     * Returns owning class scope [ClassScope].
     *
     * @returns the requested value if in the class scope or null.
     */
    fun classScope(): CodeContext?

    fun addResumePoint(bbLabel: LLVMBasicBlockRef): Int

    /**
     * Returns owning returnable block scope [ReturnableBlockScope].
     *
     * @returns the requested value if in the returnableBlockScope scope or null.
     */
    fun returnableBlockScope(): CodeContext?

    /**
     * Returns location information for given source location [LocationInfo].
     */
    fun location(line:Int, column: Int): LocationInfo?

    /**
     * Returns [DIScopeOpaqueRef] instance for corresponding scope.
     */
    fun scope(): DIScopeOpaqueRef?
}

//-------------------------------------------------------------------------//

internal class CodeGeneratorVisitor(val context: Context, val lifetimes: Map<IrElement, Lifetime>) : IrElementVisitorVoid {

    val codegen = CodeGenerator(context)

    //-------------------------------------------------------------------------//

    // TODO: consider eliminating mutable state
    private var currentCodeContext: CodeContext = TopLevelCodeContext


    /**
     * Fake [CodeContext] that doesn't support any operation.
     *
     * During function code generation [FunctionScope] should be set up.
     */
    private object TopLevelCodeContext : CodeContext {
        private fun unsupported(any: Any? = null): Nothing = throw UnsupportedOperationException(any?.toString() ?: "")

        override fun genReturn(target: IrSymbolOwner, value: LLVMValueRef?) = unsupported(target)

        override fun genBreak(destination: IrBreak) = unsupported()

        override fun genContinue(destination: IrContinue) = unsupported()

        override val exceptionHandler get() = unsupported()

        override fun genThrow(exception: LLVMValueRef) = unsupported()

        override fun genDeclareVariable(descriptor: VariableDescriptor, value: LLVMValueRef?, variableLocation: VariableDebugLocation?) = unsupported(descriptor)

        override fun getDeclaredVariable(descriptor: VariableDescriptor) = -1

        override fun genGetValue(descriptor: ValueDescriptor) = unsupported(descriptor)

        override fun functionScope(): CodeContext? = null

        override fun fileScope(): CodeContext? = null

        override fun classScope(): CodeContext? = null

        override fun addResumePoint(bbLabel: LLVMBasicBlockRef) = unsupported(bbLabel)

        override fun returnableBlockScope(): CodeContext? = null

        override fun location(line: Int, column: Int): LocationInfo? = unsupported()

        override fun scope(): DIScopeOpaqueRef? = unsupported()
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

    private fun appendCAdapters() {
        CAdapterGenerator(context, codegen).generateBindings()
    }

    //-------------------------------------------------------------------------//

    override fun visitElement(element: IrElement) {
        TODO(ir2string(element))
    }

    //-------------------------------------------------------------------------//
    override fun visitModuleFragment(declaration: IrModuleFragment) {
        context.log{"visitModule                    : ${ir2string(declaration)}"}

        initializeCachedBoxes(context)
        declaration.acceptChildrenVoid(this)

        // Note: it is here because it also generates some bitcode.
        ObjCExport(codegen).produce()

        codegen.objCDataGenerator?.finishModule()

        appendLlvmUsed("llvm.used", context.llvm.usedFunctions + context.llvm.usedGlobals)
        appendLlvmUsed("llvm.compiler.used", context.llvm.compilerUsedGlobals)
        appendStaticInitializers()
        appendEntryPointSelector(context.ir.symbols.entryPoint?.owner)
        if (context.isNativeLibrary) {
            appendCAdapters()
        }

    }

    //-------------------------------------------------------------------------//

    val kVoidFuncType = LLVMFunctionType(LLVMVoidType(), null, 0, 0)!!
    val kInitFuncType = LLVMFunctionType(LLVMVoidType(), cValuesOf(LLVMInt32Type()), 1, 0)!!
    val kNodeInitType = LLVMGetTypeByName(context.llvmModule, "struct.InitNode")!!
    //-------------------------------------------------------------------------//

    val INIT_GLOBALS = 0
    val DEINIT_THREAD_LOCAL_GLOBALS = 1
    val DEINIT_GLOBALS = 2

    private fun createInitBody(): LLVMValueRef {
        val initFunction = LLVMAddFunction(context.llvmModule, "", kInitFuncType)!!
        generateFunction(codegen, initFunction) {
            using(FunctionScope(initFunction, "init_body", it)) {
                val bbInit = basicBlock("init", null)
                val bbLocalDeinit = basicBlock("local_deinit", null)
                val bbGlobalDeinit = basicBlock("global_deinit", null)
                val bbDefault = basicBlock("default", null) {
                    unreachable()
                }

                switch(LLVMGetParam(initFunction, 0)!!,
                        listOf(Int32(INIT_GLOBALS).llvm                to bbInit,
                               Int32(DEINIT_THREAD_LOCAL_GLOBALS).llvm to bbLocalDeinit,
                               Int32(DEINIT_GLOBALS).llvm              to bbGlobalDeinit),
                        bbDefault)

                // Globals initalizers may contain accesses to objects, so visit them first.
                appendingTo(bbInit) {
                    context.llvm.fileInitializers
                            .forEach {
                                if (it.initializer?.expression !is IrConst<*>?) {
                                    val initialization = evaluateExpression(it.initializer!!.expression)
                                    val address = context.llvmDeclarations.forStaticField(it).storage
                                    storeAny(initialization, address)
                                }
                            }
                    ret(null)
                }

                appendingTo(bbLocalDeinit) {
                    context.llvm.fileInitializers.forEach {
                        val descriptor = it
                        if (descriptor.type.isValueType())
                            return@forEach // Is not a subject for memory management.
                        val address = context.llvmDeclarations.forStaticField(descriptor).storage
                        storeAny(codegen.kNullObjHeaderPtr, address)
                    }
                    context.llvm.objects.forEach { storeAny(codegen.kNullObjHeaderPtr, it) }
                    ret(null)
                }

                appendingTo(bbGlobalDeinit) {
                    context.llvm.sharedObjects.forEach { storeAny(codegen.kNullObjHeaderPtr, it) }
                    ret(null)
                }
            }
        }
        return initFunction
    }

    //-------------------------------------------------------------------------//
    // Creates static struct InitNode $nodeName = {$initName, NULL};

    private fun createInitNode(initFunction: LLVMValueRef): LLVMValueRef {
        val nextInitNode = LLVMConstNull(pointerType(kNodeInitType))
        val argList = cValuesOf(initFunction, nextInitNode)
        // Create static object of class InitNode.
        val initNode = LLVMConstNamedStruct(kNodeInitType, argList, 2)!!
        // Create global variable with init record data.
        return context.llvm.staticData.placeGlobal(
                "init_node", constPointer(initNode), isExported = false).llvmGlobal
    }

    //-------------------------------------------------------------------------//

    private fun createInitCtor(initNodePtr: LLVMValueRef): LLVMValueRef {
        val ctorFunction = LLVMAddFunction(context.llvmModule, "", kVoidFuncType)!!
        generateFunction(codegen, ctorFunction) {
            call(context.llvm.appendToInitalizersTail, listOf(initNodePtr))
            ret(null)
        }
        return ctorFunction
    }

    //-------------------------------------------------------------------------//

    override fun visitFile(declaration: IrFile) {
        // TODO: collect those two in one place.
        context.llvm.fileInitializers.clear()
        context.llvm.objects.clear()
        context.llvm.sharedObjects.clear()

        using(FileScope(declaration)) {
            declaration.acceptChildrenVoid(this)

            if (context.llvm.fileInitializers.isEmpty() && context.llvm.objects.isEmpty() && context.llvm.sharedObjects.isEmpty())
                return

            // Create global initialization records.
            val initNode = createInitNode(createInitBody())
            context.llvm.staticInitializers.add(createInitCtor(initNode))
        }
    }

    //-------------------------------------------------------------------------//

    private inner class LoopScope(val loop: IrLoop) : InnerScopeImpl() {
        val loopExit  = functionGenerationContext.basicBlock("loop_exit", loop.condition.startLocation)
        val loopCheck = functionGenerationContext.basicBlock("loop_check", loop.condition.startLocation)

        override fun genBreak(destination: IrBreak) {
            if (destination.loop == loop)
                functionGenerationContext.br(loopExit)
            else
                super.genBreak(destination)
        }

        override fun genContinue(destination: IrContinue) {
            if (destination.loop == loop)
                functionGenerationContext.br(loopCheck)
            else
                super.genContinue(destination)
        }
    }

    //-------------------------------------------------------------------------//

    fun evaluateBreak(destination: IrBreak): LLVMValueRef {
        currentCodeContext.genBreak(destination)
        return codegen.kNothingFakeValue
    }

    //-------------------------------------------------------------------------//

    fun evaluateContinue(destination: IrContinue): LLVMValueRef {
        currentCodeContext.genContinue(destination)
        return codegen.kNothingFakeValue
    }

    //-------------------------------------------------------------------------//

    override fun visitConstructor(declaration: IrConstructor) {
        context.log{"visitConstructor               : ${ir2string(declaration)}"}
        if (declaration.descriptor.containingDeclaration.defaultType.isValueType()) {
            // Do not generate any ctors for value types.
            return
        }

        if (declaration.isObjCConstructor()) {
            // Do not generate any ctors for external Objective-C classes.
            return
        }

        visitFunction(declaration)
    }

    //-------------------------------------------------------------------------//

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) {
        context.log{"visitAnonymousInitializer      : ${ir2string(declaration)}"}
    }

    //-------------------------------------------------------------------------//

    /**
     * The scope of variable visibility.
     */
    private inner class VariableScope : InnerScopeImpl() {

        override fun genDeclareVariable(descriptor: VariableDescriptor, value: LLVMValueRef?, variableLocation: VariableDebugLocation?): Int {
            return functionGenerationContext.vars.createVariable(descriptor, value, variableLocation)
        }

        override fun getDeclaredVariable(descriptor: VariableDescriptor): Int {
            val index = functionGenerationContext.vars.indexOf(descriptor)
            return if (index < 0) super.getDeclaredVariable(descriptor) else return index
        }

        override fun genGetValue(descriptor: ValueDescriptor): LLVMValueRef {
            val index = functionGenerationContext.vars.indexOf(descriptor)
            if (index < 0) {
                return super.genGetValue(descriptor)
            } else {
                return functionGenerationContext.vars.load(index)
            }
        }
    }

    /**
     * The scope of parameter visibility.
     */
    private open inner class ParameterScope(
            function: IrFunction?,
            private val functionGenerationContext: FunctionGenerationContext): InnerScopeImpl() {

        val parameters = bindParameters(function)

        init {
            if (function != null) {
                parameters.forEach{
                    val descriptor = it.key
                    val ir = descriptor

                    val local = functionGenerationContext.vars.createParameter(descriptor,
                            debugInfoIfNeeded(function, ir))
                    functionGenerationContext.mapParameterForDebug(local, it.value)

                }
            }
        }

        override fun genGetValue(descriptor: ValueDescriptor): LLVMValueRef {
            val index = functionGenerationContext.vars.indexOf(descriptor)
            if (index < 0) {
                return super.genGetValue(descriptor)
            } else {
                return functionGenerationContext.vars.load(index)
            }
        }
    }

    /**
     * The [CodeContext] enclosing the entire function body.
     */
    private inner class FunctionScope (val declaration: IrFunction?, val functionGenerationContext: FunctionGenerationContext) : InnerScopeImpl() {


        constructor(llvmFunction:LLVMValueRef, name:String, functionGenerationContext: FunctionGenerationContext):this(null, functionGenerationContext) {
            this.llvmFunction = llvmFunction
            this.name = name
        }

        var llvmFunction:LLVMValueRef? = declaration?.let{
            codegen.llvmFunction(it)
        }

        private var name:String? = declaration?.descriptor?.name?.asString()

        override fun genReturn(target: IrSymbolOwner, value: LLVMValueRef?) {
            if (declaration == null || target == declaration) {
                if ((target as IrFunction).returnsUnit()) {
                    functionGenerationContext.ret(null)
                } else {
                    functionGenerationContext.ret(value!!)
                }
            } else {
                super.genReturn(target, value)
            }
        }

        override val exceptionHandler get() = ExceptionHandler.Caller

        override fun genThrow(exception: LLVMValueRef) {
            val objHeaderPtr = functionGenerationContext.bitcast(codegen.kObjHeaderPtr, exception)
            val args = listOf(objHeaderPtr)

            functionGenerationContext.call(
                    context.llvm.throwExceptionFunction, args, Lifetime.IRRELEVANT, this.exceptionHandler
            )
            functionGenerationContext.unreachable()
        }

        override fun functionScope(): CodeContext = this


        private val scope by lazy {
            if (!context.shouldContainDebugInfo())
                return@lazy null
            declaration?.scope() ?: llvmFunction!!.scope(0, subroutineType(context, codegen.llvmTargetData, listOf(context.builtIns.intType)))
        }

        override fun location(line: Int, column: Int) = scope?.let { LocationInfo(it, line, column) }

        override fun scope() = scope
    }

    private val functionGenerationContext
            get() = (currentCodeContext.functionScope() as FunctionScope).functionGenerationContext
    /**
     * Binds LLVM function parameters to IR parameter descriptors.
     */
    private fun bindParameters(descriptor: FunctionDescriptor?): Map<ParameterDescriptor, LLVMValueRef> {
        if (descriptor == null) return emptyMap()
        val parameterDescriptors = descriptor.allParameters
        return parameterDescriptors.mapIndexed { i, parameterDescriptor ->
            val parameter = codegen.param(descriptor, i)
            assert(codegen.getLLVMType(parameterDescriptor.type) == parameter.type)
            parameterDescriptor to parameter
        }.toMap()
    }

    override fun visitFunction(declaration: IrFunction) {
        context.log{"visitFunction                  : ${ir2string(declaration)}"}
        val body = declaration.body

        if (declaration.descriptor.modality == Modality.ABSTRACT) return
        if (declaration.descriptor.isExternal)                    return
        if (body == null)                                         return

        generateFunction(codegen, declaration,
                declaration.location(declaration.startLine(), declaration.startColumn()),
                declaration.location(declaration.endLine(), declaration.endColumn())) {
            using(FunctionScope(declaration, it)) {
                val parameterScope = ParameterScope(declaration, functionGenerationContext)
                using(parameterScope) {
                    using(VariableScope()) {
                        when (body) {
                            is IrBlockBody -> body.statements.forEach { generateStatement(it) }
                            is IrExpressionBody -> generateStatement(body.expression)
                            is IrSyntheticBody -> throw AssertionError("Synthetic body ${body.kind} has not been lowered")
                            else -> TODO(ir2string(body))
                        }
                    }
                }
            }
        }


        if (declaration.descriptor.usedAnnotation) {
            context.llvm.usedFunctions.add(codegen.llvmFunction(declaration))
        }

        if (context.shouldVerifyBitCode())
            verifyModule(context.llvmModule!!,
                "${declaration.descriptor.containingDeclaration}::${ir2string(declaration)}")
    }

    private fun IrFunction.location(line: Int, column:Int) =
            if (context.shouldContainDebugInfo()) LocationInfo(
                scope = scope()!!,
                line = line,
                column = column)
            else null

    //-------------------------------------------------------------------------//

    override fun visitClass(declaration: IrClass) {
        context.log{"visitClass                     : ${ir2string(declaration)}"}
        if (declaration.descriptor.kind == ClassKind.ANNOTATION_CLASS) {
            // For annotation classes generate only nested classes.
            declaration.declarations
                    .filterIsInstance<IrClass>()
                    .forEach { it.acceptVoid(this) }
            return
        }
        using(ClassScope(declaration)) {
            declaration.declarations.forEach {
                it.acceptVoid(this)
            }
        }
    }

    //-------------------------------------------------------------------------//

    override fun visitTypeAlias(declaration: IrTypeAlias) {
        // Nothing to do.
    }

    //-------------------------------------------------------------------------//

    override fun visitProperty(declaration: IrProperty) {
        val container = declaration.descriptor.containingDeclaration
        // For value types with real backing field there's no point to generate an accessor.
        if (container is ClassDescriptor && container.defaultType.isValueType() && declaration.backingField != null)
            return
        declaration.getter?.acceptVoid(this)
        declaration.setter?.acceptVoid(this)
        declaration.backingField?.acceptVoid(this)
    }

    //-------------------------------------------------------------------------//

    override fun visitField(declaration: IrField) {
        context.log{"visitField                     : ${ir2string(declaration)}"}
        debugFieldDeclaration(declaration)
        val descriptor = declaration
        if (context.needGlobalInit(declaration)) {
            val type = codegen.getLLVMType(descriptor.type)
            val globalProperty = context.llvmDeclarations.forStaticField(descriptor).storage
            val initializer = declaration.initializer?.expression as? IrConst<*>
            if (initializer != null)
                LLVMSetInitializer(globalProperty, evaluateExpression(initializer))
            else
                LLVMSetInitializer(globalProperty, LLVMConstNull(type))
            context.llvm.fileInitializers.add(declaration)

            // (Cannot do this before the global is initialized).
            LLVMSetLinkage(globalProperty, LLVMLinkage.LLVMInternalLinkage)
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateExpression(value: IrExpression): LLVMValueRef {
        updateBuilderDebugLocation(value)
        when (value) {
            is IrTypeOperatorCall    -> return evaluateTypeOperator           (value)
            is IrCall                -> return evaluateCall                   (value)
            is IrDelegatingConstructorCall ->
                                        return evaluateCall                   (value)
            is IrInstanceInitializerCall ->
                                        return evaluateInstanceInitializerCall(value)
            is IrGetValue            -> return evaluateGetValue               (value)
            is IrSetVariable         -> return evaluateSetVariable            (value)
            is IrGetField            -> return evaluateGetField               (value)
            is IrSetField            -> return evaluateSetField               (value)
            is IrConst<*>            -> return evaluateConst                  (value)
            is IrReturn              -> return evaluateReturn                 (value)
            is IrWhen                -> return evaluateWhen                   (value)
            is IrThrow               -> return evaluateThrow                  (value)
            is IrTry                 -> return evaluateTry                    (value)
            is IrReturnableBlockImpl -> return evaluateReturnableBlock        (value)
            is IrContainerExpression -> return evaluateContainerExpression    (value)
            is IrWhileLoop           -> return evaluateWhileLoop              (value)
            is IrDoWhileLoop         -> return evaluateDoWhileLoop            (value)
            is IrVararg              -> return evaluateVararg                 (value)
            is IrBreak               -> return evaluateBreak                  (value)
            is IrContinue            -> return evaluateContinue               (value)
            is IrGetObjectValue      -> return evaluateGetObjectValue         (value)
            is IrFunctionReference   -> return evaluateFunctionReference      (value)
            is IrSuspendableExpression ->
                                        return evaluateSuspendableExpression  (value)
            is IrSuspensionPoint     -> return evaluateSuspensionPoint        (value)
            is IrPrivateClassReference ->
                                        return evaluatePrivateClassReference  (value)
            else                     -> {
                TODO(ir2string(value))
            }
        }
    }

    private fun generateStatement(statement: IrStatement) {
        when (statement) {
            is IrExpression -> evaluateExpression(statement)
            is IrVariable -> generateVariable(statement)
            else -> TODO(ir2string(statement))
        }
    }

    private fun IrStatement.generate() = generateStatement(this)

    //-------------------------------------------------------------------------//

    private fun evaluateGetObjectValue(value: IrGetObjectValue): LLVMValueRef =
            functionGenerationContext.getObjectValue(
                    value.symbol.owner,
                    value.symbol.objectIsShared && context.config.threadsAreAllowed,
                    currentCodeContext.exceptionHandler,
                    value.startLocation
            )


    //-------------------------------------------------------------------------//

    private fun evaluateExpressionAndJump(expression: IrExpression, destination: ContinuationBlock) {
        val result = evaluateExpression(expression)

        // It is possible to check here whether the generated code has the normal continuation path
        // and do not generate any jump if not;
        // however such optimization can lead to phi functions with zero entries, which is not allowed by LLVM;
        // TODO: find the better solution.

        jump(destination, result)
    }

    //-------------------------------------------------------------------------//

    /**
     * Represents the basic block which may expect a value:
     * when generating a [jump] to this block, one should provide the value.
     * Inside the block that value is accessible as [valuePhi].
     *
     * This class is designed to be used to generate Kotlin expressions that have a value and require branching.
     *
     * [valuePhi] may be `null`, which would mean `Unit` value is passed.
     */
    private data class ContinuationBlock(val block: LLVMBasicBlockRef, val valuePhi: LLVMValueRef?)

    private val ContinuationBlock.value: LLVMValueRef
        get() = this.valuePhi ?: codegen.theUnitInstanceRef.llvm

    /**
     * Jumps to [target] passing [value].
     */
    private fun jump(target: ContinuationBlock, value: LLVMValueRef?) {
        val entry = target.block
        functionGenerationContext.br(entry)
        if (target.valuePhi != null) {
            functionGenerationContext.assignPhis(target.valuePhi to value!!)
        }
    }

    /**
     * Creates new [ContinuationBlock] that receives the value of given Kotlin type
     * and generates [code] starting from its beginning.
     */
    private fun continuationBlock(type: IrType,
                                  locationInfo: LocationInfo?, code: (ContinuationBlock) -> Unit = {}): ContinuationBlock {
        val entry = functionGenerationContext.basicBlock("continuation_block", locationInfo)

        functionGenerationContext.appendingTo(entry) {
            val valuePhi = if (type.isUnit()) {
                null
            } else {
                functionGenerationContext.phi(codegen.getLLVMType(type))
            }

            val result = ContinuationBlock(entry, valuePhi)
            code(result)
            return result
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateVararg(value: IrVararg): LLVMValueRef {
        val elements = value.elements.map {
            if (it is IrExpression) {
                val mapped = evaluateExpression(it)
                if (mapped.isConst) {
                    return@map mapped
                }
            }

            throw IllegalStateException("IrVararg neither was lowered nor can be statically evaluated")
        }

        val arrayClass = value.type.getClass()!!

        // Note: even if all elements are const, they aren't guaranteed to be statically initialized.
        // E.g. an element may be a pointer to lazy-initialized object (aka singleton).
        // However it is guaranteed that all elements are already initialized at this point.
        return codegen.staticData.createKotlinArray(arrayClass, elements)
    }

    //-------------------------------------------------------------------------//

    private fun evaluateThrow(expression: IrThrow): LLVMValueRef {
        val exception = evaluateExpression(expression.value)
        currentCodeContext.genThrow(exception)
        return codegen.kNothingFakeValue
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
                functionGenerationContext.basicBlock("landingpad", endLocationInfoFromScope()) {
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
                continuationBlock(context.ir.symbols.throwable.owner.defaultType, endLocationInfoFromScope()) {
                    genHandler(it.value)
                }
            }
        }

        private fun endLocationInfoFromScope(): LocationInfo? {
            val functionScope = currentCodeContext.functionScope()
            val irFunction = functionScope?.let {
                (functionScope as FunctionScope).declaration
            }
            return irFunction?.endLocation
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
            with(functionGenerationContext) {
                val exceptionPtr = catchKotlinException()
                jumpToHandler(exceptionPtr)
            }
        }

        override val exceptionHandler: ExceptionHandler get() = object : ExceptionHandler.Local() {
            override val unwind get() = landingpad
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

            for (catch in catches) {
                fun genCatchBlock() {
                    using(VariableScope()) {
                        currentCodeContext.genDeclareVariable(catch.catchParameter, exception, null)
                        evaluateExpressionAndJump(catch.result, success)
                    }
                }

                if (catch.parameter.type == context.builtIns.throwable.defaultType) {
                    genCatchBlock()
                    return      // Remaining catch clauses are unreachable.
                } else {
                    val isInstance = genInstanceOf(exception, catch.catchParameter.type.getClass()!!)
                    val body = functionGenerationContext.basicBlock("catch", catch.startLocation)
                    val nextCheck = functionGenerationContext.basicBlock("catchCheck", catch.endLocation)
                    functionGenerationContext.condBr(isInstance, body, nextCheck)

                    functionGenerationContext.appendingTo(body) {
                        genCatchBlock()
                    }

                    functionGenerationContext.positionAtEnd(nextCheck)
                }
            }
            // rethrow the exception if no clause can handle it.
            outerContext.genThrow(exception)
        }
    }

    private fun evaluateTry(expression: IrTry): LLVMValueRef {
        // TODO: does basic block order influence machine code order?
        // If so, consider reordering blocks to reduce exception tables size.

        assert (expression.finallyExpression == null, { "All finally blocks should've been lowered" })

        val continuation = continuationBlock(expression.type, expression.endLocation)

        val catchScope = if (expression.catches.isEmpty())
                             null
                         else
                             CatchScope(expression.catches, continuation)
        using(catchScope) {
            evaluateExpressionAndJump(expression.tryResult, continuation)
        }
        functionGenerationContext.positionAtEnd(continuation.block)

        return continuation.value
    }

    //-------------------------------------------------------------------------//
    /* FIXME. Fix "when" type in frontend.
     * For the following code:
     *  fun foo(x: Int) {
     *      when (x) {
     *          0 -> 0
     *      }
     *  }
     *  we cannot determine if the result of when is assigned or not.
     */
    private fun evaluateWhen(expression: IrWhen): LLVMValueRef {
        context.log{"evaluateWhen                   : ${ir2string(expression)}"}
        var bbExit: LLVMBasicBlockRef? = null             // By default "when" does not have "exit".
        val isUnit                = expression.type.isUnit()
        val isNothing             = expression.type.isNothing()

        // We may not cover all cases if IrWhen is used as statement.
        val coverAllCases         = isUnconditional(expression.branches.last())

        // "When" has exit block if:
        //      its type is not Nothing - we must place phi in the exit block
        //      or it doesn't cover all cases - we may fall through all of them and must create exit block to continue.
        if (!isNothing || !coverAllCases)                                         // If "when" has "exit".
            bbExit = functionGenerationContext.basicBlock("when_exit", expression.startLocation)   // Create basic block to process "exit".

        val llvmType = codegen.getLLVMType(expression.type)
        val resultPhi = if (isUnit || isNothing || !coverAllCases) null else
            functionGenerationContext.appendingTo(bbExit!!) {
                functionGenerationContext.phi(llvmType)
            }

        expression.branches.forEach {                           // Iterate through "when" branches (clauses).
            var bbNext = bbExit                                 // For last clause bbNext coincides with bbExit.
            if (it != expression.branches.last())                            // If it is not last clause.
                bbNext = functionGenerationContext.basicBlock("when_next", it.startLocation)  // Create new basic block for next clause.
            generateWhenCase(resultPhi, it, bbNext, bbExit)                  // Generate code for current clause.
        }

        return when {
            // FIXME: remove the hacks.
            isUnit -> functionGenerationContext.theUnitInstanceRef.llvm
            isNothing -> functionGenerationContext.kNothingFakeValue
            !coverAllCases -> LLVMGetUndef(llvmType)!!
            else -> resultPhi!!
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateWhileLoop(loop: IrWhileLoop): LLVMValueRef {
        val loopScope = LoopScope(loop)
        using(loopScope) {
            val loopBody = functionGenerationContext.basicBlock("while_loop", loop.startLocation)
            functionGenerationContext.br(loopScope.loopCheck)

            functionGenerationContext.positionAtEnd(loopScope.loopCheck)
            val condition = evaluateExpression(loop.condition)
            functionGenerationContext.condBr(condition, loopBody, loopScope.loopExit)

            functionGenerationContext.positionAtEnd(loopBody)
            loop.body?.generate()

            functionGenerationContext.br(loopScope.loopCheck)
            functionGenerationContext.positionAtEnd(loopScope.loopExit)

        }

        assert(loop.type.isUnit())
        return functionGenerationContext.theUnitInstanceRef.llvm
    }

    //-------------------------------------------------------------------------//

    private fun evaluateDoWhileLoop(loop: IrDoWhileLoop): LLVMValueRef {
        val loopScope = LoopScope(loop)
        using(loopScope) {
            val loopBody = functionGenerationContext.basicBlock("do_while_loop", loop.body?.startLocation ?: loop.startLocation)
            functionGenerationContext.br(loopBody)

            functionGenerationContext.positionAtEnd(loopBody)
            loop.body?.generate()
            functionGenerationContext.br(loopScope.loopCheck)

            functionGenerationContext.positionAtEnd(loopScope.loopCheck)
            val condition = evaluateExpression(loop.condition)
            functionGenerationContext.condBr(condition, loopBody, loopScope.loopExit)

            functionGenerationContext.positionAtEnd(loopScope.loopExit)
        }

        assert(loop.type.isUnit())
        return functionGenerationContext.theUnitInstanceRef.llvm
    }

    //-------------------------------------------------------------------------//

    private fun evaluateGetValue(value: IrGetValue): LLVMValueRef {
        context.log{"evaluateGetValue               : ${ir2string(value)}"}
        return currentCodeContext.genGetValue(value.symbol.owner)
    }

    //-------------------------------------------------------------------------//

    private fun evaluateSetVariable(value: IrSetVariable): LLVMValueRef {
        context.log{"evaluateSetVariable            : ${ir2string(value)}"}
        val result = evaluateExpression(value.value)
        val variable = currentCodeContext.getDeclaredVariable(value.symbol.owner)
        functionGenerationContext.vars.store(result, variable)
        assert(value.type.isUnit())
        return functionGenerationContext.theUnitInstanceRef.llvm
    }

    //-------------------------------------------------------------------------//
    private fun debugInfoIfNeeded(function: IrFunction?, element: IrElement): VariableDebugLocation? {
        if (function == null || !element.needDebugInfo(context) || currentCodeContext.scope() == null) return null
        val locationInfo = element.startLocation ?: return null
        val location = codegen.generateLocationInfo(locationInfo)
        val file = (currentCodeContext.fileScope() as FileScope).file.file()
        return when (element) {
            is IrVariable -> if (element.origin != IrDeclarationOrigin.IR_TEMPORARY_VARIABLE) debugInfoLocalVariableLocation(
                    builder       = context.debugInfo.builder,
                    functionScope = locationInfo.scope,
                    diType        = element.descriptor.type.diType(context, codegen.llvmTargetData),
                    name          = element.descriptor.name,
                    file          = file,
                    line          = locationInfo.line,
                    location      = location)
                    else null
            is IrValueParameter -> debugInfoParameterLocation(
                    builder       = context.debugInfo.builder,
                    functionScope = locationInfo.scope,
                    diType        = element.descriptor.type.diType(context, codegen.llvmTargetData),
                    name          = element.descriptor.name,
                    argNo         = if (element.isValueParameter) element.index else 0,
                    file          = file,
                    line          = locationInfo.line,
                    location      = location)
            else -> throw Error("Unsupported element type: ${ir2string(element)}")
        }
    }

    private fun generateVariable(variable: IrVariable) {
        context.log{"generateVariable               : ${ir2string(variable)}"}
        val value = variable.initializer?.let { evaluateExpression(it) }
        currentCodeContext.genDeclareVariable(
                variable, value, debugInfoIfNeeded(
                (currentCodeContext.functionScope() as FunctionScope).declaration, variable))
    }

    //-------------------------------------------------------------------------//

    private fun evaluateTypeOperator(value: IrTypeOperatorCall): LLVMValueRef {
        return when (value.operator) {
            IrTypeOperator.CAST                      -> evaluateCast(value)
            IrTypeOperator.IMPLICIT_INTEGER_COERCION -> evaluateIntegerCoercion(value)
            IrTypeOperator.IMPLICIT_CAST             -> evaluateExpression(value.argument)
            IrTypeOperator.IMPLICIT_NOTNULL          -> TODO(ir2string(value))
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> {
                evaluateExpression(value.argument)
                functionGenerationContext.theUnitInstanceRef.llvm
            }
            IrTypeOperator.SAFE_CAST                 -> throw IllegalStateException("safe cast wasn't lowered")
            IrTypeOperator.INSTANCEOF                -> evaluateInstanceOf(value)
            IrTypeOperator.NOT_INSTANCEOF            -> evaluateNotInstanceOf(value)
        }
    }

    //-------------------------------------------------------------------------//

    private fun IrType.isPrimitiveInteger(): Boolean {
        return this.isPrimitiveType() &&
               !this.isBoolean() &&
               !this.isFloat() &&
               !this.isDouble() &&
               !this.isChar()
    }

    private fun evaluateIntegerCoercion(value: IrTypeOperatorCall): LLVMValueRef {
        context.log{"evaluateIntegerCoercion        : ${ir2string(value)}"}
        val type = value.typeOperand
        assert(type.isPrimitiveInteger())
        val result = evaluateExpression(value.argument)
        val llvmSrcType = codegen.getLLVMType(value.argument.type)
        val llvmDstType = codegen.getLLVMType(type)
        val srcWidth    = LLVMGetIntTypeWidth(llvmSrcType)
        val dstWidth    = LLVMGetIntTypeWidth(llvmDstType)
        return when {
            srcWidth == dstWidth           -> result
            srcWidth > dstWidth            -> LLVMBuildTrunc(functionGenerationContext.builder, result, llvmDstType, "")!!
            else /* srcWidth < dstWidth */ -> LLVMBuildSExt(functionGenerationContext.builder, result, llvmDstType, "")!!
        }
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

    private fun evaluateCast(value: IrTypeOperatorCall): LLVMValueRef {
        context.log{"evaluateCast                   : ${ir2string(value)}"}
        val dstDescriptor = value.typeOperand.getClass()!!

        assert(!dstDescriptor.defaultType.isValueType() &&
                !value.argument.type.isValueType())

        val srcArg        = evaluateExpression(value.argument)                         // Evaluate src expression.

        if (dstDescriptor.defaultType.isObjCObjectType()) {
            with(functionGenerationContext) {
                ifThen(not(genInstanceOf(srcArg, dstDescriptor))) {
                    callDirect(
                            context.ir.symbols.ThrowTypeCastException.owner,
                            emptyList(),
                            Lifetime.GLOBAL
                    )
                }
            }
            return srcArg
        }
        // Note: the code above would actually work for any classes.
        // However, the code generated below is shorter. Consider it to be a specialization.

        val dstTypeInfo   = codegen.typeInfoValue(dstDescriptor)                       // Get TypeInfo for dst type.
        val srcObjInfoPtr = functionGenerationContext.bitcast(codegen.kObjHeaderPtr, srcArg)             // Cast src to ObjInfoPtr.
        val args          = listOf(srcObjInfoPtr, dstTypeInfo)                         // Create arg list.
        call(context.llvm.checkInstanceFunction, args)                                 // Check if dst is subclass of src.
        return srcArg
    }

    //-------------------------------------------------------------------------//

    private fun evaluateInstanceOf(value: IrTypeOperatorCall): LLVMValueRef {
        context.log{"evaluateInstanceOf             : ${ir2string(value)}"}

        val type     = value.typeOperand
        val srcArg   = evaluateExpression(value.argument)     // Evaluate src expression.

        val bbExit       = functionGenerationContext.basicBlock("instance_of_exit", value.startLocation)
        val bbInstanceOf = functionGenerationContext.basicBlock("instance_of_notnull", value.startLocation)
        val bbNull       = functionGenerationContext.basicBlock("instance_of_null", value.startLocation)

        val condition = functionGenerationContext.icmpEq(srcArg, codegen.kNullObjHeaderPtr)
        functionGenerationContext.condBr(condition, bbNull, bbInstanceOf)

        functionGenerationContext.positionAtEnd(bbNull)
        val resultNull = if (type.containsNull()) kTrue else kFalse
        functionGenerationContext.br(bbExit)

        functionGenerationContext.positionAtEnd(bbInstanceOf)
        val typeOperandClass = value.typeOperand.getClass()
        val resultInstanceOf = if (typeOperandClass != null) {
            genInstanceOf(srcArg, typeOperandClass)
        } else {
            // E.g. when generating type operation with reified type parameter in the original body of inline function.
            kTrue
            // TODO: these code should be unreachable, however [BridgesBuilding] generates IR with such type checks.
        }
        functionGenerationContext.br(bbExit)
        val bbInstanceOfResult = functionGenerationContext.currentBlock

        functionGenerationContext.positionAtEnd(bbExit)
        val result = functionGenerationContext.phi(kBoolean)
        functionGenerationContext.addPhiIncoming(result, bbNull to resultNull, bbInstanceOfResult to resultInstanceOf)
        return result
    }

    //-------------------------------------------------------------------------//

    private fun genInstanceOf(obj: LLVMValueRef, dstClass: IrClass): LLVMValueRef {
        if (dstClass.defaultType.isObjCObjectType()) {
            return genInstanceOfObjC(obj, dstClass)
        }

        val dstTypeInfo   = codegen.typeInfoValue(dstClass)                            // Get TypeInfo for dst type.
        val srcObjInfoPtr = functionGenerationContext.bitcast(codegen.kObjHeaderPtr, obj)                // Cast src to ObjInfoPtr.
        val args          = listOf(srcObjInfoPtr, dstTypeInfo)                         // Create arg list.

        return call(context.llvm.isInstanceFunction, args)                       // Check if dst is subclass of src.
    }

    private fun genInstanceOfObjC(obj: LLVMValueRef, dstClass: IrClass): LLVMValueRef {
        val objCObject = callDirect(
                context.ir.symbols.interopObjCObjectRawValueGetter.owner,
                listOf(obj),
                Lifetime.IRRELEVANT
        )

        return if (dstClass.isObjCClass()) {
            if (dstClass.isInterface) {
                val isMeta = if (dstClass.isObjCMetaClass()) kTrue else kFalse
                call(
                        context.llvm.Kotlin_Interop_DoesObjectConformToProtocol,
                        listOf(
                                objCObject,
                                genGetObjCProtocol(dstClass),
                                isMeta
                        )
                )
            } else {
                call(
                        context.llvm.Kotlin_Interop_IsObjectKindOfClass,
                        listOf(objCObject, genGetObjCClass(dstClass))
                )
            }.let {
                functionGenerationContext.icmpNe(it, kFalse)
            }


        } else {
            // e.g. ObjCObject, ObjCObjectBase etc.
            if (dstClass.isObjCMetaClass()) {
                val isClass = context.llvm.externalFunction(
                        "object_isClass",
                        functionType(int8Type, false, int8TypePtr),
                        context.standardLlvmSymbolsOrigin
                )

                call(isClass, listOf(objCObject)).let {
                    functionGenerationContext.icmpNe(it, Int8(0).llvm)
                }
            } else {
                kTrue
            }
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateNotInstanceOf(value: IrTypeOperatorCall): LLVMValueRef {
        val instanceOfResult = evaluateInstanceOf(value)
        return functionGenerationContext.not(instanceOfResult)
    }

    //-------------------------------------------------------------------------//

    private fun evaluateGetField(value: IrGetField): LLVMValueRef {
        context.log{"evaluateGetField               : ${ir2string(value)}"}
        if (value.descriptor.dispatchReceiverParameter != null) {
            val thisPtr = evaluateExpression(value.receiver!!)
            return functionGenerationContext.loadSlot(
                    fieldPtrOfClass(thisPtr, value.symbol.owner), value.descriptor.isVar())
        } else {
            assert (value.receiver == null)
            val ptr = context.llvmDeclarations.forStaticField(value.symbol.owner).storage
            return functionGenerationContext.loadSlot(ptr, value.descriptor.isVar())
        }
    }

    //-------------------------------------------------------------------------//
    // TODO: rewrite in IR!
    private fun needMutationCheck(descriptor: org.jetbrains.kotlin.descriptors.DeclarationDescriptor): Boolean {
        // For now we omit mutation checks on immutable types, as this allows initialization in constructor
        // and it is assumed that API doesn't allow to change them.
        return !descriptor.isFrozen
    }

    private fun evaluateSetField(value: IrSetField): LLVMValueRef {
        context.log{"evaluateSetField               : ${ir2string(value)}"}
        val valueToAssign = evaluateExpression(value.value)
        if (value.descriptor.dispatchReceiverParameter != null) {
            val thisPtr = evaluateExpression(value.receiver!!)
            if (needMutationCheck(value.descriptor.containingDeclaration)) {
                functionGenerationContext.call(context.llvm.mutationCheck,
                        listOf(functionGenerationContext.bitcast(codegen.kObjHeaderPtr, thisPtr)),
                        Lifetime.IRRELEVANT, ExceptionHandler.Caller)
            }
            functionGenerationContext.storeAny(valueToAssign, fieldPtrOfClass(thisPtr, value.symbol.owner))
        } else {
            assert (value.receiver == null)
            val globalValue = context.llvmDeclarations.forStaticField(value.symbol.owner).storage
            functionGenerationContext.storeAny(valueToAssign, globalValue)
        }

        assert (value.type.isUnit())
        return codegen.theUnitInstanceRef.llvm
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
    private fun fieldPtrOfClass(thisPtr: LLVMValueRef, value: IrField): LLVMValueRef {
        val fieldInfo = context.llvmDeclarations.forField(value)

        val classDescriptor = value.containingDeclaration as ClassDescriptor
        val typePtr = pointerType(fieldInfo.classBodyType)

        val bodyPtr = getObjectBodyPtr(classDescriptor, thisPtr)

        val typedBodyPtr = functionGenerationContext.bitcast(typePtr, bodyPtr)
        val fieldPtr = LLVMBuildStructGEP(functionGenerationContext.builder, typedBodyPtr, fieldInfo.index, "")
        return fieldPtr!!
    }

    private fun getObjectBodyPtr(classDescriptor: ClassDescriptor, objectPtr: LLVMValueRef): LLVMValueRef {
        return if (classDescriptor.isObjCClass()) {
            assert(classDescriptor.isKotlinObjCClass())

            val objCPtr = callDirect(context.ir.symbols.interopObjCObjectRawValueGetter.owner,
                    listOf(objectPtr), Lifetime.IRRELEVANT)

            val objCDeclarations = context.llvmDeclarations.forClass(classDescriptor).objCDeclarations!!
            val bodyOffset = functionGenerationContext.load(objCDeclarations.bodyOffsetGlobal.llvmGlobal)

            functionGenerationContext.gep(objCPtr, bodyOffset)
        } else {
            LLVMBuildGEP(functionGenerationContext.builder, objectPtr, cValuesOf(kImmOne), 1, "")!!
        }
    }

    //-------------------------------------------------------------------------//
    private fun evaluateStringConst(value: IrConst<String>) =
            context.llvm.staticData.kotlinStringLiteral(value.value).llvm

    private fun evaluateConst(value: IrConst<*>): LLVMValueRef {
        context.log{"evaluateConst                  : ${ir2string(value)}"}
        /* This suppression against IrConst<String> */
        @Suppress("UNCHECKED_CAST")
        when (value.kind) {
            IrConstKind.Null    -> return codegen.kNullObjHeaderPtr
            IrConstKind.Boolean -> when (value.value) {
                true  -> return kTrue
                false -> return kFalse
            }
            IrConstKind.Char   -> return LLVMConstInt(LLVMInt16Type(), (value.value as Char).toLong(),  0)!!
            IrConstKind.Byte   -> return LLVMConstInt(LLVMInt8Type(),  (value.value as Byte).toLong(),  1)!!
            IrConstKind.Short  -> return LLVMConstInt(LLVMInt16Type(), (value.value as Short).toLong(), 1)!!
            IrConstKind.Int    -> return LLVMConstInt(LLVMInt32Type(), (value.value as Int).toLong(),   1)!!
            IrConstKind.Long   -> return LLVMConstInt(LLVMInt64Type(), value.value as Long,             1)!!
            IrConstKind.String -> return evaluateStringConst(value as IrConst<String>)
            IrConstKind.Float  -> return LLVMConstRealOfString(LLVMFloatType(), (value.value as Float).toString())!!
            IrConstKind.Double -> return LLVMConstRealOfString(LLVMDoubleType(), (value.value as Double).toString())!!
        }
        TODO(ir2string(value))
    }

    //-------------------------------------------------------------------------//

    private fun evaluateReturn(expression: IrReturn): LLVMValueRef {
        context.log{"evaluateReturn                 : ${ir2string(expression)}"}
        val value = expression.value

        val evaluated = evaluateExpression(value)

        val target = expression.returnTargetSymbol.owner

        currentCodeContext.genReturn(target, evaluated)
        return codegen.kNothingFakeValue
    }

    //-------------------------------------------------------------------------//
    fun getFileEntry(sourceFileName: String): SourceManager.FileEntry =
        // We must cache file entries, otherwise we reparse same file many times.
            context.fileEntryCache.getOrPut(sourceFileName) {
                NaiveSourceBasedFileEntryImpl(sourceFileName)
            }

    private inner class ReturnableBlockScope(val returnableBlock: IrReturnableBlock) :
            FileScope(IrFileImpl(getFileEntry(returnableBlock.sourceFileName))) {

        var bbExit : LLVMBasicBlockRef? = null
        var resultPhi : LLVMValueRef? = null

        private fun getExit(): LLVMBasicBlockRef {
            if (bbExit == null) bbExit = functionGenerationContext.basicBlock("returnable_block_exit", null)
            return bbExit!!
        }

        private fun getResult(): LLVMValueRef {
            if (resultPhi == null) {
                val bbCurrent = functionGenerationContext.currentBlock
                functionGenerationContext.positionAtEnd(getExit())
                resultPhi = functionGenerationContext.phi(codegen.getLLVMType(returnableBlock.type))
                functionGenerationContext.positionAtEnd(bbCurrent)
            }
            return resultPhi!!
        }

        override fun genReturn(target: IrSymbolOwner, value: LLVMValueRef?) {
            if (target != returnableBlock) {                                    // It is not our "local return".
                super.genReturn(target, value)
                return
            }
                                                                                // It is local return from current function.
            functionGenerationContext.br(getExit())                                               // Generate branch on exit block.

            if (!returnableBlock.type.isUnit()) {                               // If function returns more then "unit"
                functionGenerationContext.assignPhis(getResult() to value!!)                      // Assign return value to result PHI node.
            }
        }

        override fun returnableBlockScope(): CodeContext? = this


        /**
         * Note: DILexicalBlocks aren't nested, they should be scoped with the parent function.
         */
        private val scope by lazy {
            if (!context.shouldContainDebugInfo())
                return@lazy null
            val lexicalBlockFile = DICreateLexicalBlockFile(context.debugInfo.builder, functionScope()!!.scope(), super.file.file())
            DICreateLexicalBlock(context.debugInfo.builder, lexicalBlockFile, super.file.file(), returnableBlock.startLine(), returnableBlock.startColumn())!!
        }

        override fun scope() = scope

    }

    //-------------------------------------------------------------------------//

    private open inner class FileScope(val file:IrFile) : InnerScopeImpl() {
        override fun fileScope(): CodeContext? = this

        override fun location(line: Int, column: Int) = scope()?.let {LocationInfo(it, line, column) }

        @Suppress("UNCHECKED_CAST")
        private val scope by lazy {
            if (!context.shouldContainDebugInfo())
                return@lazy null
            file.file() as DIScopeOpaqueRef?
        }

        override fun scope() = scope
    }

    //-------------------------------------------------------------------------//

    private inner class ClassScope(val clazz:IrClass) : InnerScopeImpl() {
        val isExported
            get() = clazz.isExported()
        var offsetInBits = 0L
        val members = mutableListOf<DIDerivedTypeRef>()
        @Suppress("UNCHECKED_CAST")
        val scope = if (isExported && context.shouldContainDebugInfo())
            DICreateReplaceableCompositeType(
                    tag        = DwarfTag.DW_TAG_structure_type.value,
                    refBuilder = context.debugInfo.builder,
                    refScope   = (currentCodeContext.fileScope() as FileScope).file.file() as DIScopeOpaqueRef,
                    name       = clazz.typeInfoSymbolName,
                    refFile    = file().file(),
                    line       = clazz.startLine()) as DITypeOpaqueRef
        else null
        override fun classScope(): CodeContext? = this
    }

    //-------------------------------------------------------------------------//

    private fun evaluateReturnableBlock(value: IrReturnableBlock): LLVMValueRef {
        context.log{"evaluateReturnableBlock         : ${value.statements.forEach { ir2string(it) }}"}

        val returnableBlockScope = ReturnableBlockScope(value)
        using(returnableBlockScope) {
            using(VariableScope()) {
                value.statements.forEach {
                    generateStatement(it)
                }
            }
        }

        val bbExit = returnableBlockScope.bbExit
        if (bbExit != null) {
            if (!functionGenerationContext.isAfterTerminator()) {                 // TODO should we solve this problem once and for all
                if (returnableBlockScope.resultPhi != null) {
                    functionGenerationContext.unreachable()
                } else {
                    functionGenerationContext.br(bbExit)
                }
            }
            functionGenerationContext.positionAtEnd(bbExit)
        }

        return returnableBlockScope.resultPhi ?: codegen.theUnitInstanceRef.llvm
    }

    //-------------------------------------------------------------------------//

    private fun evaluateContainerExpression(value: IrContainerExpression): LLVMValueRef {
        context.log{"evaluateContainerExpression    : ${value.statements.forEach { ir2string(it) }}"}

        val scope = if (value.isTransparentScope) {
            null
        } else {
            VariableScope()
        }

        using(scope) {
            value.statements.dropLast(1).forEach {
                generateStatement(it)
            }
            value.statements.lastOrNull()?.let {
                if (it is IrExpression) {
                    return evaluateExpression(it)
                } else {
                    generateStatement(it)
                }
            }

            assert(value.type.isUnit())
            return codegen.theUnitInstanceRef.llvm
        }
    }

    private fun evaluateInstanceInitializerCall(expression: IrInstanceInitializerCall): LLVMValueRef {
        assert (expression.type.isUnit())
        return codegen.theUnitInstanceRef.llvm
    }

    //-------------------------------------------------------------------------//

    private fun evaluateSpecialIntrinsicCall(expression: IrFunctionAccessExpression): LLVMValueRef? {
        val function = expression.symbol.owner

        if (function.isIntrinsic) {
            when (function.descriptor) {
                context.interopBuiltIns.objCObjectInitBy -> {
                    val receiver = evaluateExpression(expression.extensionReceiver!!)
                    val irConstructorCall = expression.getValueArgument(0) as IrCall
                    val constructorDescriptor = irConstructorCall.symbol.owner as ClassConstructorDescriptor
                    val constructorArgs = evaluateExplicitArgs(irConstructorCall)
                    val args = listOf(receiver) + constructorArgs
                    callDirect(constructorDescriptor, args, Lifetime.IRRELEVANT)
                    return receiver
                }

                context.builtIns.immutableBinaryBlobOf -> {
                    @Suppress("UNCHECKED_CAST")
                    val arg = expression.getValueArgument(0) as IrConst<String>
                    return context.llvm.staticData.createImmutableBinaryBlob(arg)
                }

                context.ir.symbols.initInstance.descriptor -> {
                    val callee = expression as IrCall
                    val initializer = callee.getValueArgument(1) as IrCall
                    val thiz = evaluateExpression(callee.getValueArgument(0)!!)
                    evaluateSimpleFunctionCall(
                            initializer.symbol.owner,
                            listOf(thiz) + evaluateExplicitArgs(initializer),
                            resultLifetime(initializer)
                    )
                    return codegen.theUnitInstanceRef.llvm
                }
            }
        }

        return null
    }

    //-------------------------------------------------------------------------//
    private fun evaluateCall(value: IrFunctionAccessExpression): LLVMValueRef {
        context.log{"evaluateCall                   : ${ir2string(value)}"}

        evaluateSpecialIntrinsicCall(value)?.let { return it }

        val args = evaluateExplicitArgs(value)

        updateBuilderDebugLocation(value)
        when {
            value is IrDelegatingConstructorCall   ->
                return delegatingConstructorCall(value.symbol.owner, args)

            else ->
                return evaluateFunctionCall(value as IrCall, args, resultLifetime(value))
        }
    }

    //-------------------------------------------------------------------------//
    private fun file() = (currentCodeContext.fileScope() as FileScope).file

    //-------------------------------------------------------------------------//
    private fun updateBuilderDebugLocation(element: IrElement):DILocationRef? {
        if (!context.shouldContainDebugInfo() || currentCodeContext.functionScope() == null) return null
        @Suppress("UNCHECKED_CAST")
        return element.startLocation?.let{functionGenerationContext.debugLocation(it)}
    }

    private val IrElement.startLocation: LocationInfo?
        get() = currentCodeContext.location(startLine(), startColumn())

    private val IrElement.endLocation: LocationInfo?
        get() = currentCodeContext.location(endLine(), endColumn())

    //-------------------------------------------------------------------------//
    private fun IrElement.startLine() = file().fileEntry.line(this.startOffset)

    //-------------------------------------------------------------------------//
    private fun IrElement.startColumn() = file().fileEntry.column(this.startOffset)

    //-------------------------------------------------------------------------//
    private fun IrElement.endLine() = file().fileEntry.line(this.endOffset)

    //-------------------------------------------------------------------------//
    private fun IrElement.endColumn() = file().fileEntry.column(this.endOffset)

    //-------------------------------------------------------------------------//
    private fun debugFieldDeclaration(expression: IrField) {
        val scope = currentCodeContext.classScope() as? ClassScope ?: return
        if (!scope.isExported || !context.shouldContainDebugInfo()) return
        val irFile = (currentCodeContext.fileScope() as FileScope).file
        val sizeInBits = expression.descriptor.type.size(context)
        scope.offsetInBits += sizeInBits
        val alignInBits = expression.descriptor.type.alignment(context)
        scope.offsetInBits = alignTo(scope.offsetInBits, alignInBits)
        @Suppress("UNCHECKED_CAST")
        scope.members.add(DICreateMemberType(
                refBuilder   = context.debugInfo.builder,
                refScope     = scope.scope as DIScopeOpaqueRef,
                name         = expression.symbolName,
                file         = irFile.file(),
                lineNum      = expression.startLine(),
                sizeInBits   = sizeInBits,
                alignInBits  = alignInBits,
                offsetInBits = scope.offsetInBits,
                flags        = 0,
                type         = expression.descriptor.type.diType(context, codegen.llvmTargetData)
        )!!)
    }


    //-------------------------------------------------------------------------//
    private fun IrFile.file(): DIFileRef {
        return context.debugInfo.files.getOrPut(this.fileEntry.name) {
            val path = this.fileEntry.name.toFileAndFolder()
            DICreateCompilationUnit(
                    builder     = context.debugInfo.builder,
                    lang        = DwarfLanguage.DW_LANG_Kotlin.value,
                    File        = path.file,
                    dir         = path.folder,
                    producer    = DWARF.producer,
                    isOptimized = 0,
                    flags       = "",
                    rv          = DWARF.runtimeVersion)
            DICreateFile(context.debugInfo.builder, path.file, path.folder)!!
        }
    }

    //-------------------------------------------------------------------------//

    private fun IrFunction.scope():DIScopeOpaqueRef? = this.scope(startLine())

    @Suppress("UNCHECKED_CAST")
    private fun FunctionDescriptor.scope(startLine:Int): DIScopeOpaqueRef? {
        if (codegen.isExternal(this) || !context.shouldContainDebugInfo())
            return null
        return context.debugInfo.subprograms.getOrPut(codegen.llvmFunction(this)) {
            memScoped {
                val subroutineType = subroutineType(context, codegen.llvmTargetData)
                val functionLlvmValue = codegen.llvmFunction(this@scope)
                diFunctionScope(name.asString(), functionLlvmValue.name!!, startLine, subroutineType, functionLlvmValue)
            }
        }  as DIScopeOpaqueRef
    }

    @Suppress("UNCHECKED_CAST")
    private fun LLVMValueRef.scope(startLine:Int, subroutineType: DISubroutineTypeRef): DIScopeOpaqueRef? {
        return context.debugInfo.subprograms.getOrPut(this) {
            diFunctionScope(name!!, name!!, startLine, subroutineType, this)
        }  as DIScopeOpaqueRef
    }
    private fun diFunctionScope(name: String, linkageName: String, startLine: Int, subroutineType: DISubroutineTypeRef, functionLlvmValue: LLVMValueRef): DISubprogramRef {
        @Suppress("UNCHECKED_CAST")
        val diFunction = DICreateFunction(
                builder = context.debugInfo.builder,
                scope = (currentCodeContext.fileScope() as FileScope).file.file() as DIScopeOpaqueRef,
                name = name,
                linkageName = linkageName,
                file = file().file(),
                lineNo = startLine,
                type = subroutineType,
                //TODO: need more investigations.
                isLocal = 0,
                isDefinition = 1,
                scopeLine = 0)
        DIFunctionAddSubprogram(functionLlvmValue, diFunction)
        return diFunction!!
    }

    //-------------------------------------------------------------------------//

    private val coroutineImplDescriptor = context.ir.symbols.coroutineImpl.owner
    private val doResumeFunctionDescriptor = coroutineImplDescriptor.declarations
            .filterIsInstance<IrSimpleFunction>().single { it.name.asString() == "doResume" }

    private fun getContinuation(): LLVMValueRef {
        val caller = functionGenerationContext.functionDescriptor!!
        return if (caller.isSuspend)
            codegen.param(caller, caller.allParameters.size)    // The last argument.
        else {
            // Suspend call from non-suspend function - must be [CoroutineImpl].
            assert (doResumeFunctionDescriptor.symbol in (caller as IrSimpleFunction).overriddenSymbols,
                    { "Expected 'CoroutineImpl.doResume' but was '$caller'" })
            currentCodeContext.genGetValue(caller.dispatchReceiverParameter!!)   // Coroutine itself is a continuation.
        }
    }

    private fun FunctionDescriptor.returnsUnit() = returnType.isUnit() && !isSuspend

    /**
     * Evaluates all arguments of [expression] that are explicitly represented in the IR.
     * Returns results in the same order as LLVM function expects, assuming that all explicit arguments
     * exactly correspond to a tail of LLVM parameters.
     */
    private fun evaluateExplicitArgs(expression: IrMemberAccessExpression): List<LLVMValueRef> {
        val evaluatedArgs = expression.getArguments().map { (param, argExpr) ->
            param to evaluateExpression(argExpr)
        }.toMap()

        val allValueParameters = expression.descriptor.allParameters

        return allValueParameters.dropWhile { it !in evaluatedArgs }.map {
            evaluatedArgs[it]!!
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateFunctionReference(expression: IrFunctionReference): LLVMValueRef {
        // TODO: consider creating separate IR element for pointer to function.
        assert (expression.type.getClass()?.descriptor == context.interopBuiltIns.cPointer)

        assert (expression.getArguments().isEmpty())

        val descriptor = expression.symbol.owner
        assert (descriptor.dispatchReceiverParameter == null)

        val entry = codegen.functionEntryPointAddress(descriptor)
        return entry
    }

    //-------------------------------------------------------------------------//

    private inner class SuspendableExpressionScope(val resumePoints: MutableList<LLVMBasicBlockRef>) : InnerScopeImpl() {
        override fun addResumePoint(bbLabel: LLVMBasicBlockRef): Int {
            val result = resumePoints.size
            resumePoints.add(bbLabel)
            return result
        }
    }

    private fun evaluateSuspendableExpression(expression: IrSuspendableExpression): LLVMValueRef {
        val suspensionPointId = evaluateExpression(expression.suspensionPointId)
        val bbStart = functionGenerationContext.basicBlock("start", expression.result.startLocation)
        val bbDispatch = functionGenerationContext.basicBlock("dispatch", expression.suspensionPointId.startLocation)

        val resumePoints = mutableListOf<LLVMBasicBlockRef>()
        using (SuspendableExpressionScope(resumePoints)) {
            functionGenerationContext.condBr(functionGenerationContext.icmpEq(suspensionPointId, kNullInt8Ptr), bbStart, bbDispatch)

            functionGenerationContext.positionAtEnd(bbStart)
            val result = evaluateExpression(expression.result)

            functionGenerationContext.appendingTo(bbDispatch) {
                if (context.config.indirectBranchesAreAllowed)
                    functionGenerationContext.indirectBr(suspensionPointId, resumePoints)
                else {
                    val bbElse = functionGenerationContext.basicBlock("else", null) {
                        functionGenerationContext.unreachable()
                    }

                    val cases = resumePoints.withIndex().map { Int32(it.index + 1).llvm to it.value }
                    functionGenerationContext.switch(functionGenerationContext.ptrToInt(suspensionPointId, int32Type), cases, bbElse)
                }
            }
            return result
        }
    }

    private inner class SuspensionPointScope(val suspensionPointId: VariableDescriptor,
                                             val bbResume: LLVMBasicBlockRef,
                                             val bbResumeId: Int): InnerScopeImpl() {
        override fun genGetValue(descriptor: ValueDescriptor): LLVMValueRef {
            if (descriptor == suspensionPointId) {
                return if (context.config.indirectBranchesAreAllowed)
                           functionGenerationContext.blockAddress(bbResume)
                       else
                           functionGenerationContext.intToPtr(Int32(bbResumeId + 1).llvm, int8TypePtr)
            }
            return super.genGetValue(descriptor)
        }
    }

    private fun evaluateSuspensionPoint(expression: IrSuspensionPoint): LLVMValueRef {
        val bbResume = functionGenerationContext.basicBlock("resume", expression.resumeResult.startLocation)
        val id = currentCodeContext.addResumePoint(bbResume)

        using (SuspensionPointScope(expression.suspensionPointIdParameter, bbResume, id)) {
            continuationBlock(expression.type, expression.result.startLocation).run {
                val normalResult = evaluateExpression(expression.result)
                jump(this, normalResult)

                functionGenerationContext.positionAtEnd(bbResume)
                val resumeResult = evaluateExpression(expression.resumeResult)
                jump(this, resumeResult)

                functionGenerationContext.positionAtEnd(this.block)
                return this.value
            }
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateFunctionCall(callee: IrCall, args: List<LLVMValueRef>,
                                     resultLifetime: Lifetime): LLVMValueRef {
        val descriptor = callee.symbol.owner

        val argsWithContinuationIfNeeded = if (descriptor.isSuspend)
                                               args + getContinuation()
                                           else args
        if (descriptor.isIntrinsic) {
            return evaluateIntrinsicCall(callee, argsWithContinuationIfNeeded)
        }

        if (callee is IrPrivateFunctionCall)
            return evaluatePrivateFunctionCall(callee, argsWithContinuationIfNeeded, callee.virtualCallee?.let { resultLifetime(it) } ?: resultLifetime)

        when {
            descriptor.origin == IrDeclarationOrigin.IR_BUILTINS_STUB ->
                return evaluateOperatorCall(callee, argsWithContinuationIfNeeded)

            descriptor is ConstructorDescriptor -> return evaluateConstructorCall(callee, argsWithContinuationIfNeeded)

            else -> return evaluateSimpleFunctionCall(
                    descriptor, argsWithContinuationIfNeeded, resultLifetime, callee.superQualifierSymbol?.owner)
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluatePrivateFunctionCall(callee: IrPrivateFunctionCall, args: List<LLVMValueRef>, resultLifetime: Lifetime): LLVMValueRef {
        val dfgSymbol = callee.dfgSymbol
        val functionIndex = callee.functionIndex
        val function = if (callee.moduleDescriptor == context.irModule!!.descriptor) {
            codegen.llvmFunction(context.privateFunctions[functionIndex].first)
        } else {
            context.llvm.externalFunction(
                    callee.moduleDescriptor.privateFunctionSymbolName(functionIndex, callee.dfgSymbol.name),
                    codegen.getLlvmFunctionType(dfgSymbol),
                    callee.moduleDescriptor.llvmSymbolOrigin

            )
        }
        return call(callee.symbol.owner, function, args, resultLifetime)
    }

    //-------------------------------------------------------------------------//

    private fun evaluatePrivateClassReference(classReference: IrPrivateClassReference): LLVMValueRef {
        val classIndex = classReference.classIndex
        val typeInfoPtr = if (classReference.moduleDescriptor == context.irModule!!.descriptor) {
            codegen.typeInfoValue(context.privateClasses[classIndex].first)
        } else {
            codegen.importGlobal(
                    classReference.moduleDescriptor.privateClassSymbolName(classIndex, classReference.dfgSymbol.name),
                    codegen.kTypeInfo,
                    classReference.moduleDescriptor.llvmSymbolOrigin
            )
        }
        return functionGenerationContext.bitcast(int8TypePtr, typeInfoPtr)
    }

    //-------------------------------------------------------------------------//

    private fun evaluateSimpleFunctionCall(
            descriptor: FunctionDescriptor, args: List<LLVMValueRef>,
            resultLifetime: Lifetime, superClass: ClassDescriptor? = null): LLVMValueRef {
        //context.log{"evaluateSimpleFunctionCall : $tmpVariableName = ${ir2string(value)}"}
        if (superClass == null && descriptor is SimpleFunctionDescriptor && descriptor.isOverridable)
            return callVirtual(descriptor, args, resultLifetime)
        else
            return callDirect(descriptor, args, resultLifetime)
    }

    //-------------------------------------------------------------------------//
    private fun resultLifetime(callee: IrElement): Lifetime {
        return lifetimes.getOrElse(callee) { /* TODO: make IRRELEVANT */ Lifetime.GLOBAL }
    }

    private fun evaluateConstructorCall(callee: IrCall, args: List<LLVMValueRef>): LLVMValueRef {
        context.log{"evaluateConstructorCall        : ${ir2string(callee)}"}
        return memScoped {
            val constructedClass = (callee.symbol as IrConstructorSymbol).owner.constructedClass
            val thisValue = if (constructedClass.isArray) {
                assert(args.isNotEmpty() && args[0].type == int32Type)
                functionGenerationContext.allocArray(codegen.typeInfoValue(constructedClass), args[0],
                        resultLifetime(callee))
            } else if (constructedClass == context.ir.symbols.string.owner) {
                // TODO: consider returning the empty string literal instead.
                assert(args.isEmpty())
                functionGenerationContext.allocArray(codegen.typeInfoValue(constructedClass), count = kImmZero,
                        lifetime = resultLifetime(callee))
            } else if (constructedClass.isObjCClass()) {
                assert(constructedClass.isKotlinObjCClass()) // Calls to other ObjC class constructors must be lowered.
                val symbols = context.ir.symbols
                val rawPtr = callDirect(
                        symbols.interopAllocObjCObject.owner,
                        listOf(genGetObjCClass(constructedClass)),
                        Lifetime.IRRELEVANT
                )

                callDirect(symbols.interopInterpretObjCPointer.owner, listOf(rawPtr), resultLifetime(callee)).also {
                    // Balance pointer retained by alloc:
                    callDirect(symbols.interopObjCRelease.owner, listOf(rawPtr), Lifetime.IRRELEVANT)
                }
            } else {
                functionGenerationContext.allocInstance(constructedClass, resultLifetime(callee))
            }
            evaluateSimpleFunctionCall(callee.symbol.owner,
                    listOf(thisValue) + args, Lifetime.IRRELEVANT /* constructor doesn't return anything */)
            thisValue
        }
    }

    //-------------------------------------------------------------------------//

    private fun evaluateIntrinsicCall(callee: IrCall, args: List<LLVMValueRef>): LLVMValueRef {
        val descriptor = callee.descriptor.original
        val function = callee.symbol.owner
        val name = descriptor.fqNameUnsafe.asString()

        when (name) {
            "konan.internal.areEqualByValue" -> {
                val arg0 = args[0]
                val arg1 = args[1]
                assert (arg0.type == arg1.type,
                        { "Types are different: '${llvmtype2string(arg0.type)}' and '${llvmtype2string(arg1.type)}'" })

                return functionGenerationContext.icmpEq(arg0, arg1)
            }
            "konan.internal.ieee754Equals" -> {
                val arg0 = args[0]
                val arg1 = args[1]
                assert (arg0.type == arg1.type,
                        { "Types are different: '${llvmtype2string(arg0.type)}' and '${llvmtype2string(arg1.type)}'" })
                val type = LLVMGetTypeKind(arg0.type)
                assert (type == LLVMTypeKind.LLVMFloatTypeKind || type == LLVMTypeKind.LLVMDoubleTypeKind,
                        { "Should be of floating point kind, not: '${llvmtype2string(arg0.type)}'"})
                return functionGenerationContext.fcmpEq(arg0, arg1)

            }
            "konan.internal.getContinuation" -> return getContinuation()
        }

        val interop = context.interopBuiltIns

        return when (descriptor) {
            interop.interpretNullablePointed, interop.interpretCPointer,
            interop.nativePointedGetRawPointer, interop.cPointerGetRawValue -> args.single()

            in interop.readPrimitive -> {
                val pointerType = pointerType(codegen.getLLVMType(function.returnType))
                val rawPointer = args.last()
                val pointer = functionGenerationContext.bitcast(pointerType, rawPointer)
                functionGenerationContext.load(pointer)
            }
            in interop.writePrimitive -> {
                val pointerType = pointerType(codegen.getLLVMType(function.valueParameters.last().type))
                val rawPointer = args[1]
                val pointer = functionGenerationContext.bitcast(pointerType, rawPointer)
                functionGenerationContext.store(args[2], pointer)
                codegen.theUnitInstanceRef.llvm
            }
            context.builtIns.nativePtrPlusLong -> functionGenerationContext.gep(args[0], args[1])
            context.builtIns.getNativeNullPtr -> kNullInt8Ptr
            interop.getPointerSize -> Int32(LLVMPointerSize(codegen.llvmTargetData)).llvm
            context.builtIns.nativePtrToLong -> {
                val intPtrValue = functionGenerationContext.ptrToInt(args.single(), codegen.intPtrType)
                val resultType = functionGenerationContext.getLLVMType(function.returnType)

                if (resultType == intPtrValue.type) {
                    intPtrValue
                } else {
                    LLVMBuildSExt(functionGenerationContext.builder, intPtrValue, resultType, "")!!
                }
            }

            interop.getObjCReceiverOrSuper -> {
                genGetObjCReceiverOrSuper(args)
            }

            interop.getObjCClass -> {
                val typeArgument = callee.getTypeArgument(descriptor.typeParameters.single())
                val irClass = typeArgument!!.getClass()!!
                genGetObjCClass(irClass)
            }

            interop.getObjCMessenger -> {
                genGetObjCMessenger(args, isStret = false)
            }
            interop.getObjCMessengerStret -> {
                genGetObjCMessenger(args, isStret = true)
            }

            interop.readBits -> genReadBits(args)
            interop.writeBits -> genWriteBits(args)

            context.ir.symbols.getClassTypeInfo.descriptor -> {
                val typeArgument = callee.getTypeArgument(0)!!
                val typeArgumentClass = typeArgument.getClass()
                if (typeArgumentClass == null) {
                    // E.g. for `T::class` in a body of an inline function itself.
                    functionGenerationContext.unreachable()
                    kNullInt8Ptr
                } else {
                    val irClass = context.ir.symbols.valueClassToBox[typeArgumentClass.symbol]?.owner
                            ?: typeArgumentClass

                    val typeInfo = codegen.typeInfoValue(irClass)
                    LLVMConstBitCast(typeInfo, kInt8Ptr)!!
                }
            }

            context.ir.symbols.createUninitializedInstance.descriptor -> {
                val typeParameterT = context.ir.symbols.createUninitializedInstance.descriptor.typeParameters[0]
                val enumClass = callee.getTypeArgument(typeParameterT)!!
                val enumIrClass = enumClass.getClass()!!
                functionGenerationContext.allocInstance(enumIrClass, resultLifetime(callee))
            }

            context.ir.symbols.listOfInternal.descriptor -> {
                val varargExpression = callee.getValueArgument(0) as IrVararg
                val vararg = args.single()

                val length = varargExpression.elements.size
                // TODO: store length in `vararg` itself when more abstract types will be used for values.

                val array = constPointer(vararg)
                // Note: dirty hack here: `vararg` has type `Array<out E>`, but `createArrayList` expects `Array<E>`;
                // however `vararg` is immutable, and in current implementation it has type `Array<E>`,
                // so let's ignore this mismatch currently for simplicity.

                return context.llvm.staticData.createArrayList(array, length).llvm
            }

            else -> TODO(callee.descriptor.original.toString())
        }
    }

    private fun genReadBits(args: List<LLVMValueRef>): LLVMValueRef {
        val ptr = args[0]
        assert(ptr.type == int8TypePtr)

        val offset = extractConstUnsignedInt(args[1])
        val size = extractConstUnsignedInt(args[2]).toInt()
        val signed = extractConstUnsignedInt(args[3]) != 0L

        val prefixBitsNum = (offset % 8).toInt()
        val suffixBitsNum = (8 - ((size + offset) % 8).toInt()) % 8

        // Note: LLVM allows to read without padding tail up to byte boundary, but the result seems to be incorrect.

        val bitsWithPaddingNum = prefixBitsNum + size + suffixBitsNum
        val bitsWithPaddingType = LLVMIntType(bitsWithPaddingNum)!!

        with (functionGenerationContext) {
            val bitsWithPaddingPtr = bitcast(pointerType(bitsWithPaddingType), gep(ptr, Int64(offset / 8).llvm))
            val bitsWithPadding = load(bitsWithPaddingPtr).setUnaligned()

            val bits = shr(
                    shl(bitsWithPadding, suffixBitsNum),
                    prefixBitsNum + suffixBitsNum, signed
            )

            return if (bitsWithPaddingNum == 64) {
                bits
            } else if (bitsWithPaddingNum > 64) {
                trunc(bits, int64Type)
            } else {
                ext(bits, int64Type, signed)
            }
        }
    }

    private fun genWriteBits(args: List<LLVMValueRef>): LLVMValueRef {
        val ptr = args[0]
        assert(ptr.type == int8TypePtr)

        val offset = extractConstUnsignedInt(args[1])
        val size = extractConstUnsignedInt(args[2]).toInt()

        val value = args[3]
        assert(value.type == int64Type)

        val bitsType = LLVMIntType(size)!!

        val prefixBitsNum = (offset % 8).toInt()
        val suffixBitsNum = (8 - ((size + offset) % 8).toInt()) % 8

        val bitsWithPaddingNum = prefixBitsNum + size + suffixBitsNum
        val bitsWithPaddingType = LLVMIntType(bitsWithPaddingNum)!!

        // 0011111000:
        val discardBitsMask = LLVMConstShl(
                LLVMConstZExt(
                        LLVMConstAllOnes(bitsType), // 11111
                        bitsWithPaddingType
                ), // 1111100000
                LLVMConstInt(bitsWithPaddingType, prefixBitsNum.toLong(), 0)
        )

        val preservedBitsMask = LLVMConstNot(discardBitsMask)!!

        with (functionGenerationContext) {
            val bitsWithPaddingPtr = bitcast(pointerType(bitsWithPaddingType), gep(ptr, Int64(offset / 8).llvm))

            val bits = trunc(value, bitsType)

            val bitsToStore = if (prefixBitsNum == 0 && suffixBitsNum == 0) {
                bits
            } else {
                val previousValue = load(bitsWithPaddingPtr).setUnaligned()
                val preservedBits = and(previousValue, preservedBitsMask)
                val bitsWithPadding = shl(zext(bits, bitsWithPaddingType), prefixBitsNum)

                or(bitsWithPadding, preservedBits)
            }

            LLVMBuildStore(builder, bitsToStore, bitsWithPaddingPtr)!!.setUnaligned()
        }

        return codegen.theUnitInstanceRef.llvm
    }

    private fun extractConstUnsignedInt(value: LLVMValueRef): Long {
        assert(LLVMIsConstant(value) != 0)
        return LLVMConstIntGetZExtValue(value)
    }

    private fun genGetObjCClass(classDescriptor: ClassDescriptor): LLVMValueRef {
        return functionGenerationContext.getObjCClass(classDescriptor, currentCodeContext.exceptionHandler)
    }

    private fun genGetObjCProtocol(irClass: IrClass): LLVMValueRef {
        // Note: this function will return the same result for Obj-C protocol and corresponding meta-class.

        assert(irClass.isInterface)
        assert(irClass.isExternalObjCClass())

        val annotation = irClass.descriptor.annotations.findAnnotation(externalObjCClassFqName)!!
        val protocolGetterName = annotation.getStringValue("protocolGetter")
        val protocolGetter = context.llvm.externalFunction(
                protocolGetterName,
                functionType(int8TypePtr, false),
                irClass.llvmSymbolOrigin
        )

        return call(protocolGetter, emptyList())
    }

    private fun genGetObjCMessenger(args: List<LLVMValueRef>, isStret: Boolean): LLVMValueRef {
        val gen = functionGenerationContext

        val messengerNameSuffix = if (isStret) "_stret" else ""

        val functionType = functionType(int8TypePtr, true, int8TypePtr, int8TypePtr)

        val libobjc = context.standardLlvmSymbolsOrigin
        val normalMessenger = context.llvm.externalFunction(
                "objc_msgSend$messengerNameSuffix",
                functionType,
                origin = libobjc
        )
        val superMessenger = context.llvm.externalFunction(
                "objc_msgSendSuper$messengerNameSuffix",
                functionType,
                origin = libobjc
        )

        val superClass = args.single()
        val messenger = LLVMBuildSelect(gen.builder,
                If = gen.icmpEq(superClass, kNullInt8Ptr),
                Then = normalMessenger,
                Else = superMessenger,
                Name = ""
        )!!

        return gen.bitcast(int8TypePtr, messenger)
    }

    private fun genGetObjCReceiverOrSuper(args: List<LLVMValueRef>): LLVMValueRef {
        val gen = functionGenerationContext

        assert(args.size == 2)
        val receiver = args[0]
        val superClass = args[1]

        val superClassIsNull = gen.icmpEq(superClass, kNullInt8Ptr)

        return gen.ifThenElse(superClassIsNull, receiver) {
            val structType = structType(kInt8Ptr, kInt8Ptr)
            val ptr = gen.alloca(structType)
            gen.store(receiver,
                    LLVMBuildGEP(gen.builder, ptr, cValuesOf(kImmZero, kImmZero), 2, "")!!)

            gen.store(superClass,
                    LLVMBuildGEP(gen.builder, ptr, cValuesOf(kImmZero, kImmOne), 2, "")!!)

            gen.bitcast(int8TypePtr, ptr)
        }
    }

    //-------------------------------------------------------------------------//
    private val kImmZero     = LLVMConstInt(LLVMInt32Type(),  0, 1)!!
    private val kImmOne      = LLVMConstInt(LLVMInt32Type(),  1, 1)!!
    private val kTrue        = LLVMConstInt(LLVMInt1Type(),   1, 1)!!
    private val kFalse       = LLVMConstInt(LLVMInt1Type(),   0, 1)!!

    private fun evaluateOperatorCall(callee: IrCall, args: List<LLVMValueRef>): LLVMValueRef {
        context.log{"evaluateOperatorCall           : origin:${ir2string(callee)}"}
        val descriptor = callee.symbol.owner
        val ib = context.irModule!!.irBuiltins

        with(functionGenerationContext) {
            return when {
                descriptor == ib.eqeqeqFun -> icmpEq(args[0], args[1])
                descriptor == ib.booleanNotFun -> icmpNe(args[0], kTrue)

                descriptor.isComparisonDescriptor(ib.greaterFunByOperandType) -> {
                    if (args[0].type.isFloatingPoint()) fcmpGt(args[0], args[1])
                    else icmpGt(args[0], args[1])
                }
                descriptor.isComparisonDescriptor(ib.greaterOrEqualFunByOperandType) -> {
                    if (args[0].type.isFloatingPoint()) fcmpGe(args[0], args[1])
                    else icmpGe(args[0], args[1])
                }
                descriptor.isComparisonDescriptor(ib.lessFunByOperandType) -> {
                    if (args[0].type.isFloatingPoint()) fcmpLt(args[0], args[1])
                    else icmpLt(args[0], args[1])
                }
                descriptor.isComparisonDescriptor(ib.lessOrEqualFunByOperandType) -> {
                    if (args[0].type.isFloatingPoint()) fcmpLe(args[0], args[1])
                    else icmpLe(args[0], args[1])
                }
                else -> TODO(descriptor.name.toString())
            }
        }
    }

    //-------------------------------------------------------------------------//

    private fun generateWhenCase(resultPhi: LLVMValueRef?, branch: IrBranch,
                                 bbNext: LLVMBasicBlockRef?, bbExit: LLVMBasicBlockRef?) {
        val branchResult = branch.result
        val brResult = if (isUnconditional(branch)) {                                 // It is the "else" clause.
            evaluateExpression(branchResult)                                          // Generate clause body.
        } else {                                                                      // It is conditional clause.
            val bbCase = functionGenerationContext.basicBlock("when_case", branch.startLocation) // Create block for clause body.
            val condition = evaluateExpression(branch.condition)                      // Generate cmp instruction.
            functionGenerationContext.condBr(condition, bbCase, bbNext)               // Conditional branch depending on cmp result.
            functionGenerationContext.positionAtEnd(bbCase)                           // Switch generation to block for clause body.
            evaluateExpression(branch.result)                                         // Generate clause body.
        }
        if (!functionGenerationContext.isAfterTerminator()) {
            if (resultPhi != null)
                functionGenerationContext.assignPhis(resultPhi to brResult)
            if (bbExit != null)
                functionGenerationContext.br(bbExit)
            else
                functionGenerationContext.unreachable()
        }
        if (bbNext != null)                                                          // Switch generation to next or exit.
            functionGenerationContext.positionAtEnd(bbNext)
        else if (bbExit != null)
            functionGenerationContext.positionAtEnd(bbExit)
    }

    //-------------------------------------------------------------------------//
    // Checks if the branch is unconditional

    private fun isUnconditional(branch: IrBranch): Boolean =
        branch.condition is IrConst<*>                            // If branch condition is constant.
            && (branch.condition as IrConst<*>).value as Boolean  // If condition is "true"

    //-------------------------------------------------------------------------//

    fun callDirect(descriptor: FunctionDescriptor, args: List<LLVMValueRef>,
                   resultLifetime: Lifetime): LLVMValueRef {
        val realDescriptor = descriptor.target
        val llvmFunction = codegen.llvmFunction(realDescriptor)
        return call(descriptor, llvmFunction, args, resultLifetime)
    }

    //-------------------------------------------------------------------------//

    fun callVirtual(descriptor: FunctionDescriptor, args: List<LLVMValueRef>,
                    resultLifetime: Lifetime): LLVMValueRef {

        val function = functionGenerationContext.lookupVirtualImpl(args.first(), descriptor)

        return call(descriptor, function, args, resultLifetime)                      // Invoke the method
    }

    //-------------------------------------------------------------------------//

    // TODO: it seems to be much more reliable to get args as a mapping from parameter descriptor to LLVM value,
    // instead of a plain list.
    // In such case it would be possible to check that all args are available and in the correct order.
    // However, it currently requires some refactoring to be performed.
    private fun call(descriptor: FunctionDescriptor, function: LLVMValueRef, args: List<LLVMValueRef>,
                     resultLifetime: Lifetime): LLVMValueRef {
        val result = call(function, args, resultLifetime)
        if (descriptor.returnType.isNothing()) {
            functionGenerationContext.unreachable()
        }

        if (LLVMGetReturnType(getFunctionType(function)) == voidType) {
            return codegen.theUnitInstanceRef.llvm
        }

        return result
    }

    private fun call(function: LLVMValueRef, args: List<LLVMValueRef>,
                     resultLifetime: Lifetime = Lifetime.IRRELEVANT): LLVMValueRef {
        return functionGenerationContext.call(function, args, resultLifetime, currentCodeContext.exceptionHandler)
    }

    //-------------------------------------------------------------------------//

    private fun delegatingConstructorCall(
            descriptor: ClassConstructorDescriptor, args: List<LLVMValueRef>): LLVMValueRef {

        val constructedClass = functionGenerationContext.constructedClass!!
        val thisPtr = currentCodeContext.genGetValue(constructedClass.thisReceiver!!)

        if (constructedClass.isObjCClass()) {
            return codegen.theUnitInstanceRef.llvm
        }

        val thisPtrArgType = codegen.getLLVMType(descriptor.allParameters[0].type)
        val thisPtrArg = if (thisPtr.type == thisPtrArgType) {
            thisPtr
        } else {
            // e.g. when array constructor calls super (i.e. Any) constructor.
            functionGenerationContext.bitcast(thisPtrArgType, thisPtr)
        }

        return callDirect(descriptor, listOf(thisPtrArg) + args,
                Lifetime.IRRELEVANT /* no value returned */)
    }

    //-------------------------------------------------------------------------//

    private fun appendLlvmUsed(name: String, args: List<LLVMValueRef>) {
        if (args.isEmpty()) return

        memScoped {
            val argsCasted = args.map { it -> constPointer(it).bitcast(int8TypePtr) }
            val llvmUsedGlobal =
                context.llvm.staticData.placeGlobalArray(name, int8TypePtr, argsCasted)

            LLVMSetLinkage(llvmUsedGlobal.llvmGlobal, LLVMLinkage.LLVMAppendingLinkage)
            LLVMSetSection(llvmUsedGlobal.llvmGlobal, "llvm.metadata")
        }
    }

    //-------------------------------------------------------------------------//

    private fun entryPointSelector(entryPoint: LLVMValueRef,
        entryPointType: LLVMTypeRef, selectorName: String): LLVMValueRef {

        assert(LLVMCountParams(entryPoint) == 1)

        val selector = LLVMAddFunction(context.llvmModule, selectorName, entryPointType)!!
        generateFunction(codegen, selector) {
            // Note, that 'parameter' is an object reference, and as such, shall
            // be accounted for in the rootset. However, current object management
            // scheme for arguments guarantees, that reference is being held in C++
            // launcher, so we could optimize out creating slot for 'parameter' in
            // this function.
            val parameter = LLVMGetParam(selector, 0)!!
            call(entryPoint, listOf(parameter), Lifetime.IRRELEVANT, ExceptionHandler.Caller)
            ret(null)
        }
        return selector
    }

    //-------------------------------------------------------------------------//

    private fun appendEntryPointSelector(descriptor: FunctionDescriptor?) {
        if (descriptor == null) return

        val entryPoint = codegen.llvmFunction(descriptor)
        val selectorName = "EntryPointSelector"
        val entryPointType = getFunctionType(entryPoint)

        val selector = entryPointSelector(entryPoint, entryPointType, selectorName)

        LLVMSetLinkage(selector, LLVMLinkage.LLVMExternalLinkage)
    }


    //-------------------------------------------------------------------------//
    // Create type { i32, void ()*, i8* }

    val kCtorType = LLVMStructType(
            cValuesOf(
                    LLVMInt32Type(),
                    LLVMPointerType(kVoidFuncType, 0),
                    kInt8Ptr
            ),
            3, 0)!!

    //-------------------------------------------------------------------------//
    // Create object { i32, void ()*, i8* } { i32 1, void ()* @ctorFunction, i8* null }

    fun createGlobalCtor(ctorFunction: LLVMValueRef): ConstPointer {
        val priority = kImmInt32One
        val data     = kNullInt8Ptr
        val argList  = cValuesOf(priority, ctorFunction, data)
        val ctorItem = LLVMConstNamedStruct(kCtorType, argList, 3)!!
        return constPointer(ctorItem)
    }

    //-------------------------------------------------------------------------//
    fun appendStaticInitializers() {
        val ctorName = context.config.moduleId.moduleConstructorName
        val ctorFunction = LLVMAddFunction(context.llvmModule, ctorName, kVoidFuncType)!!
        LLVMSetLinkage(ctorFunction, LLVMLinkage.LLVMExternalLinkage)
        generateFunction(codegen, ctorFunction) {
            val initGuard = LLVMAddGlobal(context.llvmModule, int32Type, "Konan_init_guard")
            LLVMSetInitializer(initGuard, kImmZero)
            LLVMSetLinkage(initGuard, LLVMLinkage.LLVMPrivateLinkage)
            val bbInited = basicBlock("inited", null)
            val bbNeedInit = basicBlock("need_init", null)

            val value = LLVMBuildLoad(builder, initGuard, "")!!
            condBr(icmpEq(value, kImmZero), bbNeedInit, bbInited)

            appendingTo(bbInited) {
                ret(null)
            }

            appendingTo(bbNeedInit) {
                LLVMBuildStore(builder, kImmOne, initGuard)

                if (context.config.produce.isNativeBinary) {
                    context.llvm.librariesToLink.forEach {
                        val dependencyCtorFunction = context.llvm.externalFunction(
                                it.moduleConstructorName, kVoidFuncType, CurrentKonanModule)
                        call(dependencyCtorFunction, emptyList(), Lifetime.IRRELEVANT,
                                exceptionHandler = ExceptionHandler.Caller, verbatim = true)
                    }
                }
                // TODO: shall we put that into the try block?
                context.llvm.staticInitializers.forEach {
                    call(it, emptyList(), Lifetime.IRRELEVANT,
                            exceptionHandler = ExceptionHandler.Caller, verbatim = true)
                }
                ret(null)
            }
        }

        if (context.config.produce.isNativeBinary) {
            // Append initializers of global variables in "llvm.global_ctors" array.
            val globalCtors = context.llvm.staticData.placeGlobalArray("llvm.global_ctors", kCtorType,
                    listOf(createGlobalCtor(ctorFunction)))
            LLVMSetLinkage(globalCtors.llvmGlobal, LLVMLinkage.LLVMAppendingLinkage)
            if (context.config.produce == CompilerOutputKind.PROGRAM) {
                // Provide an optional handle for calling .ctors, if standard constructors mechanism
                // is not available on the platform (i.e. WASM, embedded).
                val globalCtorFunction = LLVMAddFunction(context.llvmModule, "_Konan_constructors", kVoidFuncType)!!
                LLVMSetLinkage(globalCtorFunction, LLVMLinkage.LLVMExternalLinkage)
                generateFunction(codegen, globalCtorFunction) {
                    // Unfortunately, LLVMAddAlias() doesn't seem to work on WASM yet.
                    call(ctorFunction, emptyList(), Lifetime.IRRELEVANT,
                            exceptionHandler = ExceptionHandler.Caller, verbatim = true)
                    ret(null)
                }
                appendLlvmUsed("llvm.used", listOf(globalCtorFunction))
            }
        }
    }

    //-------------------------------------------------------------------------//

    fun FunctionGenerationContext.basicBlock(name: String, locationInfo: LocationInfo?, code: () -> Unit) = functionGenerationContext.basicBlock(name, locationInfo).apply {
        appendingTo(this) {
            code()
        }
    }
}

internal data class LocationInfo(val scope:DIScopeOpaqueRef,
                                 val line:Int,
                                 val column:Int)
