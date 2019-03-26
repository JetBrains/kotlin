/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.common.descriptors.propertyIfAccessor
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.intrinsics.IntrinsicMethods
import org.jetbrains.kotlin.backend.jvm.intrinsics.JavaClassProperty
import org.jetbrains.kotlin.backend.jvm.intrinsics.Not
import org.jetbrains.kotlin.backend.jvm.lower.CrIrType
import org.jetbrains.kotlin.backend.jvm.lower.constantValue
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.AsmUtil.*
import org.jetbrains.kotlin.codegen.ExpressionCodegen.putReifiedOperationMarkerIfTypeIsReifiedParameter
import org.jetbrains.kotlin.codegen.inline.*
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner.OperationKind.AS
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner.OperationKind.SAFE_AS
import org.jetbrains.kotlin.codegen.pseudoInsns.fakeAlwaysFalseIfeq
import org.jetbrains.kotlin.codegen.pseudoInsns.fixStackAndJump
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.isReleaseCoroutines
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils.isEnumClass
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typesApproximation.approximateCapturedTypes
import org.jetbrains.kotlin.types.upperIfFlexible
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
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

class BlockInfo private constructor(val parent: BlockInfo?) {
    val variables = mutableListOf<VariableInfo>()
    val infos = Stack<ExpressionInfo>()

    fun create() = BlockInfo(this).apply {
        this@apply.infos.addAll(this@BlockInfo.infos)
    }

    fun hasFinallyBlocks(): Boolean = infos.firstIsInstanceOrNull<TryInfo>() != null

    inline fun <T : ExpressionInfo, R> withBlock(info: T, f: (T) -> R): R {
        infos.add(info)
        try {
            return f(info)
        } finally {
            infos.pop()
        }
    }

