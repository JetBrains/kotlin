/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.common.lower.BOUND_RECEIVER_PARAMETER
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.intrinsics.IrIntrinsicMethods
import org.jetbrains.kotlin.backend.jvm.intrinsics.JavaClassProperty
import org.jetbrains.kotlin.backend.jvm.lower.MultifileFacadeFileEntry
import org.jetbrains.kotlin.backend.jvm.lower.constantValue
import org.jetbrains.kotlin.backend.jvm.lower.inlineclasses.unboxInlineClass
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.AsmUtil.*
import org.jetbrains.kotlin.codegen.coroutines.INVOKE_SUSPEND_METHOD_NAME
import org.jetbrains.kotlin.codegen.inline.*
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner.Companion.putNeedClassReificationMarker
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner.OperationKind.AS
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner.OperationKind.SAFE_AS
import org.jetbrains.kotlin.codegen.intrinsics.TypeIntrinsics
import org.jetbrains.kotlin.codegen.pseudoInsns.fakeAlwaysFalseIfeq
import org.jetbrains.kotlin.codegen.pseudoInsns.fixStackAndJump
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.isReleaseCoroutines
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.kotlin.utils.keysToMap
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import java.util.*

sealed class ExpressionInfo

class LoopInfo(val loop: IrLoop, val continueLabel: Label, val breakLabel: Label) : ExpressionInfo()

class TryInfo(val onExit: IrExpression) : ExpressionInfo() {
    // Regions corresponding to copy-pasted contents of the `finally` block.
    // These should not be covered by `catch` clauses.
    val gaps = mutableListOf<Pair<Label, Label>>()
}

class ReturnableBlockInfo(
    val returnLabel: Label,
    val returnSymbol: IrSymbol,
    val returnTemporary: Int? = null
) : ExpressionInfo()

class BlockInfo(val parent: BlockInfo? = null) {
    val variables = mutableListOf<VariableInfo>()
    private val infos: Stack<ExpressionInfo> = parent?.infos ?: Stack()

    fun hasFinallyBlocks(): Boolean = infos.firstIsInstanceOrNull<TryInfo>() != null

