/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.common.lower.BOUND_RECEIVER_PARAMETER
import org.jetbrains.kotlin.backend.common.lower.SYNTHESIZED_INIT_BLOCK
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.JvmLoweredStatementOrigin
import org.jetbrains.kotlin.backend.jvm.intrinsics.IrIntrinsicMethods
import org.jetbrains.kotlin.backend.jvm.intrinsics.JavaClassProperty
import org.jetbrains.kotlin.backend.jvm.lower.MultifileFacadeFileEntry
import org.jetbrains.kotlin.backend.jvm.lower.constantValue
import org.jetbrains.kotlin.backend.jvm.lower.inlineclasses.unboxInlineClass
import org.jetbrains.kotlin.backend.jvm.lower.suspendFunctionOriginal
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.AsmUtil.*
import org.jetbrains.kotlin.codegen.DescriptorAsmUtil.*
import org.jetbrains.kotlin.codegen.coroutines.SuspensionPointKind
import org.jetbrains.kotlin.codegen.coroutines.generateCoroutineSuspendedCheck
import org.jetbrains.kotlin.codegen.inline.*
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner.Companion.putNeedClassReificationMarker
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner.OperationKind.AS
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner.OperationKind.SAFE_AS
import org.jetbrains.kotlin.codegen.intrinsics.TypeIntrinsics
import org.jetbrains.kotlin.codegen.pseudoInsns.fakeAlwaysFalseIfeq
import org.jetbrains.kotlin.codegen.pseudoInsns.fixStackAndJump
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.isReleaseCoroutines
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.descriptors.toIrBasedKotlinType
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.*
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.model.TypeParameterMarker
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.kotlin.utils.keysToMap
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import java.util.*

sealed class ExpressionInfo

class LoopInfo(val loop: IrLoop, val continueLabel: Label, val breakLabel: Label) : ExpressionInfo()

open class TryInfo : ExpressionInfo() {
    // Regions corresponding to copy-pasted contents of the `finally` block.
    // These should not be covered by `catch` clauses.
    val gaps = mutableListOf<Pair<Label, Label>>()
}

class TryWithFinallyInfo(val onExit: IrExpression) : TryInfo()

class BlockInfo(val parent: BlockInfo? = null) {
    val variables = mutableListOf<VariableInfo>()
    private val infos: Stack<ExpressionInfo> = parent?.infos ?: Stack()

    fun hasFinallyBlocks(): Boolean = infos.firstIsInstanceOrNull<TryWithFinallyInfo>() != null

    internal inline fun <T : ExpressionInfo, R> withBlock(info: T, f: (T) -> R): R {
        infos.add(info)
        try {
            return f(info)
        } finally {
            infos.pop()
        }
    }

    internal inline fun <R> handleBlock(f: (ExpressionInfo) -> R): R? {
        if (infos.isEmpty()) {
            return null
        }
        val top = infos.pop()
        try {
            return f(top)
        } finally {
            infos.add(top)
        }
    }
}

class VariableInfo(val declaration: IrVariable, val index: Int, val type: Type, val startLabel: Label)