    inline fun <R> handleBlock(f: (ExpressionInfo) -> R): R? {
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

    companion object {
        fun create() = BlockInfo(null)
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

    val typeMapper = classCodegen.typeMapper

    val context = classCodegen.context

    private val state = classCodegen.state

    private val fileEntry = classCodegen.context.psiSourceManager.getFileEntry(irFunction.fileParent)

    override val visitor: InstructionAdapter
        get() = mv

    override val inlineNameGenerator: NameGenerator = NameGenerator("${classCodegen.type.internalName}\$todo") // TODO

    override var lastLineNumber: Int = -1

    private val KotlinType.asmType: Type
        get() = typeMapper.mapType(this)

    private val IrType.asmType: Type
        get() = toKotlinType().asmType

    private val CallableDescriptor.asmType: Type
        get() = typeMapper.mapType(this)

    val IrExpression.asmType: Type
        get() = type.asmType

    // Assume this expression's result has already been materialized on the stack
    // with the correct type.
    val IrExpression.onStack: MaterialValue
        get() = MaterialValue(mv, asmType)

    private fun markNewLabel() = Label().apply { mv.visitLabel(this) }

    private fun IrElement.markLineNumber(startOffset: Boolean) {
        val offset = if (startOffset) this.startOffset else endOffset
        if (offset < 0) {
            return
        }
        val lineNumber = fileEntry.getLineNumber(offset) + 1
        assert(lineNumber > 0)
        if (lastLineNumber != lineNumber) {
            lastLineNumber = lineNumber
            mv.visitLineNumber(lineNumber, markNewLabel())
        }
    }

    // TODO remove
    fun gen(expression: IrElement, type: Type, data: BlockInfo): StackValue {
        expression.accept(this, data).coerce(type).materialize()
        return StackValue.onStack(type)
    }

    fun generate() {
        mv.visitCode()
        val startLabel = markNewLabel()
        val info = BlockInfo.create()
        val body = irFunction.body!!
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
            val returnType = typeMapper.mapReturnType(irFunction.descriptor)
            result.coerce(returnType).materialize()
            mv.areturn(returnType)
        }
        writeLocalVariablesInTable(info)
        writeParameterInLocalVariableTable(startLabel)
        mv.visitEnd()
    }

    private fun writeParameterInLocalVariableTable(startLabel: Label) {
        if (!irFunction.isStatic) {
            mv.visitLocalVariable("this", classCodegen.type.descriptor, null, startLabel, markNewLabel(), 0)
        }
        val extensionReceiverParameter = irFunction.extensionReceiverParameter
        if (extensionReceiverParameter != null) {
            writeValueParameterInLocalVariableTable(extensionReceiverParameter, startLabel)
        }
        for (param in irFunction.valueParameters) {
            writeValueParameterInLocalVariableTable(param, startLabel)
        }
    }

    private fun writeValueParameterInLocalVariableTable(param: IrValueParameter, startLabel: Label) {
        val descriptor = param.descriptor
        val nameForDestructuredParameter = if (descriptor is ValueParameterDescriptor) {
            getNameForDestructuredParameterOrNull(descriptor)
        } else {
            null
        }

        val type = typeMapper.mapType(descriptor)
        // NOTE: we expect all value parameters to be present in the frame.
        mv.visitLocalVariable(
            nameForDestructuredParameter ?: param.name.asString(),
            type.descriptor, null, startLabel, markNewLabel(), findLocalIndex(param.symbol)
        )
    }

    override fun visitBlock(expression: IrBlock, data: BlockInfo): PromisedValue {
        if (expression.isTransparentScope)
            return super.visitBlock(expression, data)
        val info = data.create()
        // Force materialization to avoid reading from out-of-scope variables.
        return super.visitBlock(expression, info).materialized.apply {
            writeLocalVariablesInTable(info)
        }
    }

    private fun writeLocalVariablesInTable(info: BlockInfo) {
        val endLabel = markNewLabel()
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
        container.statements.fold(voidValue as PromisedValue) { prev, exp ->
            prev.discard()
            exp.accept(this, data).also { (exp as? IrExpression)?.markEndOfStatementIfNeeded() }
        }

    override fun visitBlockBody(body: IrBlockBody, data: BlockInfo) =
        visitStatementContainer(body, data).discard()

    override fun visitContainerExpression(expression: IrContainerExpression, data: BlockInfo) =
        visitStatementContainer(expression, data).coerce(expression.asmType)

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: BlockInfo): PromisedValue {
        expression.markLineNumber(startOffset = true)
        mv.load(0, OBJECT_TYPE) // HACK
        return generateCall(expression, null, data)
    }

    override fun visitCall(expression: IrCall, data: BlockInfo): PromisedValue {
        expression.markLineNumber(startOffset = true)
        if (expression.descriptor is ConstructorDescriptor) {
            throw AssertionError("IrCall with ConstructorDescriptor: ${expression.javaClass.simpleName}")
        }
        return generateCall(expression, expression.superQualifier, data)
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: BlockInfo): PromisedValue {
        val type = expression.asmType
        if (type.sort == Type.ARRAY) {
            //noinspection ConstantConditions
            return generateNewArray(expression, data)
        }

        mv.anew(expression.asmType)
        mv.dup()
        generateCall(expression, null, data)
        return expression.onStack
    }

    fun generateNewArray(expression: IrConstructorCall, data: BlockInfo): PromisedValue {
        val args = expression.descriptor.valueParameters
        assert(args.size == 1 || args.size == 2) { "Unknown constructor called: " + args.size + " arguments" }

        if (args.size == 1) {
            // TODO move to the intrinsic
            expression.getValueArgument(0)!!.accept(this, data).coerce(Type.INT_TYPE).materialize()
            newArrayInstruction(expression.type.toKotlinType())
            return expression.onStack
        }

        return generateCall(expression, null, data)
    }

    private fun generateCall(expression: IrFunctionAccessExpression, superQualifier: ClassDescriptor?, data: BlockInfo): PromisedValue {
        classCodegen.context.irIntrinsics.getIntrinsic(expression.descriptor.original as CallableMemberDescriptor)
            ?.invoke(expression, this, data)?.let { return it.coerce(expression.asmType) }
        val isSuperCall = superQualifier != null
        val callable = resolveToCallable(expression, isSuperCall)
        return generateCall(expression, callable, data, isSuperCall)
    }

    fun generateCall(
        expression: IrMemberAccessExpression,
        callable: Callable,
        data: BlockInfo,
        isSuperCall: Boolean = false
    ): PromisedValue {
        val callGenerator = getOrCreateCallGenerator(expression, expression.descriptor, data)

        val receiver = expression.dispatchReceiver
        receiver?.apply {
            callGenerator.genValueAndPut(
                null, this,
                if (isSuperCall) receiver.asmType else callable.dispatchReceiverType
                    ?: throw AssertionError("No dispatch receiver type: ${expression.render()}"),
                -1, this@ExpressionCodegen, data
            )
        }

        expression.extensionReceiver?.apply {
            callGenerator.genValueAndPut(null, this, callable.extensionReceiverType!!, -1, this@ExpressionCodegen, data)
        }

        callGenerator.beforeValueParametersStart()
        val defaultMask = DefaultCallArgs(callable.valueParameterTypes.size)
        val extraArgsShift =
            when {
                expression.descriptor is ConstructorDescriptor && isEnumClass(expression.descriptor.containingDeclaration) -> 2
                expression.descriptor is ConstructorDescriptor &&
                        (expression.descriptor.containingDeclaration as ClassDescriptor).isInner -> 1 // skip the `$outer` parameter
                else -> 0
            }
        expression.descriptor.valueParameters.forEachIndexed { i, parameterDescriptor ->
            val arg = expression.getValueArgument(i)
            val parameterType = callable.valueParameterTypes[i]
            when {
                arg != null -> {
                    callGenerator.genValueAndPut(parameterDescriptor, arg, parameterType, i, this@ExpressionCodegen, data)
                }
                parameterDescriptor.hasDefaultValue() -> {
                    callGenerator.putValueIfNeeded(
                        parameterType,
                        StackValue.createDefaultValue(parameterType),
                        ValueKind.DEFAULT_PARAMETER,
                        i,
                        this@ExpressionCodegen
                    )
                    defaultMask.mark(i - extraArgsShift/*TODO switch to separate lower*/)
                }
                else -> {
                    assert(parameterDescriptor.varargElementType != null)
                    //empty vararg

                    // Upper bound for type of vararg parameter should always have a form of 'Array<out T>',
                    // while its lower bound may be Nothing-typed after approximation
                    val type = typeMapper.mapType(parameterDescriptor.type.upperIfFlexible())
                    callGenerator.putValueIfNeeded(
                        parameterType,
                        StackValue.operation(type) {
                            it.aconst(0)
                            it.newarray(correctElementType(type))
                        },
                        ValueKind.GENERAL_VARARG, i, this@ExpressionCodegen
                    )
                }
            }
        }

        callGenerator.genCall(
            callable,
            defaultMask.generateOnStackIfNeeded(callGenerator, expression.descriptor is ConstructorDescriptor, this),
            this,
            expression
        )

        val returnType = expression.descriptor.returnType
        if (returnType != null && returnType.isNothing()) {
            mv.aconst(null)
            mv.athrow()
            return voidValue
        } else if (expression.descriptor is ConstructorDescriptor) {
            return voidValue
        } else if (expression.type.isUnit()) {
            // NewInference allows casting `() -> T` to `() -> Unit`. A CHECKCAST here will fail.
            return MaterialValue(mv, callable.returnType).discard().coerce(expression.asmType)
        }
        return MaterialValue(mv, callable.returnType).coerce(expression.asmType)
    }

    override fun visitVariable(declaration: IrVariable, data: BlockInfo): PromisedValue {
        val varType = typeMapper.mapType(declaration.descriptor)
        val index = frameMap.enter(declaration.symbol, varType)

        declaration.markLineNumber(startOffset = true)

        declaration.initializer?.let {
            it.accept(this, data).coerce(varType).materialize()
            mv.store(index, varType)
            it.markLineNumber(startOffset = true)
        }

        data.variables.add(VariableInfo(declaration, index, varType, markNewLabel()))
        return voidValue
    }

    override fun visitGetValue(expression: IrGetValue, data: BlockInfo): PromisedValue {
        // Do not generate line number information for loads from compiler-generated
        // temporary variables. They do not correspond to variable loads in user code.
        if (expression.symbol.owner.origin != IrDeclarationOrigin.IR_TEMPORARY_VARIABLE)
            expression.markLineNumber(startOffset = true)
        mv.load(findLocalIndex(expression.symbol), expression.asmType)
        return expression.onStack
    }

    override fun visitFieldAccess(expression: IrFieldAccessExpression, data: BlockInfo): PromisedValue {
        expression.symbol.owner.constantValue()?.let {
            // Handling const reads before codegen is important for constant folding.
            assert(expression is IrSetField) { "read of const val ${expression.symbol.owner.name} not inlined by ConstLowering" }
            // This can only be the field's initializer; JVM implementations are required
            // to generate those for ConstantValue-marked fields automatically, so this is redundant.
            return voidValue.coerce(expression.asmType)
        }

        val realDescriptor = DescriptorUtils.unwrapFakeOverride(expression.descriptor)
        val fieldType = typeMapper.mapType(realDescriptor.original.type)
        val ownerType = typeMapper.mapImplementationOwner(expression.descriptor).internalName
        val fieldName = expression.descriptor.name.asString()
        val isStatic = expression.receiver == null
        expression.markLineNumber(startOffset = true)
        expression.receiver?.accept(this, data)?.materialize()
        return if (expression is IrSetField) {
            expression.value.accept(this, data).coerce(fieldType).materialize()
            when {
                isStatic -> mv.putstatic(ownerType, fieldName, fieldType.descriptor)
                else -> mv.putfield(ownerType, fieldName, fieldType.descriptor)
            }
            voidValue.coerce(expression.asmType)
        } else {
            when {
                isStatic -> mv.getstatic(ownerType, fieldName, fieldType.descriptor)
                else -> mv.getfield(ownerType, fieldName, fieldType.descriptor)
            }
            MaterialValue(mv, fieldType).coerce(expression.asmType)
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
        return if (skip) voidValue.coerce(expression.asmType) else super.visitSetField(expression, data)
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
        if (index >= 0) {
            return index
        }
        if (irFunction.dispatchReceiverParameter != null && (irFunction.parent as? IrClass)?.thisReceiver?.symbol == irSymbol) {
            return 0
        }
        val dump = if (irSymbol.isBound) irSymbol.owner.dump() else irSymbol.descriptor.toString()
        throw AssertionError("Non-mapped local declaration: $dump\n in ${irFunction.dump()}")
    }

    override fun visitSetVariable(expression: IrSetVariable, data: BlockInfo): PromisedValue {
        expression.markLineNumber(startOffset = true)
        expression.value.markLineNumber(startOffset = true)
        expression.value.accept(this, data).coerce(expression.descriptor.asmType).materialize()
        mv.store(findLocalIndex(expression.symbol), expression.descriptor.asmType)
        return voidValue.coerce(expression.asmType)
    }

    override fun <T> visitConst(expression: IrConst<T>, data: BlockInfo): PromisedValue {
        expression.markLineNumber(startOffset = true)
        when (val value = expression.value) {
            is Boolean -> return object : BooleanValue(mv) {
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

    // TODO maybe remove?
    override fun visitClass(declaration: IrClass, data: BlockInfo): PromisedValue {
        classCodegen.generateLocalClass(declaration)
        return voidValue
    }

    override fun visitVararg(expression: IrVararg, data: BlockInfo): PromisedValue {
        expression.markLineNumber(startOffset = true)
        val outType = expression.type
        val type = expression.asmType
        assert(type.sort == Type.ARRAY)
        val elementType = correctElementType(type)
        val arguments = expression.elements
        val size = arguments.size

        val hasSpread = arguments.firstIsInstanceOrNull<IrSpreadElement>() != null

        if (hasSpread) {
            val arrayOfReferences = KotlinBuiltIns.isArray(outType.toKotlinType())
            if (size == 1) {
                // Arrays.copyOf(receiverValue, newLength)
                val argument = (arguments[0] as IrSpreadElement).expression
                val arrayType = if (arrayOfReferences)
                    Type.getType("[Ljava/lang/Object;")
                else
                    Type.getType("[" + elementType.descriptor)
                argument.accept(this, data).coerce(type).materialize()
                mv.dup()
                mv.arraylength()
                mv.invokestatic("java/util/Arrays", "copyOf", Type.getMethodDescriptor(arrayType, arrayType, Type.INT_TYPE), false)
                if (arrayOfReferences) {
                    mv.checkcast(type)
                }
            } else {
                val owner: String
                val addDescriptor: String
                val toArrayDescriptor: String
                if (arrayOfReferences) {
                    owner = "kotlin/jvm/internal/SpreadBuilder"
                    addDescriptor = "(Ljava/lang/Object;)V"
                    toArrayDescriptor = "([Ljava/lang/Object;)[Ljava/lang/Object;"
                } else {
                    val spreadBuilderClassName =
                        AsmUtil.asmPrimitiveTypeToLangPrimitiveType(elementType)!!.typeName.identifier + "SpreadBuilder"
                    owner = "kotlin/jvm/internal/" + spreadBuilderClassName
                    addDescriptor = "(" + elementType.descriptor + ")V"
                    toArrayDescriptor = "()" + type.descriptor
                }
                mv.anew(Type.getObjectType(owner))
                mv.dup()
                mv.iconst(size)
                mv.invokespecial(owner, "<init>", "(I)V", false)
                for (i in 0..size - 1) {
                    mv.dup()
                    val argument = arguments[i]
                    if (argument is IrSpreadElement) {
                        argument.expression.accept(this, data).coerce(AsmTypes.OBJECT_TYPE).materialize()
                        mv.invokevirtual(owner, "addSpread", "(Ljava/lang/Object;)V", false)
                    } else {
                        argument.accept(this, data).coerce(elementType).materialize()
                        mv.invokevirtual(owner, "add", addDescriptor, false)
                    }
                }
                if (arrayOfReferences) {
                    mv.dup()
                    mv.invokevirtual(owner, "size", "()I", false)
                    newArrayInstruction(outType.toKotlinType())
                    mv.invokevirtual(owner, "toArray", toArrayDescriptor, false)
                    mv.checkcast(type)
                } else {
                    mv.invokevirtual(owner, "toArray", toArrayDescriptor, false)
                }
            }
        } else {
            mv.iconst(size)
            newArrayInstruction(expression.type.toKotlinType())
            for ((i, element) in expression.elements.withIndex()) {
                mv.dup()
                mv.iconst(i)
                element.accept(this, data).coerce(elementType).materialize()
                mv.astore(elementType)
            }
        }
        return expression.onStack
    }

    fun newArrayInstruction(arrayType: KotlinType) {
        if (KotlinBuiltIns.isArray(arrayType)) {
            val elementKotlinType = arrayType.arguments[0].type
            putReifiedOperationMarkerIfTypeIsReifiedParameter(
                elementKotlinType,
                ReifiedTypeInliner.OperationKind.NEW_ARRAY,
                mv,
                this
            )
            mv.newarray(boxType(elementKotlinType.asmType))
        } else {
            val type = typeMapper.mapType(arrayType)
            mv.newarray(correctElementType(type))
        }
    }

    override fun visitReturn(expression: IrReturn, data: BlockInfo): PromisedValue {
        val owner = expression.returnTargetSymbol.owner
        //TODO: should be owner != irFunction
        val isNonLocalReturn = state.typeMapper.mapFunctionName(
            owner.descriptor,
            OwnerKind.IMPLEMENTATION
        ) != state.typeMapper.mapFunctionName(irFunction.descriptor, OwnerKind.IMPLEMENTATION)
        if (isNonLocalReturn && state.isInlineDisabled) {
            //TODO: state.diagnostics.report(Errors.NON_LOCAL_RETURN_IN_DISABLED_INLINE.on(expression))
            genThrow(
                mv, "java/lang/UnsupportedOperationException",
                "Non-local returns are not allowed with inlining disabled"
            )
            return voidValue
        }

        val returnType = typeMapper.mapReturnType(owner.descriptor)
        val afterReturnLabel = Label()
        expression.value.accept(this, data).coerce(returnType).materialize()
        generateFinallyBlocksIfNeeded(returnType, afterReturnLabel, data)
        expression.markLineNumber(startOffset = true)
        if (isNonLocalReturn) {
            generateGlobalReturnFlag(mv, (owner as IrFunction).name.asString())
        }
        mv.areturn(returnType)
        mv.mark(afterReturnLabel)
        mv.nop()/*TODO check RESTORE_STACK_IN_TRY_CATCH processor*/
        return voidValue
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
            val result = branch.result.accept(this, data).coerce(expression.asmType).materialized
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
        val result = voidValue.coerce(expression.asmType).materialized
        mv.mark(endLabel)
        return result
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: BlockInfo): PromisedValue {
        val kotlinType = expression.typeOperand.toKotlinType()
        val asmType = kotlinType.asmType
        return when (expression.operator) {
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> {
                expression.argument.accept(this, data).discard()
                expression.argument.markEndOfStatementIfNeeded()
                voidValue.coerce(expression.asmType)
            }

            IrTypeOperator.IMPLICIT_CAST, IrTypeOperator.IMPLICIT_INTEGER_COERCION -> {
                expression.argument.accept(this, data).coerce(expression.asmType)
            }

            IrTypeOperator.CAST, IrTypeOperator.SAFE_CAST -> {
                expression.argument.accept(this, data).coerce(AsmTypes.OBJECT_TYPE).materialize()
                val boxedType = boxType(asmType)

                if (TypeUtils.isReifiedTypeParameter(kotlinType)) {
                    putReifiedOperationMarkerIfTypeIsReifiedParameter(
                        kotlinType, if (IrTypeOperator.SAFE_CAST == expression.operator) SAFE_AS else AS, mv, this
                    )
                    v.checkcast(boxedType)
                } else {
                    generateAsCast(
                        mv, kotlinType, boxedType, expression.operator == IrTypeOperator.SAFE_CAST,
                        state.languageVersionSettings.isReleaseCoroutines()
                    )
                }
                MaterialValue(mv, boxedType).coerce(expression.asmType)
            }

            IrTypeOperator.INSTANCEOF, IrTypeOperator.NOT_INSTANCEOF -> {
                expression.argument.accept(this, data).coerce(AsmTypes.OBJECT_TYPE).materialize()
                val type = boxType(asmType)
                if (TypeUtils.isReifiedTypeParameter(kotlinType)) {
                    putReifiedOperationMarkerIfTypeIsReifiedParameter(kotlinType, ReifiedTypeInliner.OperationKind.IS, mv, this)
                    v.instanceOf(type)
                } else {
                    generateIsCheck(mv, kotlinType, type, state.languageVersionSettings.isReleaseCoroutines())
                }
                // TODO remove this type operator, generate an intrinsic call around INSTANCEOF instead
                if (IrTypeOperator.NOT_INSTANCEOF == expression.operator)
                    Not.BooleanNegation(expression.onStack.coerceToBoolean())
                else
                    expression.onStack
            }

            IrTypeOperator.IMPLICIT_NOTNULL -> {
                val value = expression.argument.accept(this, data).materialized
                mv.dup()
                mv.visitLdcInsn("TODO provide message for IMPLICIT_NOTNULL") /*TODO*/
                mv.invokestatic(
                    "kotlin/jvm/internal/Intrinsics", "checkExpressionValueIsNotNull",
                    "(Ljava/lang/Object;Ljava/lang/String;)V", false
                )
                // Unbox primitives.
                value.coerce(expression.asmType)
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
                val type = expression.arguments[0].accept(this, data).materialized.type
                if (!expression.arguments[0].type.isString())
                    AsmUtil.genToString(StackValue.onStack(type), type, null, typeMapper).put(expression.asmType, mv)
            }
            arity == 2 && expression.arguments[0].type.isStringClassType() -> {
                // Call the stringPlus intrinsic
                expression.arguments.forEach {
                    val type = it.accept(this, data).materialized.type
                    if (type.sort != Type.OBJECT)
                        AsmUtil.genToString(StackValue.onStack(type), type, null, typeMapper).put(expression.asmType, mv)
                }
                mv.invokestatic(
                    IntrinsicMethods.INTRINSICS_CLASS_NAME,
                    "stringPlus",
                    "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/String;",
                    false
                )
            }
            else -> {
                // Use StringBuilder to concatenate.
                AsmUtil.genStringBuilderConstructor(mv)
                expression.arguments.forEach {
                    AsmUtil.genInvokeAppendMethod(mv, it.accept(this, data).materialized.type, null)
                }
                mv.invokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
            }
        }
        return expression.onStack
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
        return voidValue
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
        return voidValue
    }

    private fun unwindBlockStack(endLabel: Label, data: BlockInfo, loop: IrLoop? = null): LoopInfo? {
        return data.handleBlock {
            when {
                it is TryInfo -> genFinallyBlock(it, null, endLabel, data)
                it is LoopInfo && it.loop == loop -> return it
            }
            unwindBlockStack(endLabel, data, loop)
        }
    }

    override fun visitBreakContinue(jump: IrBreakContinue, data: BlockInfo): PromisedValue {
        jump.markLineNumber(startOffset = true)
        val endLabel = Label()
        val stackElement = unwindBlockStack(endLabel, data, jump.loop)
            ?: throw AssertionError("Target label for break/continue not found")
        mv.fixStackAndJump(if (jump is IrBreak) stackElement.breakLabel else stackElement.continueLabel)
        mv.mark(endLabel)
        return voidValue
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
            tryResult.coerce(tryAsmType).materialize()
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
            val descriptor = clause.parameter
            val descriptorType = descriptor.asmType
            val index = frameMap.enter(clause.catchParameter, descriptorType)
            mv.store(index, descriptorType)

            val catchBody = clause.result
            catchBody.markLineNumber(true)
            val catchResult = catchBody.accept(this, data)
            if (savedValue != null) {
                catchResult.coerce(tryAsmType).materialize()
                mv.store(savedValue, tryAsmType)
            } else {
                catchResult.discard()
            }

            frameMap.leave(clause.catchParameter)

            val clauseEnd = markNewLabel()

            mv.visitLocalVariable(
                descriptor.name.asString(), descriptorType.descriptor, null, clauseStart, clauseEnd,
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
        return voidValue
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

    fun generateFinallyBlocksIfNeeded(returnType: Type, afterReturnLabel: Label, data: BlockInfo) {
        if (data.hasFinallyBlocks()) {
            if (Type.VOID_TYPE != returnType) {
                val returnValIndex = frameMap.enterTemp(returnType)
                mv.store(returnValIndex, returnType)
                unwindBlockStack(afterReturnLabel, data, null)
                mv.load(returnValIndex, returnType)
                frameMap.leaveTemp(returnType)
            } else {
                unwindBlockStack(afterReturnLabel, data, null)
            }
        }
    }

    override fun visitThrow(expression: IrThrow, data: BlockInfo): PromisedValue {
        expression.markLineNumber(startOffset = true)
        expression.value.accept(this, data).coerce(AsmTypes.JAVA_THROWABLE_TYPE).materialize()
        mv.athrow()
        return voidValue
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
            if (classType is CrIrType) {
                putJavaLangClassInstance(mv, classType.type, null, typeMapper)
                return classReference.onStack
            } else {
                val kotlinType = classType.toKotlinType()
                if (TypeUtils.isTypeParameter(kotlinType)) {
                    assert(TypeUtils.isReifiedTypeParameter(kotlinType)) { "Non-reified type parameter under ::class should be rejected by type checker: " + kotlinType }
                    putReifiedOperationMarkerIfTypeIsReifiedParameter(kotlinType, ReifiedTypeInliner.OperationKind.JAVA_CLASS, mv, this)
                }

                putJavaLangClassInstance(mv, typeMapper.mapType(kotlinType), kotlinType, typeMapper)
            }
        } else {
            throw AssertionError("not an IrGetClass or IrClassReference: ${classReference.dump()}")
        }

        if (wrapIntoKClass) {
            wrapJavaClassIntoKClass(mv)
        }
        return classReference.onStack
    }

    private fun resolveToCallable(irCall: IrMemberAccessExpression, isSuper: Boolean): Callable {
        var descriptor = irCall.descriptor
        if (descriptor is TypeAliasConstructorDescriptor) {
            throw AssertionError("TypeAliasConstructorDescriptor should be unwrapped in psi2ir: $descriptor")
        }
        if (descriptor is PropertyDescriptor) {
            descriptor = descriptor.getter!!
        }
        if (descriptor is CallableMemberDescriptor && JvmCodegenUtil.getDirectMember(descriptor) is SyntheticJavaPropertyDescriptor) {
            val propertyDescriptor = JvmCodegenUtil.getDirectMember(descriptor) as SyntheticJavaPropertyDescriptor
            descriptor = if (descriptor is PropertyGetterDescriptor) {
                propertyDescriptor.getMethod
            } else {
                propertyDescriptor.setMethod!!
            }
        }
        return typeMapper.mapToCallableMethod(descriptor as FunctionDescriptor, isSuper)
    }

    private fun getOrCreateCallGenerator(
        descriptor: CallableDescriptor,
        element: IrMemberAccessExpression?,
        typeParameterMappings: TypeParameterMappings?,
        isDefaultCompilation: Boolean,
        data: BlockInfo
    ): IrCallGenerator {
        if (element == null) return IrCallGenerator.DefaultCallGenerator

        // We should inline callable containing reified type parameters even if inline is disabled
        // because they may contain something to reify and straight call will probably fail at runtime
        val isInline = descriptor.isInlineCall(state)

        if (!isInline) return IrCallGenerator.DefaultCallGenerator

        val original = unwrapInitialSignatureDescriptor(DescriptorUtils.unwrapFakeOverride(descriptor.original as FunctionDescriptor))
        return if (isDefaultCompilation) {
            TODO()
        } else {
            IrInlineCodegen(this, state, original, typeParameterMappings!!, IrSourceCompilerForInline(state, element, this, data))
        }
    }

    private fun getOrCreateCallGenerator(
        memberAccessExpression: IrMemberAccessExpression,
        descriptor: CallableDescriptor,
        data: BlockInfo
    ): IrCallGenerator {
        val typeArguments =
            if (memberAccessExpression.typeArgumentsCount == 0) {
                //avoid ambiguity with type constructor type parameters
                emptyMap()
            } else (descriptor.propertyIfAccessor as? CallableDescriptor)?.original?.typeParameters?.keysToMap {
                memberAccessExpression.getTypeArgumentOrDefault(it)
            } ?: emptyMap()

        val mappings = TypeParameterMappings()
        for (entry in typeArguments.entries) {
            val key = entry.key
            val type = entry.value

            val isReified = key.isReified || InlineUtil.isArrayConstructorWithLambda(descriptor)

            val typeParameterAndReificationArgument = extractReificationArgument(type)
            if (typeParameterAndReificationArgument == null) {
                val approximatedType = approximateCapturedTypes(entry.value).upper
                // type is not generic
                val signatureWriter = BothSignatureWriter(BothSignatureWriter.Mode.TYPE)
                val asmType = typeMapper.mapTypeParameter(approximatedType, signatureWriter)

                mappings.addParameterMappingToType(
                    key.name.identifier, approximatedType, asmType, signatureWriter.toString(), isReified
                )
            } else {
                mappings.addParameterMappingForFurtherReification(
                    key.name.identifier, type, typeParameterAndReificationArgument.second, isReified
                )
            }
        }

        return getOrCreateCallGenerator(descriptor, memberAccessExpression, mappings, false, data)
    }

    override fun consumeReifiedOperationMarker(typeParameterDescriptor: TypeParameterDescriptor) {
        //TODO
    }

    override fun propagateChildReifiedTypeParametersUsages(reifiedTypeParametersUsages: ReifiedTypeParametersUsages) {
        //TODO
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
        //TODO
    }

    fun isFinallyMarkerRequired(): Boolean {
        return irFunction.isInline || isInlineLambda
    }
}

fun DefaultCallArgs.generateOnStackIfNeeded(callGenerator: IrCallGenerator, isConstructor: Boolean, codegen: ExpressionCodegen): Boolean {
    val toInts = toInts()
    if (!toInts.isEmpty()) {
        for (mask in toInts) {
            callGenerator.putValueIfNeeded(Type.INT_TYPE, StackValue.constant(mask, Type.INT_TYPE), ValueKind.DEFAULT_MASK, -1, codegen)
        }

        val parameterType = if (isConstructor) AsmTypes.DEFAULT_CONSTRUCTOR_MARKER else AsmTypes.OBJECT_TYPE
        callGenerator.putValueIfNeeded(
            parameterType,
            StackValue.constant(null, parameterType),
            ValueKind.METHOD_HANDLE_IN_DEFAULT,
            -1,
            codegen
        )
    }
    return toInts.isNotEmpty()
}

internal fun CallableDescriptor.isInlineCall(state: GenerationState) =
    (!state.isInlineDisabled || InlineUtil.containsReifiedTypeParameters(this)) &&
            (InlineUtil.isInline(this) || InlineUtil.isArrayConstructorWithLambda(this))