    internal inline fun <reified T : ExpressionInfo> findBlock(predicate: (T) -> Boolean): T? =
        infos.find { it is T && predicate(it) } as? T

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
    override val frameMap: IrFrameMap,
    val mv: InstructionAdapter,
    val classCodegen: ClassCodegen,
    val isInlineLambda: Boolean = false
) : IrElementVisitor<PromisedValue, BlockInfo>, BaseExpressionCodegen {

    var finallyDepth = 0

    val context = classCodegen.context
    val typeMapper = context.typeMapper
    val methodSignatureMapper = context.methodSignatureMapper

    private val state = classCodegen.state

    private val fileEntry = classCodegen.context.psiSourceManager.getFileEntry(irFunction.fileParent)

    override val visitor: InstructionAdapter
        get() = mv

    override val inlineNameGenerator: NameGenerator = NameGenerator("${classCodegen.type.internalName}\$todo") // TODO

    override var lastLineNumber: Int = -1

    private val closureReifiedMarkers = hashMapOf<IrClass, ReifiedTypeParametersUsages>()

    private val IrType.asmType: Type
        get() = typeMapper.mapType(this)

    val IrExpression.asmType: Type
        get() = type.asmType

    val IrVariable.asmType: Type
        get() = type.asmType

    // Assume this expression's result has already been materialized on the stack
    // with the correct type.
    val IrExpression.onStack: MaterialValue
        get() = MaterialValue(this@ExpressionCodegen, asmType, type)

    private fun markNewLabel() = Label().apply { mv.visitLabel(this) }

    private fun IrElement.markLineNumber(startOffset: Boolean) {
        val offset = if (startOffset) this.startOffset else endOffset
        if (offset < 0) {
            return
        }
        if (fileEntry != null) {
            val lineNumber = fileEntry.getLineNumber(offset) + 1
            assert(lineNumber > 0)
            if (lastLineNumber != lineNumber) {
                lastLineNumber = lineNumber
                mv.visitLineNumber(lineNumber, markNewLabel())
            }
        }
    }

    // TODO remove
    fun gen(expression: IrExpression, type: Type, irType: IrType, data: BlockInfo): StackValue {
        expression.accept(this, data).coerce(type, irType).materialize()
        return StackValue.onStack(type, irType.toKotlinType())
    }

    fun generate() {
        mv.visitCode()
        val startLabel = markNewLabel()
        val info = BlockInfo()
        val body = irFunction.body!!
        generateNonNullAssertions()
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
            val returnType = methodSignatureMapper.mapReturnType(irFunction)
            val returnIrType = if (irFunction !is IrConstructor) irFunction.returnType else context.irBuiltIns.unitType
            result.coerce(returnType, returnIrType).materialize()
            mv.areturn(returnType)
        }
        val endLabel = markNewLabel()
        writeLocalVariablesInTable(info, endLabel)
        writeParameterInLocalVariableTable(startLabel, endLabel)
    }

    private fun generateNonNullAssertions() {
        if (state.isParamAssertionsDisabled)
            return

        val isSyntheticOrBridge = irFunction.origin.isSynthetic ||
                // Although these are accessible from Java, the functions they bridge to already have the assertions.
                irFunction.origin == IrDeclarationOrigin.BRIDGE_SPECIAL ||
                irFunction.origin == JvmLoweredDeclarationOrigin.DEFAULT_IMPLS_BRIDGE ||
                irFunction.origin == JvmLoweredDeclarationOrigin.JVM_STATIC_WRAPPER ||
                irFunction.origin == JvmLoweredDeclarationOrigin.MULTIFILE_BRIDGE
        if (!isInlineLambda && !isSyntheticOrBridge && !Visibilities.isPrivate(irFunction.visibility)) {
            irFunction.extensionReceiverParameter?.let { generateNonNullAssertion(it) }
            irFunction.valueParameters.forEach(::generateNonNullAssertion)
        }
    }

    private fun generateNonNullAssertion(param: IrValueParameter) {
        val asmType = param.type.asmType
        if (!param.type.unboxInlineClass().isNullable() && !isPrimitive(asmType)) {
            mv.load(findLocalIndex(param.symbol), asmType)
            mv.aconst(param.name.asString())
            val methodName =
                if (state.languageVersionSettings.apiVersion >= ApiVersion.KOTLIN_1_4) "checkNotNullParameter"
                else "checkParameterIsNotNull"
            mv.invokestatic(IrIntrinsicMethods.INTRINSICS_CLASS_NAME, methodName, "(Ljava/lang/Object;Ljava/lang/String;)V", false)
        }
    }

    private fun writeParameterInLocalVariableTable(startLabel: Label, endLabel: Label) {
        if (!irFunction.isStatic) {
            mv.visitLocalVariable("this", classCodegen.type.descriptor, null, startLabel, endLabel, 0)
        }
        val extensionReceiverParameter = irFunction.extensionReceiverParameter
        if (extensionReceiverParameter != null) {
            writeValueParameterInLocalVariableTable(extensionReceiverParameter, startLabel, endLabel, true)
        }
        for (param in irFunction.valueParameters) {
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
                irFunction.descriptor,
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
        if (expression.isTransparentScope)
            return super.visitBlock(expression, data)
        val info = BlockInfo(data)
        return if (expression is IrReturnableBlock) {
            val returnType = expression.asmType
            val returnLabel = Label()
            // Because the return might be inside an expression, it will need to pop excess items
            // before jumping and store the result in a temporary variable.
            val returnTemporary = if (returnType != Type.VOID_TYPE) frameMap.enterTemp(returnType) else null
            info.withBlock(ReturnableBlockInfo(returnLabel, expression.symbol, returnTemporary)) {
                // Remember current stack depth.
                mv.fakeAlwaysFalseIfeq(returnLabel)
                super.visitBlock(expression, info).materialized.also {
                    returnTemporary?.let { mv.store(it, returnType) }
                    // Variables leave the scope in reverse order, so must write locals first.
                    mv.mark(returnLabel)
                    writeLocalVariablesInTable(info, returnLabel)
                    returnTemporary?.let {
                        mv.load(it, returnType)
                        frameMap.leaveTemp(returnType)
                    }
                }
            }
        } else {
            // Force materialization to avoid reading from out-of-scope variables.
            super.visitBlock(expression, info).materialized.also {
                writeLocalVariablesInTable(info, markNewLabel())
            }
        }
    }

    private fun writeLocalVariablesInTable(info: BlockInfo, endLabel: Label) {
        info.variables.forEach {
            when (it.declaration.origin) {
                IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
                IrDeclarationOrigin.FOR_LOOP_ITERATOR,
                IrDeclarationOrigin.FOR_LOOP_IMPLICIT_VARIABLE -> {
                    // Ignore implicitly created variables
                }
                else -> {
                    mv.visitLocalVariable(
                        it.declaration.name.asString(), it.type.descriptor, null, it.startLabel, endLabel, it.index
                    )
                }
            }
        }

        info.variables.reversed().forEach {
            frameMap.leave(it.declaration.symbol)
        }
    }

    private fun visitStatementContainer(container: IrStatementContainer, data: BlockInfo) =
        container.statements.fold(immaterialUnitValue as PromisedValue) { prev, exp ->
            prev.discard()
            exp.accept(this, data).also { (exp as? IrExpression)?.markEndOfStatementIfNeeded() }
        }

    override fun visitBlockBody(body: IrBlockBody, data: BlockInfo) =
        visitStatementContainer(body, data).discard()

    override fun visitContainerExpression(expression: IrContainerExpression, data: BlockInfo) =
        visitStatementContainer(expression, data).coerce(expression.type)

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: BlockInfo): PromisedValue {
        classCodegen.context.irIntrinsics.getIntrinsic(expression.symbol)
            ?.invoke(expression, this, data)?.let { return it.coerce(expression.type) }

        val callable = methodSignatureMapper.mapToCallableMethod(expression)
        val callee = expression.symbol.owner
        val callGenerator = getOrCreateCallGenerator(expression, data)
        val asmType = if (expression is IrConstructorCall) typeMapper.mapTypeAsDeclaration(expression.type) else expression.asmType

        when {
            expression is IrConstructorCall -> {
                closureReifiedMarkers[expression.symbol.owner.parentAsClass]?.let {
                    if (it.wereUsedReifiedParameters()) {
                        putNeedClassReificationMarker(v)
                        propagateChildReifiedTypeParametersUsages(it)
                    }
                }

                // IR constructors have no receiver and return the new instance, but on JVM they are void-returning
                // instance methods named <init>.
                mv.anew(asmType)
                mv.dup()
            }
            expression is IrDelegatingConstructorCall -> {
                // In this case the receiver is `this` (not specified in IR) and the return value is discarded anyway.
                mv.load(0, OBJECT_TYPE)

                for (argumentIndex in 0 until expression.typeArgumentsCount) {
                    expression.getTypeArgument(argumentIndex)?.getTypeParameterOrNull()?.takeIf { it.isReified }?.let {
                        consumeReifiedOperationMarker(it)
                    }
                }
            }
            expression.descriptor is ConstructorDescriptor ->
                throw AssertionError("IrCall with ConstructorDescriptor: ${expression.javaClass.simpleName}")
            callee.isSuspend && !irFunction.isInvokeSuspendInContinuation() ->
                addInlineMarker(mv, isStartNotEnd = true)
        }

        expression.dispatchReceiver?.let { receiver ->
            val type = if ((expression as? IrCall)?.superQualifier != null) receiver.asmType else callable.owner
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

        // Do not generate redundant markers in continuation class.
        if (callee.isSuspend && !irFunction.isInvokeSuspendInContinuation()) {
            addSuspendMarker(mv, isStartNotEnd = true)
        }

        callGenerator.genCall(callable, this, expression)

        if (callee.isSuspend && !irFunction.isInvokeSuspendInContinuation()) {
            addSuspendMarker(mv, isStartNotEnd = false)
            addInlineMarker(mv, isStartNotEnd = false)
        }

        val returnType = callee.returnType
        return when {
            returnType.substitute(expression.typeSubstitutionMap).isNothing() -> {
                mv.aconst(null)
                mv.athrow()
                immaterialUnitValue
            }
            expression is IrConstructorCall ->
                MaterialValue(this, asmType, expression.type)
            expression is IrDelegatingConstructorCall ->
                immaterialUnitValue
            expression.type.isUnit() ->
                // NewInference allows casting `() -> T` to `() -> Unit`. A CHECKCAST here will fail.
                MaterialValue(this, callable.asmMethod.returnType, returnType).discard().coerce(expression.type)
            else ->
                MaterialValue(this, callable.asmMethod.returnType, returnType).coerce(expression.type)
        }
    }

    private fun IrFunction.isInvokeSuspendInContinuation(): Boolean =
        name.asString() == INVOKE_SUSPEND_METHOD_NAME && parentAsClass in classCodegen.context.suspendFunctionContinuations.values

    override fun visitVariable(declaration: IrVariable, data: BlockInfo): PromisedValue {
        val varType = typeMapper.mapType(declaration)
        val index = frameMap.enter(declaration.symbol, varType)

        declaration.markLineNumber(startOffset = true)

        declaration.initializer?.let {
            it.accept(this, data).coerce(varType, declaration.type).materialize()
            mv.store(index, varType)
            it.markLineNumber(startOffset = true)
        }

        data.variables.add(VariableInfo(declaration, index, varType, markNewLabel()))
        return immaterialUnitValue
    }

    override fun visitGetValue(expression: IrGetValue, data: BlockInfo): PromisedValue {
        // Do not generate line number information for loads from compiler-generated
        // temporary variables. They do not correspond to variable loads in user code.
        if (expression.symbol.owner.origin != IrDeclarationOrigin.IR_TEMPORARY_VARIABLE)
            expression.markLineNumber(startOffset = true)
        val type = frameMap.typeOf(expression.symbol)
        mv.load(findLocalIndex(expression.symbol), type)
        return MaterialValue(this, type, expression.type)
    }

    override fun visitFieldAccess(expression: IrFieldAccessExpression, data: BlockInfo): PromisedValue {
        val callee = expression.symbol.owner
        callee.constantValue()?.let {
            // Handling const reads before codegen is important for constant folding.
            assert(expression is IrSetField) { "read of const val ${callee.name} not inlined by ConstLowering" }
            // This can only be the field's initializer; JVM implementations are required
            // to generate those for ConstantValue-marked fields automatically, so this is redundant.
            return defaultValue(expression.type)
        }

        val realField = callee.resolveFakeOverride()!!
        val fieldType = typeMapper.mapType(realField.type)
        val ownerType = typeMapper.mapClass(callee.parentAsClass).internalName
        val fieldName = realField.name.asString()
        val isStatic = expression.receiver == null
        expression.markLineNumber(startOffset = true)
        expression.receiver?.accept(this, data)?.materialize()
        return if (expression is IrSetField) {
            expression.value.accept(this, data).coerce(fieldType, callee.type).materialize()
            when {
                isStatic -> mv.putstatic(ownerType, fieldName, fieldType.descriptor)
                else -> mv.putfield(ownerType, fieldName, fieldType.descriptor)
            }
            defaultValue(expression.type)
        } else {
            when {
                isStatic -> mv.getstatic(ownerType, fieldName, fieldType.descriptor)
                else -> mv.getfield(ownerType, fieldName, fieldType.descriptor)
            }
            MaterialValue(this, fieldType, callee.type).coerce(expression.type)
        }
    }

    override fun visitSetField(expression: IrSetField, data: BlockInfo): PromisedValue {
        val expressionValue = expression.value
        // Do not add redundant field initializers that initialize to default values.
        val inPrimaryConstructor = irFunction is IrConstructor && irFunction.isPrimary
        val inClassInit = irFunction.origin == JvmLoweredDeclarationOrigin.CLASS_STATIC_INITIALIZER
        // "expression.origin == null" means that the field is initialized when it is declared,
        // i.e., not in an initializer block or constructor body.
        val isFieldInitializer = expression.origin == null
        val skip = (inPrimaryConstructor || inClassInit) && isFieldInitializer && expressionValue is IrConst<*> &&
                isDefaultValueForType(expression.symbol.owner.type.asmType, expressionValue.value)
        return if (skip) defaultValue(expression.type) else super.visitSetField(expression, data)
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
        val dump = if (irSymbol.isBound) irSymbol.owner.dump() else irSymbol.descriptor.toString()
        throw AssertionError("Non-mapped local declaration: $dump\n in ${irFunction.dump()}")
    }

    override fun visitSetVariable(expression: IrSetVariable, data: BlockInfo): PromisedValue {
        expression.markLineNumber(startOffset = true)
        expression.value.markLineNumber(startOffset = true)
        expression.value.accept(this, data).coerce(expression.symbol.owner.type).materialize()
        mv.store(findLocalIndex(expression.symbol), expression.symbol.owner.asmType)
        return defaultValue(expression.type)
    }

    override fun <T> visitConst(expression: IrConst<T>, data: BlockInfo): PromisedValue {
        expression.markLineNumber(startOffset = true)
        when (val value = expression.value) {
            is Boolean -> return object : BooleanValue(this) {
                override fun jumpIfFalse(target: Label) = if (value) Unit else mv.goTo(target)
                override fun jumpIfTrue(target: Label) = if (value) mv.goTo(target) else Unit
                override fun materialize() = mv.iconst(if (value) 1 else 0)
            }
            is Char -> mv.iconst(value.toInt())
            is Long -> mv.lconst(value)
            is Float -> mv.fconst(value)
            is Double -> mv.dconst(value)
            is Number -> mv.iconst(value.toInt())
            else -> mv.aconst(value)
        }
        return expression.onStack
    }

    override fun visitExpressionBody(body: IrExpressionBody, data: BlockInfo) =
        body.expression.accept(this, data)

    override fun visitElement(element: IrElement, data: BlockInfo) =
        throw AssertionError(
            "Unexpected IR element found during code generation. Either code generation for it " +
                    "is not implemented, or it should have been lowered: ${element.render()}"
        )

    override fun visitClass(declaration: IrClass, data: BlockInfo): PromisedValue {
        classCodegen.generateLocalClass(declaration, irFunction.isInline).also {
            closureReifiedMarkers[declaration] = it
        }
        return immaterialUnitValue
    }

    override fun visitReturn(expression: IrReturn, data: BlockInfo): PromisedValue {
        val returnTarget = expression.returnTargetSymbol.owner
        val owner =
            returnTarget as? IrFunction
                ?: (returnTarget as? IrReturnableBlock)?.inlineFunctionSymbol?.owner
                ?: error("Unsupported IrReturnTarget: $returnTarget")
        //TODO: should be owner != irFunction
        val isNonLocalReturn =
            methodSignatureMapper.mapFunctionName(owner, OwnerKind.IMPLEMENTATION) !=
                    methodSignatureMapper.mapFunctionName(irFunction, OwnerKind.IMPLEMENTATION)
        if (isNonLocalReturn && state.isInlineDisabled) {
            //TODO: state.diagnostics.report(Errors.NON_LOCAL_RETURN_IN_DISABLED_INLINE.on(expression))
            genThrow(
                mv, "java/lang/UnsupportedOperationException",
                "Non-local returns are not allowed with inlining disabled"
            )
            return immaterialUnitValue
        }

        val target = data.findBlock<ReturnableBlockInfo> { it.returnSymbol == expression.returnTargetSymbol }
        val returnType = methodSignatureMapper.mapReturnType(owner)
        val afterReturnLabel = Label()
        expression.value.accept(this, data).coerce(returnType, owner.returnType).materialize()
        generateFinallyBlocksIfNeeded(returnType, afterReturnLabel, data, target)
        expression.markLineNumber(startOffset = true)
        if (target != null) {
            target.returnTemporary?.let { mv.store(it, returnType) }
            mv.fixStackAndJump(target.returnLabel)
        } else {
            if (isNonLocalReturn) {
                generateGlobalReturnFlag(mv, owner.name.asString())
            }
            mv.areturn(returnType)
        }
        mv.mark(afterReturnLabel)
        mv.nop()/*TODO check RESTORE_STACK_IN_TRY_CATCH processor*/
        return immaterialUnitValue
    }

    override fun visitWhen(expression: IrWhen, data: BlockInfo): PromisedValue {
        expression.markLineNumber(startOffset = true)
        SwitchGenerator(expression, data, this).generate()?.let { return it }

        val endLabel = Label()
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
                branch.condition.accept(this, data).coerceToBoolean().jumpIfFalse(elseLabel)
            }
            val result = branch.result.accept(this, data).coerce(expression.type).materialized
            if (branch.condition.isTrueConst()) {
                // The rest of the expression is dead code.
                mv.mark(endLabel)
                return result
            }
            mv.goTo(endLabel)
            mv.mark(elseLabel)
        }
        // Produce the default value for the type. Doesn't really matter right now, as non-exhaustive
        // conditionals cannot be used as expressions.
        val result = defaultValue(expression.type).materialized
        mv.mark(endLabel)
        return result
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: BlockInfo): PromisedValue {
        val typeOperand = expression.typeOperand
        val kotlinType = typeOperand.toKotlinType()
        return when (expression.operator) {
            IrTypeOperator.IMPLICIT_CAST ->
                expression.argument.accept(this, data).coerce(expression.type)

            IrTypeOperator.CAST, IrTypeOperator.SAFE_CAST -> {
                val result = expression.argument.accept(this, data)
                val boxedLeftType = typeMapper.boxType(result.irType)
                result.coerce(boxedLeftType, expression.argument.type).materialize()
                val boxedRightType = typeMapper.boxType(typeOperand)

                if (typeOperand.isReifiedTypeParameter) {
                    val operationKind = if (expression.operator == IrTypeOperator.CAST) AS else SAFE_AS
                    putReifiedOperationMarkerIfTypeIsReifiedParameter(typeOperand, operationKind, mv, this)
                    v.checkcast(boxedRightType)
                } else {
                    assert(expression.operator == IrTypeOperator.CAST) { "IrTypeOperator.SAFE_CAST should have been lowered." }
                    TypeIntrinsics.checkcast(mv, kotlinType, boxedRightType, false)
                }
                MaterialValue(this, boxedRightType, expression.type).coerce(expression.type)
            }

            IrTypeOperator.INSTANCEOF -> {
                expression.argument.accept(this, data).coerce(context.irBuiltIns.anyNType).materialize()
                val type = typeMapper.boxType(typeOperand)
                if (typeOperand.isReifiedTypeParameter) {
                    putReifiedOperationMarkerIfTypeIsReifiedParameter(typeOperand, ReifiedTypeInliner.OperationKind.IS, mv, this)
                    v.instanceOf(type)
                } else {
                    TypeIntrinsics.instanceOf(mv, kotlinType, type, state.languageVersionSettings.isReleaseCoroutines())
                }
                expression.onStack
            }

            else -> throw AssertionError("type operator ${expression.operator} should have been lowered")
        }
    }

    private fun IrExpression.markEndOfStatementIfNeeded() {
        when (this) {
            is IrWhen -> if (this.branches.size > 1) {
                this.markLineNumber(false)
            }
            is IrTry -> this.markLineNumber(false)
            is IrContainerExpression -> when (this.origin) {
                IrStatementOrigin.WHEN, IrStatementOrigin.IF ->
                    this.markLineNumber(false)
            }
        }
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: BlockInfo): PromisedValue {
        expression.markLineNumber(startOffset = true)
        val arity = expression.arguments.size
        when {
            arity == 0 -> mv.aconst("")
            arity == 1 -> {
                // Convert single arg to string.
                val arg = expression.arguments[0]
                val result = arg.accept(this, data).boxInlineClasses(arg.type).materialized
                if (!arg.type.isString()) {
                    result.genToString(mv)
                }
            }
            arity == 2 && expression.arguments[0].type.isStringClassType() -> {
                // Call the stringPlus intrinsic
                for (argument in expression.arguments) {
                    val result = argument.accept(this, data).boxInlineClasses(argument.type).materialized
                    if (result.type.sort != Type.OBJECT) {
                        result.genToString(mv)
                    }
                }
                mv.invokestatic(
                    IrIntrinsicMethods.INTRINSICS_CLASS_NAME,
                    "stringPlus",
                    "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/String;",
                    false
                )
            }
            else -> {
                // Use StringBuilder to concatenate.
                genStringBuilderConstructor(mv)
                for (argument in expression.arguments) {
                    genInvokeAppendMethod(mv, argument.accept(this, data).boxInlineClasses(argument.type).materialized.type, null)
                }
                mv.invokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
            }
        }
        return expression.onStack
    }

    private fun MaterialValue.genToString(v: InstructionAdapter) {
        val asmType =
            if (irType.getClass()?.isInline == true) OBJECT_TYPE else stringValueOfType(type)
        v.invokestatic("java/lang/String", "valueOf", Type.getMethodDescriptor(AsmTypes.JAVA_STRING_TYPE, asmType), false)
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
        return immaterialUnitValue
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
        return immaterialUnitValue
    }

    private fun unwindBlockStack(endLabel: Label, data: BlockInfo, stop: (ExpressionInfo) -> Boolean): ExpressionInfo? {
        return data.handleBlock {
            if (it is TryInfo)
                genFinallyBlock(it, null, endLabel, data)
            return if (stop(it)) it else unwindBlockStack(endLabel, data, stop)
        }
    }

    override fun visitBreakContinue(jump: IrBreakContinue, data: BlockInfo): PromisedValue {
        jump.markLineNumber(startOffset = true)
        val endLabel = Label()
        val stackElement = unwindBlockStack(endLabel, data) { it is LoopInfo && it.loop == jump.loop } as LoopInfo?
            ?: throw AssertionError("Target label for break/continue not found")
        mv.fixStackAndJump(if (jump is IrBreak) stackElement.breakLabel else stackElement.continueLabel)
        mv.mark(endLabel)
        return immaterialUnitValue
    }

    override fun visitTry(aTry: IrTry, data: BlockInfo): PromisedValue {
        aTry.markLineNumber(startOffset = true)
        return if (aTry.finallyExpression != null)
            data.withBlock(TryInfo(aTry.finallyExpression!!)) { visitTryWithInfo(aTry, data, it) }
        else
            visitTryWithInfo(aTry, data, null)
    }

    private fun visitTryWithInfo(aTry: IrTry, data: BlockInfo, tryInfo: TryInfo?): PromisedValue {
        val tryBlockStart = markNewLabel()
        mv.nop()
        val tryAsmType = aTry.asmType
        val tryResult = aTry.tryResult.accept(this, data)
        val isExpression = true //TODO: more wise check is required
        var savedValue: Int? = null
        if (isExpression) {
            tryResult.coerce(tryAsmType, aTry.type).materialize()
            savedValue = frameMap.enterTemp(tryAsmType)
            mv.store(savedValue, tryAsmType)
        } else {
            tryResult.discard()
        }

        val tryBlockEnd = markNewLabel()
        val tryBlockGaps = tryInfo?.gaps?.toList() ?: listOf()
        val tryCatchBlockEnd = Label()
        if (tryInfo != null) {
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
            mv.store(index, descriptorType)

            val catchBody = clause.result
            catchBody.markLineNumber(true)
            val catchResult = catchBody.accept(this, data)
            if (savedValue != null) {
                catchResult.coerce(tryAsmType, aTry.type).materialize()
                mv.store(savedValue, tryAsmType)
            } else {
                catchResult.discard()
            }

            frameMap.leave(clause.catchParameter)

            val clauseEnd = markNewLabel()

            mv.visitLocalVariable(
                parameter.name.asString(), descriptorType.descriptor, null, clauseStart, clauseEnd,
                index
            )

            if (tryInfo != null) {
                data.handleBlock { genFinallyBlock(tryInfo, tryCatchBlockEnd, null, data) }
            } else if (clause != catches.last()) {
                mv.goTo(tryCatchBlockEnd)
            }

            genTryCatchCover(clauseStart, tryBlockStart, tryBlockEnd, tryBlockGaps, descriptorType.internalName)
        }

        if (tryInfo != null) {
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
        if (savedValue != null) {
            mv.load(savedValue, tryAsmType)
            frameMap.leaveTemp(tryAsmType)
            return aTry.onStack
        }
        return immaterialUnitValue
    }

    private fun genTryCatchCover(catchStart: Label, tryStart: Label, tryEnd: Label, tryGaps: List<Pair<Label, Label>>, type: String?) {
        val lastRegionStart = tryGaps.fold(tryStart) { regionStart, (gapStart, gapEnd) ->
            mv.visitTryCatchBlock(regionStart, gapStart, catchStart, type)
            gapEnd
        }
        mv.visitTryCatchBlock(lastRegionStart, tryEnd, catchStart, type)
    }

    private fun genFinallyBlock(tryInfo: TryInfo, tryCatchBlockEnd: Label?, afterJumpLabel: Label?, data: BlockInfo) {
        val gapStart = markNewLabel()
        finallyDepth++
        if (isFinallyMarkerRequired()) {
            generateFinallyMarker(mv, finallyDepth, true)
        }
        tryInfo.onExit.accept(this, data).discard()
        if (isFinallyMarkerRequired()) {
            generateFinallyMarker(mv, finallyDepth, false)
        }
        finallyDepth--
        if (tryCatchBlockEnd != null) {
            tryInfo.onExit.markLineNumber(startOffset = false)
            mv.goTo(tryCatchBlockEnd)
        }
        tryInfo.gaps.add(gapStart to (afterJumpLabel ?: markNewLabel()))
    }

    fun generateFinallyBlocksIfNeeded(returnType: Type, afterReturnLabel: Label, data: BlockInfo, target: ReturnableBlockInfo? = null) {
        if (data.hasFinallyBlocks()) {
            if (Type.VOID_TYPE != returnType) {
                val returnValIndex = frameMap.enterTemp(returnType)
                mv.store(returnValIndex, returnType)
                unwindBlockStack(afterReturnLabel, data) { it == target }
                mv.load(returnValIndex, returnType)
                frameMap.leaveTemp(returnType)
            } else {
                unwindBlockStack(afterReturnLabel, data) { it == target }
            }
        }
    }

    override fun visitThrow(expression: IrThrow, data: BlockInfo): PromisedValue {
        expression.markLineNumber(startOffset = true)
        expression.value.accept(this, data).materialize()
        mv.athrow()
        return immaterialUnitValue
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
                putReifiedOperationMarkerIfTypeIsReifiedParameter(classType, ReifiedTypeInliner.OperationKind.JAVA_CLASS, mv, this)
            }

            val asmType = typeMapper.mapType(classType)
            if (classType.getClass()?.isInline == true || !isPrimitive(asmType)) {
                mv.aconst(typeMapper.boxType(classType))
            } else {
                mv.getstatic(boxType(asmType).internalName, "TYPE", "Ljava/lang/Class;")
            }
        } else {
            throw AssertionError("not an IrGetClass or IrClassReference: ${classReference.dump()}")
        }

        if (wrapIntoKClass) {
            wrapJavaClassIntoKClass(mv)
        }
        return classReference.onStack
    }

    private fun getOrCreateCallGenerator(element: IrFunctionAccessExpression, data: BlockInfo): IrCallGenerator {
        if (!element.symbol.owner.isInlineFunctionCall(context) ||
            classCodegen.irClass.fileParent.fileEntry is MultifileFacadeFileEntry
        ) {
            return IrCallGenerator.DefaultCallGenerator
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

        val mappings = IrTypeParameterMappings()
        for ((key, type) in typeArguments.entries) {
            val reificationArgument = extractReificationArgument(type)
            if (reificationArgument == null) {
                // type is not generic
                val signatureWriter = BothSignatureWriter(BothSignatureWriter.Mode.TYPE)
                val asmType = typeMapper.mapTypeParameter(type, signatureWriter)

                mappings.addParameterMappingToType(
                    key.name.identifier, type, asmType, signatureWriter.toString(), key.isReified
                )
            } else {
                mappings.addParameterMappingForFurtherReification(
                    key.name.identifier, type, reificationArgument, key.isReified
                )
            }
        }

        val original = (callee as? IrSimpleFunction)?.resolveFakeOverride() ?: irFunction
        return IrInlineCodegen(this, state, original.descriptor, mappings, IrSourceCompilerForInline(state, element, this, data))
    }

    private fun consumeReifiedOperationMarker(typeParameter: IrTypeParameter) {
        if (typeParameter.parent != irFunction) {
            classCodegen.reifiedTypeParametersUsages.addUsedReifiedParameter(typeParameter.name.asString())
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

    override fun markLineNumberAfterInlineIfNeeded() {
        // Inline function has its own line number which is in a separate instance of codegen,
        // therefore we need to reset lastLineNumber to force a line number generation after visiting inline function.
        lastLineNumber = -1
    }

    fun isFinallyMarkerRequired(): Boolean {
        return irFunction.isInline || isInlineLambda
    }

    /* Borrowed and modified from compiler/backend/src/org/jetbrains/kotlin/codegen/codegenUtil.kt */

    private fun extractReificationArgumentWithParameter(initialType: IrType): Pair<IrTypeParameter, IrReificationArgument>? {
        var type = initialType
        var arrayDepth = 0
        val isNullable = type.isMarkedNullable()
        while (type.isArray() || type.isNullableArray()) {
            arrayDepth++
            type = (type as IrSimpleType).arguments[0].safeAs<IrTypeProjection>()?.type ?: classCodegen.context.irBuiltIns.anyNType
        }

        val parameter = type.getTypeParameterOrNull() ?: return null

        return Pair(parameter, IrReificationArgument(parameter.name.asString(), isNullable, arrayDepth))
    }

    private fun extractReificationArgument(initialType: IrType): IrReificationArgument? = extractReificationArgumentWithParameter(initialType)?.second

    /* From ReifiedTypeInliner.kt */
    inner class IrReificationArgument(
        val parameterName: String, val nullable: Boolean, private val arrayDepth: Int
    ) {
        fun asString() = "[".repeat(arrayDepth) + parameterName + (if (nullable) "?" else "")
        fun combine(replacement: IrReificationArgument) =
            IrReificationArgument(
                replacement.parameterName,
                this.nullable || (replacement.nullable && this.arrayDepth == 0),
                this.arrayDepth + replacement.arrayDepth
            )

        fun reify(replacementAsmType: Type, irType: IrType) =
            Pair(
                Type.getType("[".repeat(arrayDepth) + replacementAsmType),
                irType.arrayOf(arrayDepth).withHasQuestionMark(nullable)
            )

        private fun IrType.arrayOf(arrayDepth: Int): IrType {
            val builtins = classCodegen.context.irBuiltIns
            var currentType = this

            repeat(arrayDepth) {
                currentType = builtins.arrayClass.typeWith(currentType)
            }

            return currentType
        }

        fun toReificationArgument() = ReificationArgument(parameterName, nullable, arrayDepth)
    }

    /* From ExpressionCodegen.java */
    fun putReifiedOperationMarkerIfTypeIsReifiedParameter(
        type: IrType, operationKind: ReifiedTypeInliner.OperationKind, v: InstructionAdapter,
        codegen: ExpressionCodegen?
    ) {
        val typeParameterAndReificationArgument = extractReificationArgumentWithParameter(type)
        if (typeParameterAndReificationArgument != null && typeParameterAndReificationArgument.first.isReified) {
            val irTypeParameter = typeParameterAndReificationArgument.first
            codegen?.consumeReifiedOperationMarker(irTypeParameter)
            ReifiedTypeInliner.putReifiedOperationMarker(
                operationKind, typeParameterAndReificationArgument.second.toReificationArgument(), v
            )
        }
    }
}

val IrType.isReifiedTypeParameter: Boolean
    get() = this.classifierOrNull?.safeAs<IrTypeParameterSymbol>()?.owner?.isReified == true

/* From typeUtil.java */
fun IrType.getTypeParameterOrNull() = classifierOrNull?.owner?.safeAs<IrTypeParameter>()

/* From ReifiedTypeInliner.kt */
class IrTypeParameterMappings {
    private val mappingsByName = hashMapOf<String, IrTypeParameterMapping>()

    fun addParameterMappingToType(name: String, type: IrType, asmType: Type, signature: String, isReified: Boolean) {
        mappingsByName[name] = IrTypeParameterMapping(
            name, type, asmType, reificationArgument = null, signature = signature, isReified = isReified
        )
    }

    fun addParameterMappingForFurtherReification(
        name: String,
        type: IrType,
        reificationArgument: ExpressionCodegen.IrReificationArgument,
        isReified: Boolean
    ) {
        mappingsByName[name] = IrTypeParameterMapping(
            name, type, asmType = null, reificationArgument = reificationArgument, signature = null, isReified = isReified
        )
    }

    operator fun get(name: String): IrTypeParameterMapping? = mappingsByName[name]

    fun hasReifiedParameters() = mappingsByName.values.any { it.isReified }

    internal inline fun forEach(l: (IrTypeParameterMapping) -> Unit) {
        mappingsByName.values.forEach(l)
    }

    fun toTypeParameterMappings() = TypeParameterMappings().also { result ->
        mappingsByName.forEach { (_, value) ->
            if (value.asmType == null) {
                result.addParameterMappingForFurtherReification(
                    value.name,
                    value.type.toKotlinType(),
                    value.reificationArgument!!.toReificationArgument(),
                    value.isReified
                )
            } else {
                result.addParameterMappingToType(
                    value.name,
                    value.type.toKotlinType(),
                    value.asmType,
                    value.signature!!,
                    value.isReified
                )
            }
        }
    }
}

class IrTypeParameterMapping(
    val name: String,
    val type: IrType,
    val asmType: Type?,
    val reificationArgument: ExpressionCodegen.IrReificationArgument?,
    val signature: String?,
    val isReified: Boolean
)