class ExpressionCodegen(
    val irFunction: IrFunction,
    val signature: JvmMethodSignature,
    override val frameMap: IrFrameMap,
    val mv: InstructionAdapter,
    val classCodegen: ClassCodegen,
    val inlinedInto: ExpressionCodegen?,
    val smap: SourceMapper,
) : IrElementVisitor<PromisedValue, BlockInfo>, BaseExpressionCodegen {

    var finallyDepth = 0

    val context = classCodegen.context
    val typeMapper = context.typeMapper
    val methodSignatureMapper = context.methodSignatureMapper

    val state = context.state

    private val fileEntry = classCodegen.context.psiSourceManager.getFileEntry(irFunction.fileParent)

    override val visitor: InstructionAdapter
        get() = mv

    override val inlineNameGenerator: NameGenerator = classCodegen.getRegeneratedObjectNameGenerator(irFunction)

    override val typeSystem: TypeSystemCommonBackendContext
        get() = typeMapper.typeSystem

    override var lastLineNumber: Int = -1
    var noLineNumberScope: Boolean = false

    private var isInsideCondition = false

    private val closureReifiedMarkers = hashMapOf<IrClass, ReifiedTypeParametersUsages>()

    private val IrType.asmType: Type
        get() = typeMapper.mapType(this)

    val IrExpression.asmType: Type
        get() = type.asmType

    val IrValueDeclaration.asmType: Type
        get() = type.asmType

    // Assume this expression's result has already been materialized on the stack
    // with the correct type.
    val IrExpression.onStack: MaterialValue
        get() = MaterialValue(this@ExpressionCodegen, asmType, type)

    private fun markNewLabel() = Label().apply { mv.visitLabel(this) }

    private fun markNewLinkedLabel() = linkedLabel().apply { mv.visitLabel(this) }

    private fun getLineNumberForOffset(offset: Int): Int = fileEntry?.getLineNumber(offset)?.plus(1) ?: -1

    private fun IrElement.markLineNumber(startOffset: Boolean) {
        if (noLineNumberScope) return
        val offset = if (startOffset) this.startOffset else endOffset
        if (offset < 0) {
            return
        }
        if (fileEntry != null) {
            val lineNumber = getLineNumberForOffset(offset)
            assert(lineNumber > 0)
            if (lastLineNumber != lineNumber) {
                lastLineNumber = lineNumber
                mv.visitLineNumber(lineNumber, markNewLabel())
            }
        }
    }

    fun markLineNumber(element: IrElement) = element.markLineNumber(true)

    fun noLineNumberScope(block: () -> Unit) {
        val previousState = noLineNumberScope
        noLineNumberScope = true
        block()
        noLineNumberScope = previousState
    }

    // TODO remove
    fun gen(expression: IrExpression, type: Type, irType: IrType, data: BlockInfo): StackValue {
        if (expression.attributeOwnerId === context.fakeContinuation) {
            addFakeContinuationMarker(mv)
        } else {
            expression.accept(this, data).materializeAt(type, irType)
        }
        return StackValue.onStack(type, irType.toIrBasedKotlinType())
    }

    internal fun genOrGetLocal(expression: IrExpression, data: BlockInfo): StackValue {
        if (irFunction.origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER) {
            if (expression is IrTypeOperatorCall && expression.operator == IrTypeOperator.IMPLICIT_CAST) {
                // inline lambda parameters are passed from `foo$default` to `foo` call with implicit cast,
                // we need return pure StackValue.local value to be able proper inline this parameter later
                if (expression.type.makeNullable() == expression.argument.type) {
                    return genOrGetLocal(expression.argument, data)
                }
            }
        }

        return if (expression is IrGetValue)
            StackValue.local(findLocalIndex(expression.symbol), frameMap.typeOf(expression.symbol), expression.type.toIrBasedKotlinType())
        else
            gen(expression, typeMapper.mapType(expression.type), expression.type, data)
    }

    fun generate() {
        mv.visitCode()
        val startLabel = markNewLabel()
        val info = BlockInfo()
        val body = irFunction.body!!
        generateNonNullAssertions()
        generateFakeContinuationConstructorIfNeeded()
        val result = body.accept(this, info)
        // If this function has an expression body, return the result of that expression.
        // Otherwise, if it does not end in a return statement, it must be void-returning,
        // and an explicit return instruction at the end is still required to pass validation.
        if (body !is IrStatementContainer || body.statements.lastOrNull() !is IrReturn) {
            // Allow setting a breakpoint on the closing brace of a void-returning function
            // without an explicit return, or the `class Something(` line of a primary constructor.
            if (irFunction.origin != JvmLoweredDeclarationOrigin.CLASS_STATIC_INITIALIZER) {
                irFunction.markLineNumber(startOffset = irFunction is IrConstructor && irFunction.isPrimary)
            }
            if (irFunction.isSuspend && irFunction.origin == IrDeclarationOrigin.BRIDGE) {
                mv.areturn(OBJECT_TYPE)
            } else {
                var returnType = signature.returnType
                var returnIrType = if (irFunction !is IrConstructor) irFunction.returnType else context.irBuiltIns.unitType
                val unboxedInlineClass =
                    irFunction.suspendFunctionOriginal().originalReturnTypeOfSuspendFunctionReturningUnboxedInlineClass()
                if (unboxedInlineClass != null) {
                    returnIrType = unboxedInlineClass
                    returnType = unboxedInlineClass.asmType
                }
                result.materializeAt(returnType, returnIrType)
                mv.areturn(returnType)
            }
        }
        val endLabel = markNewLabel()
        writeLocalVariablesInTable(info, endLabel)
        writeParameterInLocalVariableTable(startLabel, endLabel)
    }

    private fun generateFakeContinuationConstructorIfNeeded() {
        val continuationClass = irFunction.continuationClass() ?: return
        val continuationType = typeMapper.mapClass(continuationClass)
        val continuationIndex = frameMap.getIndex(irFunction.continuationParameter()!!.symbol)
        with(mv) {
            addFakeContinuationConstructorCallMarker(this, true)
            anew(continuationType)
            dup()
            if (irFunction.dispatchReceiverParameter != null) {
                load(0, OBJECT_TYPE)
                load(continuationIndex, Type.getObjectType("kotlin/coroutines/Continuation"))
                invokespecial(continuationType.internalName, "<init>", "(${classCodegen.type}Lkotlin/coroutines/Continuation;)V", false)
            } else {
                load(continuationIndex, Type.getObjectType("kotlin/coroutines/Continuation"))
                invokespecial(continuationType.internalName, "<init>", "(Lkotlin/coroutines/Continuation;)V", false)
            }
            addFakeContinuationConstructorCallMarker(this, false)
            pop()
        }
    }

    private fun generateNonNullAssertions() {
        if (state.isParamAssertionsDisabled)
            return

        val notCallableFromJava = inlinedInto != null ||
                (DescriptorVisibilities.isPrivate(irFunction.visibility) && !(irFunction is IrSimpleFunction && irFunction.isOperator)) ||
                irFunction.origin.isSynthetic ||
                // TODO: refine this condition to not generate nullability assertions on parameters
                //       corresponding to captured variables and anonymous object super constructor arguments
                (irFunction is IrConstructor && irFunction.parentAsClass.isAnonymousObject) ||
                // TODO: Implement this as a lowering, so that we can more easily exclude generated methods.
                irFunction.origin == JvmLoweredDeclarationOrigin.INLINE_CLASS_GENERATED_IMPL_METHOD ||
                // Although these are accessible from Java, the functions they bridge to already have the assertions.
                irFunction.origin == IrDeclarationOrigin.BRIDGE_SPECIAL ||
                irFunction.origin == JvmLoweredDeclarationOrigin.DEFAULT_IMPLS_BRIDGE ||
                irFunction.origin == JvmLoweredDeclarationOrigin.JVM_STATIC_WRAPPER ||
                irFunction.origin == JvmLoweredDeclarationOrigin.MULTIFILE_BRIDGE ||
                irFunction.parentAsClass.origin == JvmLoweredDeclarationOrigin.CONTINUATION_CLASS ||
                irFunction.parentAsClass.origin == JvmLoweredDeclarationOrigin.SUSPEND_LAMBDA

        if (notCallableFromJava)
            return

        // Do not generate non-null checks for suspend functions. When resumed the arguments
        // will be null and the actual values are taken from the continuation.

        if (irFunction.isSuspend)
            return

        irFunction.extensionReceiverParameter?.let { generateNonNullAssertion(it) }
        irFunction.valueParameters.forEach(::generateNonNullAssertion)
    }

    private fun generateNonNullAssertion(param: IrValueParameter) {
        val asmType = param.type.asmType
        if (!param.type.unboxInlineClass().isNullable() && !isPrimitive(asmType)) {
            mv.load(findLocalIndex(param.symbol), asmType)
            mv.aconst(param.name.asString())
            val methodName = if (state.unifiedNullChecks) "checkNotNullParameter" else "checkParameterIsNotNull"
            mv.invokestatic(IrIntrinsicMethods.INTRINSICS_CLASS_NAME, methodName, "(Ljava/lang/Object;Ljava/lang/String;)V", false)
        }
    }

    private fun writeParameterInLocalVariableTable(startLabel: Label, endLabel: Label) {
        if (!irFunction.isInline && irFunction.origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER) return
        if (!irFunction.isStatic) {
            mv.visitLocalVariable("this", classCodegen.type.descriptor, null, startLabel, endLabel, 0)
        }
        val extensionReceiverParameter = irFunction.extensionReceiverParameter
        if (extensionReceiverParameter != null) {
            writeValueParameterInLocalVariableTable(extensionReceiverParameter, startLabel, endLabel, true)
        }
        for (param in irFunction.valueParameters) {
            if (param.origin == IrDeclarationOrigin.MASK_FOR_DEFAULT_FUNCTION || param.origin == IrDeclarationOrigin.METHOD_HANDLER_IN_DEFAULT_FUNCTION)
                continue
            writeValueParameterInLocalVariableTable(param, startLabel, endLabel, false)
        }
    }

    private fun writeValueParameterInLocalVariableTable(param: IrValueParameter, startLabel: Label, endLabel: Label, isReceiver: Boolean) {
        // TODO: old code has a special treatment for destructuring lambda parameters.
        // There is no (easy) way to reproduce it with IR structures.
        // Does not show up in tests, but might come to bite us at some point.

        // If the parameter is an extension receiver parameter or a captured extension receiver from enclosing,
        // then generate name accordingly.
        val name = if (param.origin == BOUND_RECEIVER_PARAMETER || isReceiver) {
            getNameForReceiverParameter(
                irFunction.toIrBasedDescriptor(),
                state.bindingContext,
                context.configuration.languageVersionSettings
            )
        } else {
            param.name.asString()
        }

        val type = typeMapper.mapType(param)
        // NOTE: we expect all value parameters to be present in the frame.
        mv.visitLocalVariable(
            name, type.descriptor, null, startLabel, endLabel, findLocalIndex(param.symbol)
        )
    }

    override fun visitBlock(expression: IrBlock, data: BlockInfo): PromisedValue {
        assert(expression !is IrReturnableBlock) { "unlowered returnable block: ${expression.dump()}" }
        val isSynthesizedInitBlock = expression.origin == SYNTHESIZED_INIT_BLOCK
        if (isSynthesizedInitBlock) {
            expression.markLineNumber(startOffset = true)
            mv.nop()
        }
        if (expression.isTransparentScope)
            return super.visitBlock(expression, data)
        val info = BlockInfo(data)
        // Force materialization to avoid reading from out-of-scope variables.
        val value = super.visitBlock(expression, info).materialized().also {
            if (info.variables.isNotEmpty()) {
                writeLocalVariablesInTable(info, markNewLabel())
            }
        }
        if (isSynthesizedInitBlock) {
            expression.markLineNumber(startOffset = false)
            mv.nop()
        }
        return value
    }

    private val IrVariable.isVisibleInLVT: Boolean
        get() = origin != IrDeclarationOrigin.IR_TEMPORARY_VARIABLE &&
                origin != IrDeclarationOrigin.FOR_LOOP_ITERATOR

    private fun writeLocalVariablesInTable(info: BlockInfo, endLabel: Label) {
        info.variables.forEach {
            if (it.declaration.isVisibleInLVT) {
                mv.visitLocalVariable(it.declaration.name.asString(), it.type.descriptor, null, it.startLabel, endLabel, it.index)
            }
        }

        info.variables.reversed().forEach {
            frameMap.leave(it.declaration.symbol)
        }
    }

    private fun visitStatementContainer(container: IrStatementContainer, data: BlockInfo) =
        container.statements.fold(unitValue) { prev, exp ->
            prev.discard()
            exp.accept(this, data)
        }

    override fun visitBlockBody(body: IrBlockBody, data: BlockInfo): PromisedValue {
        visitStatementContainer(body, data).discard()
        return unitValue
    }

    override fun visitContainerExpression(expression: IrContainerExpression, data: BlockInfo) =
        visitStatementContainer(expression, data)

    override fun visitCall(expression: IrCall, data: BlockInfo): PromisedValue {
        classCodegen.context.irIntrinsics.getIntrinsic(expression.symbol)
            ?.invoke(expression, this, data)?.let { return it }

        val callee = expression.symbol.owner
        require(callee.parent is IrClass) { "Unhandled intrinsic in ExpressionCodegen: ${callee.render()}" }
        val callable = methodSignatureMapper.mapToCallableMethod(irFunction, expression)
        val callGenerator = getOrCreateCallGenerator(expression, data, callable.signature)
        val isSuspensionPoint = expression.isSuspensionPoint()

        if (isSuspensionPoint != SuspensionPointKind.NEVER) {
            addInlineMarker(mv, isStartNotEnd = true)
        }

        expression.dispatchReceiver?.let { receiver ->
            val type = if (expression.superQualifierSymbol != null) receiver.asmType else callable.owner
            callGenerator.genValueAndPut(callee.dispatchReceiverParameter!!, receiver, type, this, data)
        }

        expression.extensionReceiver?.let { receiver ->
            val type = callable.signature.valueParameters.singleOrNull { it.kind == JvmMethodParameterKind.RECEIVER }?.asmType
                ?: error("No single extension receiver parameter: ${callable.signature.valueParameters}")
            callGenerator.genValueAndPut(callee.extensionReceiverParameter!!, receiver, type, this, data)
        }

        callGenerator.beforeValueParametersStart()
        expression.symbol.owner.valueParameters.forEachIndexed { i, irParameter ->
            val arg = expression.getValueArgument(i)
            val parameterType = callable.valueParameterTypes[i]
            require(arg != null) { "Null argument in ExpressionCodegen for parameter ${irParameter.render()}" }
            callGenerator.genValueAndPut(irParameter, arg, parameterType, this, data)
        }

        expression.markLineNumber(true)

        if (isSuspensionPoint != SuspensionPointKind.NEVER) {
            addSuspendMarker(mv, isStartNotEnd = true, isSuspensionPoint == SuspensionPointKind.NOT_INLINE)
        }

        val generatorForActualCall =
            // Do not inline callee to continuation, instead, call it
            if (irFunction.isInvokeSuspendOfContinuation()) IrCallGenerator.DefaultCallGenerator else callGenerator
        generatorForActualCall.genCall(callable, this, expression, isInsideCondition)

        val unboxedInlineClassIrType =
            callee.suspendFunctionOriginal().originalReturnTypeOfSuspendFunctionReturningUnboxedInlineClass()

        if (isSuspensionPoint != SuspensionPointKind.NEVER) {
            addSuspendMarker(mv, isStartNotEnd = false, isSuspensionPoint == SuspensionPointKind.NOT_INLINE)
            if (unboxedInlineClassIrType != null) {
                generateResumePathUnboxing(mv, unboxedInlineClassIrType.toIrBasedKotlinType())
            }
            addInlineMarker(mv, isStartNotEnd = false)
        }

        if (unboxedInlineClassIrType != null) {
            val isFunctionReference = irFunction.origin != IrDeclarationOrigin.BRIDGE &&
                    irFunction.parentAsClass.origin == JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL

            val isDelegateCall = irFunction.origin == IrDeclarationOrigin.DELEGATED_MEMBER

            if (irFunction.isInvokeSuspendOfContinuation() || isFunctionReference || isDelegateCall) {
                mv.generateCoroutineSuspendedCheck(state.languageVersionSettings)
                mv.checkcast(unboxedInlineClassIrType.asmType)
            }
            if (irFunction.isInvokeSuspendOfContinuation()) {
                StackValue.boxInlineClass(unboxedInlineClassIrType.toIrBasedKotlinType(), mv)
            }
        }

        return when {
            (expression.type.isNothing() || expression.type.isUnit()) && irFunction.shouldContainSuspendMarkers() -> {
                // NewInference allows casting `() -> T` to `() -> Unit`. A CHECKCAST here will fail.
                // Also, if the callee is a suspend function with a suspending tail call, the next `resumeWith`
                // will continue from here, but the value passed to it might not have been `Unit`. An exception
                // is methods that do not pass through the state machine generating MethodVisitor, since getting
                // COROUTINE_SUSPENDED here is still possible; luckily, all those methods are bridges.
                if (callable.asmMethod.returnType != Type.VOID_TYPE)
                    MaterialValue(this, callable.asmMethod.returnType, callee.returnType).discard()
                // don't generate redundant UNIT/pop instructions
                unitValue
            }
            callee.parentAsClass.isAnnotationClass && callable.asmMethod.returnType == AsmTypes.JAVA_CLASS_TYPE -> {
                wrapJavaClassIntoKClass(mv)
                MaterialValue(this, AsmTypes.K_CLASS_TYPE, expression.type)
            }
            callee.parentAsClass.isAnnotationClass && callable.asmMethod.returnType == AsmTypes.JAVA_CLASS_ARRAY_TYPE -> {
                wrapJavaClassesIntoKClasses(mv)
                MaterialValue(this, AsmTypes.K_CLASS_ARRAY_TYPE, expression.type)
            }
            unboxedInlineClassIrType != null && !irFunction.isInvokeSuspendOfContinuation() ->
                object : PromisedValue(this, unboxedInlineClassIrType.asmType, unboxedInlineClassIrType) {
                    override fun materializeAt(target: Type, irTarget: IrType) {
                        mv.checkcast(unboxedInlineClassIrType.asmType)
                        MaterialValue(this@ExpressionCodegen, unboxedInlineClassIrType.asmType, unboxedInlineClassIrType)
                            .materializeAt(target, irTarget)
                    }

                    override fun discard() {
                        pop(mv, OBJECT_TYPE)
                    }
                }
            else ->
                MaterialValue(this, callable.asmMethod.returnType, callee.returnType)
        }
    }

    private fun IrFunctionAccessExpression.isSuspensionPoint(): SuspensionPointKind = when {
        !symbol.owner.isSuspend || !irFunction.shouldContainSuspendMarkers() -> SuspensionPointKind.NEVER
        // Copy-pasted bytecode blocks are not suspension points.
        symbol.owner.isInline ->
            if (symbol.owner.name.asString() == "suspendCoroutineUninterceptedOrReturn" &&
                symbol.owner.getPackageFragment()?.fqName == FqName("kotlin.coroutines.intrinsics")
            ) SuspensionPointKind.ALWAYS else SuspensionPointKind.NEVER
        // This includes inline lambdas, but only in functions intended for the inliner; in others, they stay as `f.invoke()`.
        dispatchReceiver.isReadOfInlineLambda() -> SuspensionPointKind.NOT_INLINE
        else -> SuspensionPointKind.ALWAYS
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: BlockInfo): PromisedValue {
        val callee = expression.symbol.owner
        val owner = typeMapper.mapClass(callee.constructedClass)
        val signature = methodSignatureMapper.mapSignatureSkipGeneric(callee)

        markLineNumber(expression)

        // In this case the receiver is `this` (not specified in IR) and the return value is discarded anyway.
        mv.load(0, OBJECT_TYPE)

        for (argumentIndex in 0 until expression.typeArgumentsCount) {
            val classifier = expression.getTypeArgument(argumentIndex)?.classifierOrNull
            if (classifier is IrTypeParameterSymbol && classifier.owner.isReified) {
                consumeReifiedOperationMarker(classifier)
            }
        }

        generateConstructorArguments(expression, signature, data)
        markLineNumber(expression)

        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, owner.internalName, signature.asmMethod.name, signature.asmMethod.descriptor, false)

        return unitValue
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: BlockInfo): PromisedValue {
        classCodegen.context.irIntrinsics.getIntrinsic(expression.symbol)
            ?.invoke(expression, this, data)?.let { return it }

        val callee = expression.symbol.owner
        val owner = typeMapper.mapClass(callee.constructedClass)
        val signature = methodSignatureMapper.mapSignatureSkipGeneric(callee)

        closureReifiedMarkers[expression.symbol.owner.parentAsClass]?.let {
            if (it.wereUsedReifiedParameters()) {
                putNeedClassReificationMarker(v)
                propagateChildReifiedTypeParametersUsages(it)
            }
        }

        // IR constructors have no receiver and return the new instance, but on JVM they are void-returning
        // instance methods named <init>.
        markLineNumber(expression)
        mv.anew(owner)
        mv.dup()

        generateConstructorArguments(expression, signature, data)
        markLineNumber(expression)

        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, owner.internalName, signature.asmMethod.name, signature.asmMethod.descriptor, false)

        return MaterialValue(this, owner, expression.type)
    }

    private fun generateConstructorArguments(expression: IrFunctionAccessExpression, signature: JvmMethodSignature, data: BlockInfo) {
        expression.symbol.owner.valueParameters.forEachIndexed { i, irParameter ->
            val arg = expression.getValueArgument(i)
                ?: error("Null argument in ExpressionCodegen for parameter ${irParameter.render()}")
            gen(arg, signature.valueParameters[i].asmType, irParameter.type, data)
        }
    }

    override fun visitVariable(declaration: IrVariable, data: BlockInfo): PromisedValue {
        val varType = typeMapper.mapType(declaration)
        val index = frameMap.enter(declaration.symbol, varType)

        val initializer = declaration.initializer
        if (initializer != null) {
            var value = initializer.accept(this, data)
            initializer.markLineNumber(startOffset = true)
            value.materializeAt(varType, declaration.type)
            declaration.markLineNumber(startOffset = true)
            mv.store(index, varType)
        } else if (declaration.isVisibleInLVT) {
            pushDefaultValueOnStack(varType, mv)
            mv.store(index, varType)
        }

        data.variables.add(VariableInfo(declaration, index, varType, markNewLabel()))
        return unitValue
    }

    override fun visitGetValue(expression: IrGetValue, data: BlockInfo): PromisedValue {
        expression.markLineNumber(startOffset = true)
        val type = frameMap.typeOf(expression.symbol)
        mv.load(findLocalIndex(expression.symbol), type)
        return MaterialValue(this, type, expression.type)
    }

    override fun visitFieldAccess(expression: IrFieldAccessExpression, data: BlockInfo): PromisedValue {
        val callee = expression.symbol.owner
        if (context.state.shouldInlineConstVals) {
            // Const fields should only have reads, and those should have been transformed by ConstLowering.
            assert(callee.constantValue() == null) { "access of const val: ${expression.dump()}" }
        }

        val isStatic = expression.receiver == null
        expression.markLineNumber(startOffset = true)

        val receiverType = expression.receiver?.let { receiver ->
            receiver.accept(this, data).materializedAt(typeMapper.mapTypeAsDeclaration(receiver.type), receiver.type).type
        }

        val ownerType = expression.superQualifierSymbol?.let { typeMapper.mapClass(it.owner) }
            ?: receiverType ?: typeMapper.mapClass(callee.parentAsClass)
        val ownerName = ownerType.internalName
        val fieldName = callee.name.asString()
        val fieldType = callee.type.asmType
        return if (expression is IrSetField) {
            val value = expression.value.accept(this, data)
            // We only initialize enum entries with a subtype of `fieldType` and can avoid the CHECKCAST.
            // This is important for some tools which analyze bytecode for enum classes by looking at the
            // initializer of the $VALUES field.
            if (callee.origin == IrDeclarationOrigin.FIELD_FOR_ENUM_ENTRY) {
                value.materialize()
            } else {
                value.materializeAt(fieldType, callee.type)
            }

            expression.markLineNumber(startOffset = true)
            mv.visitFieldInsn(if (isStatic) Opcodes.PUTSTATIC else Opcodes.PUTFIELD, ownerName, fieldName, fieldType.descriptor)
            assert(expression.type.isUnit())
            unitValue
        } else {
            mv.visitFieldInsn(if (isStatic) Opcodes.GETSTATIC else Opcodes.GETFIELD, ownerName, fieldName, fieldType.descriptor)
            MaterialValue(this, fieldType, callee.type)
        }
    }

    override fun visitSetField(expression: IrSetField, data: BlockInfo): PromisedValue {
        val expressionValue = expression.value
        // Do not add redundant field initializers that initialize to default values.
        val inClassInit = irFunction.origin == JvmLoweredDeclarationOrigin.CLASS_STATIC_INITIALIZER
        val isFieldInitializer = expression.origin == IrStatementOrigin.INITIALIZE_FIELD
        val skip = (irFunction is IrConstructor || inClassInit) && isFieldInitializer && expressionValue is IrConst<*> &&
                isDefaultValueForType(expression.symbol.owner.type.asmType, expressionValue.value)
        return if (skip) unitValue else super.visitSetField(expression, data)
    }

    /**
     * Returns true if the given constant value is the JVM's default value for the given type.
     * See: https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-2.html#jvms-2.3
     */
    private fun isDefaultValueForType(type: Type, value: Any?): Boolean =
        when (type) {
            Type.BOOLEAN_TYPE -> value is Boolean && !value
            Type.CHAR_TYPE -> value is Char && value.toInt() == 0
            Type.BYTE_TYPE, Type.SHORT_TYPE, Type.INT_TYPE, Type.LONG_TYPE -> value is Number && value.toLong() == 0L
            // Must use `equals` for these two to differentiate between +0.0 and -0.0:
            Type.FLOAT_TYPE -> value is Number && value.toFloat().equals(0.0f)
            Type.DOUBLE_TYPE -> value is Number && value.toDouble().equals(0.0)
            else -> !isPrimitive(type) && value == null
        }

    private fun findLocalIndex(irSymbol: IrSymbol): Int {
        val index = frameMap.getIndex(irSymbol)
        if (index >= 0)
            return index
        throw AssertionError("Non-mapped local declaration: $irSymbol\n in ${irFunction.dump()}")
    }

    private fun handlePlusMinus(expression: IrSetVariable, value: IrExpression?, isMinus: Boolean): Boolean {
        if (value is IrConst<*> && value.kind == IrConstKind.Int) {
            @Suppress("UNCHECKED_CAST")
            val delta = (value as IrConst<Int>).value
            val upperBound = Byte.MAX_VALUE.toInt() + (if (isMinus) 1 else 0)
            val lowerBound = Byte.MIN_VALUE.toInt() + (if (isMinus) 1 else 0)
            if (delta in lowerBound..upperBound) {
                expression.markLineNumber(startOffset = true)
                mv.iinc(findLocalIndex(expression.symbol), if (isMinus) -delta else delta)
                return true
            }
        }
        return false
    }

    private fun hasSameLineNumber(element0: IrElement, element1: IrElement): Boolean =
        getLineNumberForOffset(element0.startOffset) == getLineNumberForOffset(element1.startOffset)

    // Use iinc for all for the set var int special cases where we can.
    // Be careful to make sure that debugging behavior does not change and
    // only perform the optimization if that can be done without losing
    // line number information.
    private fun handleIntVariableSpecialCases(expression: IrSetVariable): Boolean {
        if (expression.symbol.owner.type.isInt()) {
            when (expression.origin) {
                IrStatementOrigin.PREFIX_INCR, IrStatementOrigin.PREFIX_DECR -> {
                    expression.markLineNumber(startOffset = true)
                    mv.iinc(findLocalIndex(expression.symbol), if (expression.origin == IrStatementOrigin.PREFIX_INCR) 1 else -1)
                    return true
                }
                IrStatementOrigin.PLUSEQ, IrStatementOrigin.MINUSEQ -> {
                    val argument = (expression.value as IrCall).getValueArgument(0)!!
                    if (!hasSameLineNumber(argument, expression)) {
                        return false
                    }
                    return handlePlusMinus(expression, argument, expression.origin is IrStatementOrigin.MINUSEQ)
                }
                IrStatementOrigin.EQ -> {
                    val value = expression.value
                    if (!hasSameLineNumber(value, expression)) {
                        return false
                    }
                    if (value is IrCall) {
                        val receiver = value.dispatchReceiver ?: return false
                        val symbol = expression.symbol
                        if (!hasSameLineNumber(receiver, expression)) {
                            return false
                        }
                        if (value.origin == IrStatementOrigin.PLUS || value.origin == IrStatementOrigin.MINUS) {
                            val argument = value.getValueArgument(0)!!
                            if (receiver is IrGetValue && receiver.symbol == symbol && hasSameLineNumber(argument, expression)) {
                                return handlePlusMinus(expression, argument, value.origin == IrStatementOrigin.MINUS)
                            }
                        }
                    }
                }
            }
        }
        return false
    }

    override fun visitSetVariable(expression: IrSetVariable, data: BlockInfo): PromisedValue {
        if (!handleIntVariableSpecialCases(expression)) {
            expression.value.markLineNumber(startOffset = true)
            expression.value.accept(this, data).materializeAt(expression.symbol.owner.type)
            expression.markLineNumber(startOffset = true)
            mv.store(findLocalIndex(expression.symbol), expression.symbol.owner.asmType)
        }
        return unitValue
    }

    fun setVariable(symbol: IrValueSymbol, value: IrExpression, data: BlockInfo) {
        value.markLineNumber(startOffset = true)
        value.accept(this, data).materializeAt(symbol.owner.type)
        mv.store(findLocalIndex(symbol), symbol.owner.asmType)
    }

    override fun <T> visitConst(expression: IrConst<T>, data: BlockInfo): PromisedValue {
        expression.markLineNumber(startOffset = true)
        when (val value = expression.value) {
            is Boolean -> {
                // BooleanConstants _may not_ be materialized, so we ensure an instruction for the line number.
                mv.nop()
                return BooleanConstant(this, value)
            }
            is Char -> mv.iconst(value.toInt())
            is Long -> mv.lconst(value)
            is Float -> mv.fconst(value)
            is Double -> mv.dconst(value)
            is Number -> mv.iconst(value.toInt())
            else -> if (expression.kind == IrConstKind.Null) return nullConstant else mv.aconst(value)
        }
        return expression.onStack
    }

    override fun visitExpressionBody(body: IrExpressionBody, data: BlockInfo) =
        body.expression.accept(this, data)

    override fun visitElement(element: IrElement, data: BlockInfo) =
        throw AssertionError(
            "Unexpected IR element found during code generation. Either code generation for it " +
                    "is not implemented, or it should have been lowered:\n" +
                    element.render()
        )

    override fun visitClass(declaration: IrClass, data: BlockInfo): PromisedValue {
        if (declaration.origin != JvmLoweredDeclarationOrigin.CONTINUATION_CLASS) {
            val childCodegen = ClassCodegen.getOrCreate(declaration, context, generateSequence(this) { it.inlinedInto }.last().irFunction)
            childCodegen.generate()
            closureReifiedMarkers[declaration] = childCodegen.reifiedTypeParametersUsages
        }
        return unitValue
    }

    override fun visitReturn(expression: IrReturn, data: BlockInfo): PromisedValue {
        val returnTarget = expression.returnTargetSymbol.owner
        val owner =
            returnTarget as? IrFunction
                ?: (returnTarget as? IrReturnableBlock)?.inlineFunctionSymbol?.owner
                ?: error("Unsupported IrReturnTarget: $returnTarget")
        //TODO: should be owner != irFunction
        val isNonLocalReturn =
            methodSignatureMapper.mapFunctionName(owner) != methodSignatureMapper.mapFunctionName(irFunction)
        if (isNonLocalReturn && state.isInlineDisabled) {
            context.psiErrorBuilder.at(expression, owner).report(Errors.NON_LOCAL_RETURN_IN_DISABLED_INLINE)
            genThrow(
                mv, "java/lang/UnsupportedOperationException",
                "Non-local returns are not allowed with inlining disabled"
            )
            return unitValue
        }

        var returnType = if (owner == irFunction) signature.returnType else methodSignatureMapper.mapReturnType(owner)
        var returnIrType = owner.returnType

        val unboxedInlineClass = owner.suspendFunctionOriginal().originalReturnTypeOfSuspendFunctionReturningUnboxedInlineClass()
        if (unboxedInlineClass != null) {
            returnIrType = unboxedInlineClass
            returnType = unboxedInlineClass.asmType
        }
        val afterReturnLabel = Label()
        expression.value.accept(this, data).materializeAt(returnType, returnIrType)
        generateFinallyBlocksIfNeeded(returnType, afterReturnLabel, data)
        expression.markLineNumber(startOffset = true)
        if (isNonLocalReturn) {
            generateGlobalReturnFlag(mv, owner.name.asString())
        }
        mv.areturn(returnType)
        mv.mark(afterReturnLabel)
        mv.nop()/*TODO check RESTORE_STACK_IN_TRY_CATCH processor*/
        return unitValue
    }

    override fun visitWhen(expression: IrWhen, data: BlockInfo): PromisedValue {
        expression.markLineNumber(startOffset = true)
        SwitchGenerator(expression, data, this).generate()?.let { return it }
        // When a lookup/table switch instruction is not generate, output a nop
        // for the line number of the when itself. Otherwise, there will be
        // no option of breaking on the line of the `when` if there is no
        // subject:
        //
        // when {
        //   cond1 -> exp1
        //   else -> exp2
        // }
        if (expression.origin == IrStatementOrigin.WHEN) {
            mv.nop()
        }
        val endLabel = Label()
        val exhaustive = expression.branches.any { it.condition.isTrueConst() } && !expression.type.isUnit()
        assert(exhaustive || expression.type.isUnit()) {
            "non-exhaustive conditional should return Unit: ${expression.dump()}"
        }
        val lastBranch = expression.branches.lastOrNull()
        for (branch in expression.branches) {
            val elseLabel = Label()
            if (branch.condition.isFalseConst() || branch.condition.isTrueConst()) {
                // True or false conditions known at compile time need not be generated. A linenumber and nop
                // are still required for a debugger to break on the line of the condition.
                if (branch !is IrElseBranch) {
                    branch.condition.markLineNumber(startOffset = true)
                    mv.nop()
                }
                if (branch.condition.isFalseConst())
                    continue // The branch body is dead code.
            } else {
                val oldIsInsideCondition = isInsideCondition
                isInsideCondition = true
                branch.condition.accept(this, data).coerceToBoolean().jumpIfFalse(elseLabel)
                isInsideCondition = oldIsInsideCondition
            }
            val result = branch.result.accept(this, data)
            if (!exhaustive) {
                result.discard()
            } else {
                val materializedResult = result.materializedAt(expression.type)
                if (branch.condition.isTrueConst()) {
                    // The rest of the expression is dead code.
                    mv.mark(endLabel)
                    return materializedResult
                }
            }

            if (branch != lastBranch) {
                mv.goTo(endLabel)
            }
            mv.mark(elseLabel)
        }
        mv.mark(endLabel)
        return unitValue
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: BlockInfo): PromisedValue {
        val typeOperand = expression.typeOperand
        val kotlinType = typeOperand.toIrBasedKotlinType()
        return when (expression.operator) {
            IrTypeOperator.IMPLICIT_CAST ->
                expression.argument.accept(this, data)

            IrTypeOperator.CAST, IrTypeOperator.SAFE_CAST -> {
                val result = expression.argument.accept(this, data)
                val boxedLeftType = typeMapper.boxType(result.irType)
                result.materializeAt(boxedLeftType, expression.argument.type)
                val boxedRightType = typeMapper.boxType(typeOperand)

                if (typeOperand.isReifiedTypeParameter) {
                    val operationKind = if (expression.operator == IrTypeOperator.CAST) AS else SAFE_AS
                    putReifiedOperationMarkerIfTypeIsReifiedParameter(typeOperand, operationKind)
                    v.checkcast(boxedRightType)
                } else {
                    assert(expression.operator == IrTypeOperator.CAST) { "IrTypeOperator.SAFE_CAST should have been lowered." }
                    TypeIntrinsics.checkcast(mv, kotlinType, boxedRightType, false)
                }
                MaterialValue(this, boxedRightType, expression.type)
            }

            IrTypeOperator.INSTANCEOF -> {
                expression.argument.accept(this, data).materializeAt(context.irBuiltIns.anyNType)
                val type = typeMapper.boxType(typeOperand)
                if (typeOperand.isReifiedTypeParameter) {
                    putReifiedOperationMarkerIfTypeIsReifiedParameter(typeOperand, ReifiedTypeInliner.OperationKind.IS)
                    v.instanceOf(type)
                } else {
                    TypeIntrinsics.instanceOf(mv, kotlinType, type, state.languageVersionSettings.isReleaseCoroutines())
                }
                expression.onStack
            }

            else -> throw AssertionError("type operator ${expression.operator} should have been lowered")
        }
    }

    override fun visitWhileLoop(loop: IrWhileLoop, data: BlockInfo): PromisedValue {
        val continueLabel = markNewLabel()
        val endLabel = Label()
        // Mark stack depth for break
        mv.fakeAlwaysFalseIfeq(endLabel)
        loop.condition.markLineNumber(true)
        loop.condition.accept(this, data).coerceToBoolean().jumpIfFalse(endLabel)
        data.withBlock(LoopInfo(loop, continueLabel, endLabel)) {
            loop.body?.accept(this, data)?.discard()
        }
        mv.goTo(continueLabel)
        mv.mark(endLabel)
        return unitValue
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: BlockInfo): PromisedValue {
        val entry = markNewLabel()
        val endLabel = Label()
        val continueLabel = Label()
        // Mark stack depth for break/continue
        mv.fakeAlwaysFalseIfeq(continueLabel)
        mv.fakeAlwaysFalseIfeq(endLabel)
        data.withBlock(LoopInfo(loop, continueLabel, endLabel)) {
            loop.body?.accept(this, data)?.discard()
        }
        mv.visitLabel(continueLabel)
        loop.condition.markLineNumber(true)
        loop.condition.accept(this, data).coerceToBoolean().jumpIfTrue(entry)
        mv.mark(endLabel)
        return unitValue
    }

    private fun unwindBlockStack(
        endLabel: Label,
        data: BlockInfo,
        nestedTryWithoutFinally: MutableList<TryInfo> = arrayListOf(),
        stop: (ExpressionInfo) -> Boolean = { false }
    ): ExpressionInfo? {
        return data.handleBlock {
            if (it is TryWithFinallyInfo) {
                genFinallyBlock(it, null, endLabel, data, nestedTryWithoutFinally)
                nestedTryWithoutFinally.clear()
            } else if (it is TryInfo) {
                nestedTryWithoutFinally.add(it)
            }
            return if (stop(it)) it else unwindBlockStack(endLabel, data, nestedTryWithoutFinally, stop)
        }
    }

    override fun visitBreakContinue(jump: IrBreakContinue, data: BlockInfo): PromisedValue {
        jump.markLineNumber(startOffset = true)
        val endLabel = Label()
        val stackElement = unwindBlockStack(endLabel, data) { it is LoopInfo && it.loop == jump.loop } as LoopInfo?
            ?: throw AssertionError("Target label for break/continue not found")
        mv.fixStackAndJump(if (jump is IrBreak) stackElement.breakLabel else stackElement.continueLabel)
        mv.mark(endLabel)
        return unitValue
    }

    override fun visitTry(aTry: IrTry, data: BlockInfo): PromisedValue {
        aTry.markLineNumber(startOffset = true)
        return data.withBlock(if (aTry.finallyExpression != null) TryWithFinallyInfo(aTry.finallyExpression!!) else TryInfo()) {
            visitTryWithInfo(aTry, data, it)
        }

    }

    private fun visitTryWithInfo(aTry: IrTry, data: BlockInfo, tryInfo: TryInfo): PromisedValue {
        val tryBlockStart = markNewLabel()
        mv.nop()
        val tryAsmType = aTry.asmType
        val tryResult = aTry.tryResult.accept(this, data)
        val isExpression = !aTry.type.isUnit()
        var savedValue: Int? = null
        if (isExpression) {
            tryResult.materializeAt(tryAsmType, aTry.type)
            savedValue = frameMap.enterTemp(tryAsmType)
            mv.store(savedValue, tryAsmType)
        } else {
            tryResult.discard()
        }

        val tryBlockEnd = markNewLabel()
        val tryBlockGaps = tryInfo.gaps.toList()
        val tryCatchBlockEnd = Label()
        if (tryInfo is TryWithFinallyInfo) {
            data.handleBlock { genFinallyBlock(tryInfo, tryCatchBlockEnd, null, data) }
        } else {
            mv.goTo(tryCatchBlockEnd)
        }

        val catches = aTry.catches
        for (clause in catches) {
            val clauseStart = markNewLabel()
            val parameter = clause.catchParameter
            val descriptorType = parameter.asmType
            val index = frameMap.enter(clause.catchParameter, descriptorType)
            clause.markLineNumber(true)
            mv.store(index, descriptorType)
            val afterStore = markNewLabel()

            val catchBody = clause.result
            val catchResult = catchBody.accept(this, data)
            if (savedValue != null) {
                catchResult.materializeAt(tryAsmType, aTry.type)
                mv.store(savedValue, tryAsmType)
            } else {
                catchResult.discard()
            }

            frameMap.leave(clause.catchParameter)

            val clauseEnd = markNewLabel()
            mv.visitLocalVariable(parameter.name.asString(), descriptorType.descriptor, null, afterStore, clauseEnd, index)

            if (tryInfo is TryWithFinallyInfo) {
                data.handleBlock { genFinallyBlock(tryInfo, tryCatchBlockEnd, null, data) }
            } else if (clause != catches.last()) {
                mv.goTo(tryCatchBlockEnd)
            }

            genTryCatchCover(clauseStart, tryBlockStart, tryBlockEnd, tryBlockGaps, descriptorType.internalName)
        }

        if (tryInfo is TryWithFinallyInfo) {
            // Generate `try { ... } catch (e: Any?) { <finally>; throw e }` around every part of
            // the try-catch that is not a copy-pasted `finally` block.
            val defaultCatchStart = markNewLabel()
            // While keeping this value on the stack should be enough, the bytecode validator will
            // complain if a catch block does not start with ASTORE.
            val savedException = frameMap.enterTemp(AsmTypes.JAVA_THROWABLE_TYPE)
            mv.store(savedException, AsmTypes.JAVA_THROWABLE_TYPE)

            val finallyStart = markNewLabel()
            val finallyGaps = tryInfo.gaps.toList()
            data.handleBlock { genFinallyBlock(tryInfo, null, null, data) }
            mv.load(savedException, AsmTypes.JAVA_THROWABLE_TYPE)
            frameMap.leaveTemp(AsmTypes.JAVA_THROWABLE_TYPE)
            mv.athrow()

            // Include the ASTORE into the covered region. This is used by the inliner to detect try-finally.
            genTryCatchCover(defaultCatchStart, tryBlockStart, finallyStart, finallyGaps, null)
        }

        mv.mark(tryCatchBlockEnd)
        // TODO: generate a common `finally` for try & catch blocks here? Right now this breaks the inliner.
        return object : PromisedValue(this, tryAsmType, aTry.type) {
            override fun materializeAt(target: Type, irTarget: IrType) {
                if (savedValue != null) {
                    mv.load(savedValue, tryAsmType)
                    frameMap.leaveTemp(tryAsmType)
                    super.materializeAt(target, irTarget)
                } else {
                    unitValue.materializeAt(target, irTarget)
                }
            }

            override fun discard() {
                if (savedValue != null) {
                    frameMap.leaveTemp(tryAsmType)
                }
            }
        }
    }

    private fun genTryCatchCover(catchStart: Label, tryStart: Label, tryEnd: Label, tryGaps: List<Pair<Label, Label>>, type: String?) {
        val lastRegionStart = tryGaps.fold(tryStart) { regionStart, (gapStart, gapEnd) ->
            mv.visitTryCatchBlock(regionStart, gapStart, catchStart, type)
            gapEnd
        }
        mv.visitTryCatchBlock(lastRegionStart, tryEnd, catchStart, type)
    }

    private fun genFinallyBlock(
        tryWithFinallyInfo: TryWithFinallyInfo,
        tryCatchBlockEnd: Label?,
        afterJumpLabel: Label?,
        data: BlockInfo,
        nestedTryWithoutFinally: MutableList<TryInfo> = arrayListOf()
    ) {
        val gapStart = markNewLinkedLabel()
        finallyDepth++
        if (isFinallyMarkerRequired()) {
            generateFinallyMarker(mv, finallyDepth, true)
        }
        tryWithFinallyInfo.onExit.accept(this, data).discard()
        if (isFinallyMarkerRequired()) {
            generateFinallyMarker(mv, finallyDepth, false)
        }
        finallyDepth--
        if (tryCatchBlockEnd != null) {
            tryWithFinallyInfo.onExit.markLineNumber(startOffset = false)
            mv.goTo(tryCatchBlockEnd)
        }
        val gapEnd = afterJumpLabel ?: markNewLabel()
        tryWithFinallyInfo.gaps.add(gapStart to gapEnd)
        if (state.languageVersionSettings.supportsFeature(LanguageFeature.ProperFinally)) {
            for (it in nestedTryWithoutFinally) {
                it.gaps.add(gapStart to gapEnd)
            }
        }
    }

    fun generateFinallyBlocksIfNeeded(returnType: Type, afterReturnLabel: Label, data: BlockInfo) {
        if (data.hasFinallyBlocks()) {
            if (Type.VOID_TYPE != returnType) {
                val returnValIndex = frameMap.enterTemp(returnType)
                mv.store(returnValIndex, returnType)
                unwindBlockStack(afterReturnLabel, data)
                mv.load(returnValIndex, returnType)
                frameMap.leaveTemp(returnType)
            } else {
                unwindBlockStack(afterReturnLabel, data)
            }
        }
    }

    override fun visitThrow(expression: IrThrow, data: BlockInfo): PromisedValue {
        expression.markLineNumber(startOffset = true)
        val exception = expression.value.accept(this, data)
        // Avoid unecessary CHECKCASTs to java/lang/Throwable. If the exception is not of type Object
        // then it must be some subtype of throwable and we don't need to coerce it.
        if (exception.type == OBJECT_TYPE)
            exception.materializeAt(context.irBuiltIns.throwableType)
        else
            exception.materialize()
        mv.athrow()
        return unitValue
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: BlockInfo): PromisedValue {
        assert(context.state.runtimeStringConcat.isDynamic) {
            "IrStringConcatenation expression should be presented only with dynamic concatenation: ${expression.dump()}"
        }
        val generator = StringConcatGenerator(context.state.runtimeStringConcat, mv)
        expression.arguments.forEach { arg ->
            if (arg is IrConst<*>) {
                val type = when (arg.kind) {
                    IrConstKind.Boolean -> Type.BOOLEAN_TYPE
                    IrConstKind.Char -> Type.CHAR_TYPE
                    IrConstKind.Int -> Type.INT_TYPE
                    IrConstKind.Long -> Type.LONG_TYPE
                    IrConstKind.Float -> Type.FLOAT_TYPE
                    IrConstKind.Double -> Type.DOUBLE_TYPE
                    IrConstKind.Byte -> Type.BYTE_TYPE
                    IrConstKind.Short -> Type.SHORT_TYPE
                    IrConstKind.String -> JAVA_STRING_TYPE
                    IrConstKind.Null -> OBJECT_TYPE
                }
                generator.putValueOrProcessConstant(StackValue.constant(arg.value, type, null))
            } else {
                val value = arg.accept(this, data)
                value.materializeAt(value.type, value.irType)
                generator.invokeAppend(value.type)
            }
        }
        generator.genToString()
        return MaterialValue(this@ExpressionCodegen, JAVA_STRING_TYPE, context.irBuiltIns.stringType)
    }

    override fun visitGetClass(expression: IrGetClass, data: BlockInfo) =
        generateClassLiteralReference(expression, true, data)

    override fun visitClassReference(expression: IrClassReference, data: BlockInfo) =
        generateClassLiteralReference(expression, true, data)

    fun generateClassLiteralReference(
        classReference: IrExpression,
        wrapIntoKClass: Boolean,
        data: BlockInfo
    ): PromisedValue {
        if (classReference is IrGetClass) {
            // TODO transform one sort of access into the other?
            JavaClassProperty.invokeWith(classReference.argument.accept(this, data))
        } else if (classReference is IrClassReference) {
            val classType = classReference.classType
            val classifier = classType.classifierOrNull
            if (classifier is IrTypeParameterSymbol) {
                assert(classifier.owner.isReified) {
                    "Non-reified type parameter under ::class should be rejected by type checker: ${classifier.owner.dump()}"
                }
                putReifiedOperationMarkerIfTypeIsReifiedParameter(classType, ReifiedTypeInliner.OperationKind.JAVA_CLASS)
            }

            generateClassInstance(mv, classType, typeMapper)
        } else {
            throw AssertionError("not an IrGetClass or IrClassReference: ${classReference.dump()}")
        }

        if (wrapIntoKClass) {
            wrapJavaClassIntoKClass(mv)
        }
        return classReference.onStack
    }

    private fun getOrCreateCallGenerator(
        element: IrFunctionAccessExpression, data: BlockInfo, signature: JvmMethodSignature
    ): IrCallGenerator {
        if (!element.symbol.owner.isInlineFunctionCall(context) ||
            classCodegen.irClass.fileParent.fileEntry is MultifileFacadeFileEntry ||
            irFunction.isInvokeSuspendOfContinuation()
        ) {
            return IrCallGenerator.DefaultCallGenerator
        }

        if (element.origin == JvmLoweredStatementOrigin.DEFAULT_STUB_CALL_TO_IMPLEMENTATION) {
            return IrInlineDefaultCodegen
        }

        val callee = element.symbol.owner
        val typeArgumentContainer = if (callee is IrConstructor) callee.parentAsClass else callee
        val typeArguments =
            if (element.typeArgumentsCount == 0) {
                //avoid ambiguity with type constructor type parameters
                emptyMap()
            } else typeArgumentContainer.typeParameters.keysToMap {
                element.getTypeArgumentOrDefault(it)
            }

        val mappings = TypeParameterMappings<IrType>()
        for ((key, type) in typeArguments.entries) {
            val reificationArgument = typeMapper.typeSystem.extractReificationArgument(type)
            if (reificationArgument == null) {
                // type is not generic
                val signatureWriter = BothSignatureWriter(BothSignatureWriter.Mode.TYPE)
                val asmType = typeMapper.mapTypeParameter(type, signatureWriter)

                mappings.addParameterMappingToType(
                    key.name.identifier, type, asmType, signatureWriter.toString(), key.isReified
                )
            } else {
                mappings.addParameterMappingForFurtherReification(
                    key.name.identifier, type, reificationArgument.second, key.isReified
                )
            }
        }

        val methodOwner = typeMapper.mapClass(callee.parentAsClass)
        val sourceCompiler = IrSourceCompilerForInline(state, element, callee, this, data)

        val reifiedTypeInliner = ReifiedTypeInliner(
            mappings,
            IrInlineIntrinsicsSupport(context, typeMapper),
            IrTypeCheckerContext(context.irBuiltIns),
            state.languageVersionSettings,
            state.unifiedNullChecks,
        )

        return IrInlineCodegen(this, state, callee, methodOwner, signature, mappings, sourceCompiler, reifiedTypeInliner)
    }

    override fun consumeReifiedOperationMarker(typeParameter: TypeParameterMarker) {
        require(typeParameter is IrTypeParameterSymbol)
        // This is a hack to work around the problem in LocalDeclarationsLowering. Specifically, suppose an inline
        // lambda uses a reified type parameter declared by a function:
        //
        //     object {
        //         inline fun <reified T : Any> f() = run { T::class.java.getName() }
        //     }
        //
        // LocalDeclarationsLowering would extract that lambda into a method of the enclosing type, but will not create
        // a reified type parameter in it (in fact, the lambda method isn't even marked as inline):
        //
        //     object {
        //         /* static */ private fun `f$lambda-0`() = T::class.java.getName()
        //         inline fun <reified T : Any> f() = run(::`f$lambda-0`)
        //     }
        //
        // The parent of the type parameter then is not `irFunction` (i.e. the lambda itself), but the function
        // it is inlined into.
        //
        // TODO make LocalDeclarationsLowering handle captured type parameters and only compare with `irFunction`.
        if (generateSequence(this) { it.inlinedInto }.none { it.irFunction == typeParameter.owner.parent }) {
            classCodegen.reifiedTypeParametersUsages.addUsedReifiedParameter(typeParameter.owner.name.asString())
        }
    }

    override fun propagateChildReifiedTypeParametersUsages(reifiedTypeParametersUsages: ReifiedTypeParametersUsages) {
        classCodegen.reifiedTypeParametersUsages.propagateChildUsagesWithinContext(reifiedTypeParametersUsages) {
            irFunction.typeParameters.filter { it.isReified }.map { it.name.asString() }.toSet()
        }
    }

    override fun pushClosureOnStack(
        classDescriptor: ClassDescriptor,
        putThis: Boolean,
        callGenerator: CallGenerator,
        functionReferenceReceiver: StackValue?
    ) {
        //TODO
    }

    override fun markLineNumberAfterInlineIfNeeded(registerLineNumberAfterwards: Boolean) {
        if (noLineNumberScope || registerLineNumberAfterwards) {
            if (lastLineNumber > -1) {
                val label = Label()
                v.visitLabel(label)
                v.visitLineNumber(lastLineNumber, label)
            }
        } else {
            // Inline function has its own line number which is in a separate instance of codegen,
            // therefore we need to reset lastLineNumber to force a line number generation after visiting inline function.
            lastLineNumber = -1
        }
    }

    fun isFinallyMarkerRequired(): Boolean {
        return irFunction.isInline || inlinedInto != null
    }

    val IrType.isReifiedTypeParameter: Boolean
        get() = this.classifierOrNull?.safeAs<IrTypeParameterSymbol>()?.owner?.isReified == true

    companion object {
        internal fun generateClassInstance(v: InstructionAdapter, classType: IrType, typeMapper: IrTypeMapper) {
            val asmType = typeMapper.mapType(classType)
            if (classType.getClass()?.isInline == true || !isPrimitive(asmType)) {
                v.aconst(typeMapper.boxType(classType))
            } else {
                v.getstatic(boxType(asmType).internalName, "TYPE", "Ljava/lang/Class;")
            }
        }
    }
}
