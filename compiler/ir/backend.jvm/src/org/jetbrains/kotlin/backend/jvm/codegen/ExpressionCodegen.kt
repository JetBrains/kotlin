/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.intrinsics.IrIntrinsicMethods
import org.jetbrains.kotlin.backend.jvm.lower.CrIrType
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.AsmUtil.*
import org.jetbrains.kotlin.codegen.ExpressionCodegen.putReifiedOperationMarkerIfTypeIsReifiedParameter
import org.jetbrains.kotlin.codegen.StackValue.*
import org.jetbrains.kotlin.codegen.inline.NameGenerator
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeParametersUsages
import org.jetbrains.kotlin.codegen.inline.TypeParameterMappings
import org.jetbrains.kotlin.codegen.intrinsics.JavaClassProperty
import org.jetbrains.kotlin.codegen.pseudoInsns.fakeAlwaysFalseIfeq
import org.jetbrains.kotlin.codegen.pseudoInsns.fakeAlwaysTrueIfeq
import org.jetbrains.kotlin.codegen.pseudoInsns.fixStackAndJump
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter
import org.jetbrains.kotlin.config.isReleaseCoroutines
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toKotlinType
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
import org.jetbrains.kotlin.types.typeUtil.isUnit
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
    private val irFunction: IrFunction,
    private val frame: IrFrameMap,
    val mv: InstructionAdapter,
    val classCodegen: ClassCodegen
) : IrElementVisitor<StackValue, BlockInfo>, BaseExpressionCodegen {

    private val intrinsics = IrIntrinsicMethods(classCodegen.context.irBuiltIns)

    val typeMapper = classCodegen.typeMapper

    val context = classCodegen.context

    private val state = classCodegen.state

    private val fileEntry = classCodegen.context.psiSourceManager.getFileEntry(irFunction.fileParent)

    override val frameMap: IrFrameMap
        get() = frame

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

    private val IrExpression.asmType: Type
        get() = type.asmType

    // Assume this expression's result has already been materialized on the stack
    // with the correct type.
    val IrExpression.onStack: StackValue
        get() = StackValue.onStack(asmType, type.toKotlinType())

    // If this value is immaterial, construct an object on the top of the stack. This operation is
    // idempotent and must always be done before generating other values or emitting raw bytecode.
    internal fun StackValue.materialize(): StackValue {
        put(type, kotlinType, mv)
        return onStack(type, kotlinType)
    }

    // Same as above, but if the type is Unit, do not materialize the actual push of a constant value.
    // This must be done before fan-in jumping to normalize the stack heights.
    internal fun StackValue.materializeNonUnit(): StackValue =
        if (kotlinType != null && kotlinType!!.isUnit())
            discard().coerce(kotlinType!!)
        else
            materialize()

    // Materialize and disregard this value. Materialization is forced because, presumably,
    // we only wanted the side effects anyway.
    internal fun StackValue.discard(): StackValue =
        coerce(Type.VOID_TYPE).materialize()

    // On materialization, cast the value to a different type.
    private fun StackValue.coerce(targetType: Type): StackValue =
        CoercionValue(this, targetType, null, null)

    private fun StackValue.coerce(toKotlinType: KotlinType): StackValue {
        // Avoid the code in `StackValue` that attempts to CHECKCAST java.lang.Objects into Unit.
        if (type != Type.VOID_TYPE && toKotlinType.isUnit())
            return coerce(Type.VOID_TYPE).coerce(toKotlinType)
        return CoercionValue(this, toKotlinType.asmType, toKotlinType, null)
    }

    internal fun StackValue.coerce(toIrType: IrType): StackValue =
        coerce(toIrType.toKotlinType())

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
        return expression.accept(this, data).coerce(type).materialize()
    }

    // TODO remove
    fun gen(expression: IrElement, data: BlockInfo): StackValue {
        return expression.accept(this, data).materialize()
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

    override fun visitBlock(expression: IrBlock, data: BlockInfo): StackValue {
        val info = data.create()
        return super.visitBlock(expression, info).apply {
            if (!expression.isTransparentScope) {
                writeLocalVariablesInTable(info)
            } else {
                info.variables.forEach {
                    data.variables.add(it)
                }
            }
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
            frame.leave(it.declaration.symbol)
        }
    }

    private fun visitStatementContainer(container: IrStatementContainer, data: BlockInfo): StackValue =
        container.statements.fold(none()) { prev, exp ->
            prev.discard()
            exp.accept(this, data).also { (exp as? IrExpression)?.markEndOfStatementIfNeeded() }
        }

    override fun visitBlockBody(body: IrBlockBody, data: BlockInfo): StackValue {
        return visitStatementContainer(body, data).discard()
    }

    override fun visitContainerExpression(expression: IrContainerExpression, data: BlockInfo): StackValue {
        return visitStatementContainer(expression, data).coerce(expression.type)
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: BlockInfo): StackValue {
        expression.markLineNumber(startOffset = true)
        return generateCall(expression, null, data)
    }

    override fun visitCall(expression: IrCall, data: BlockInfo): StackValue {
        expression.markLineNumber(startOffset = true)
        if (expression.descriptor is ConstructorDescriptor) {
            return generateNewCall(expression, data)
        }
        return generateCall(expression, expression.superQualifier, data)
    }

    private fun generateNewCall(expression: IrCall, data: BlockInfo): StackValue {
        val type = expression.asmType
        if (type.sort == Type.ARRAY) {
            return generateNewArray(expression, data)
        }

        mv.anew(expression.asmType)
        mv.dup()
        generateCall(expression, expression.superQualifier, data)
        return expression.onStack
    }

    private fun generateNewArray(expression: IrCall, data: BlockInfo): StackValue {
        val args = expression.descriptor.valueParameters
        assert(args.size == 1 || args.size == 2) { "Unknown constructor called: " + args.size + " arguments" }

        if (args.size == 1) {
            // TODO move to the intrinsic
            expression.getValueArgument(0)!!.accept(this, data).coerce(Type.INT_TYPE).materialize()
            newArrayInstruction(expression.type.toKotlinType())
            return expression.onStack
        }

        return generateCall(expression, expression.superQualifier, data)
    }

    private fun generateCall(expression: IrFunctionAccessExpression, superQualifier: ClassDescriptor?, data: BlockInfo): StackValue {
        intrinsics.getIntrinsic(expression.descriptor.original as CallableMemberDescriptor)
            ?.invoke(expression, this, data)?.let { return it }
        val isSuperCall = superQualifier != null
        val callable = resolveToCallable(expression, isSuperCall)
        return generateCall(expression, callable, data, isSuperCall)
    }

    fun generateCall(expression: IrMemberAccessExpression, callable: Callable, data: BlockInfo, isSuperCall: Boolean = false): StackValue {
        val callGenerator = getOrCreateCallGenerator(expression, expression.descriptor)

        val receiver = expression.dispatchReceiver
        receiver?.apply {
            callGenerator.genValueAndPut(
                null, this,
                if (isSuperCall) receiver.asmType else callable.dispatchReceiverType!!,
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
            return expression.onStack
        } else if (expression.descriptor is ConstructorDescriptor) {
            return none()
        }

        return onStack(callable.returnType, returnType).coerce(expression.type)
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: BlockInfo): StackValue {
        //HACK
        StackValue.local(0, OBJECT_TYPE).put(OBJECT_TYPE, mv)
        return super.visitDelegatingConstructorCall(expression, data)
    }

    override fun visitVariable(declaration: IrVariable, data: BlockInfo): StackValue {
        val varType = typeMapper.mapType(declaration.descriptor)
        val index = frame.enter(declaration.symbol, varType)

        declaration.markLineNumber(startOffset = true)

        declaration.initializer?.let {
            StackValue.local(index, varType).store(it.accept(this, data).coerce(varType), mv)
            it.markLineNumber(startOffset = true)
        }

        data.variables.add(VariableInfo(declaration, index, varType, markNewLabel()))
        return none()
    }

    override fun visitGetValue(expression: IrGetValue, data: BlockInfo): StackValue {
        // Do not generate line number information for loads from compiler-generated
        // temporary variables. They do not correspond to variable loads in user code.
        if (expression.symbol.owner.origin != IrDeclarationOrigin.IR_TEMPORARY_VARIABLE)
            expression.markLineNumber(startOffset = true)
        return StackValue.local(findLocalIndex(expression.symbol), expression.asmType).coerce(expression.type)
    }

    private fun generateFieldValue(expression: IrFieldAccessExpression, data: BlockInfo, materialize: Boolean): StackValue {
        val receiverValue = expression.receiver?.accept(this, data) ?: StackValue.none()
        val materialReceiver = if (materialize) receiverValue.materialize() else receiverValue
        val propertyDescriptor = expression.descriptor

        val realDescriptor = DescriptorUtils.unwrapFakeOverride(propertyDescriptor)
        val fieldKotlinType = realDescriptor.original.type
        val fieldType = typeMapper.mapType(fieldKotlinType)
        val ownerType = typeMapper.mapImplementationOwner(propertyDescriptor)
        val fieldName = propertyDescriptor.name.asString()
        val isStatic = expression.receiver == null // TODO

        return StackValue.field(fieldType, fieldKotlinType, ownerType, fieldName, isStatic, materialReceiver, realDescriptor)
    }

    override fun visitGetField(expression: IrGetField, data: BlockInfo): StackValue {
        expression.markLineNumber(startOffset = true)
        return generateFieldValue(expression, data, false).coerce(expression.type)
    }

    override fun visitSetField(expression: IrSetField, data: BlockInfo): StackValue {
        val expressionValue = expression.value
        // Do not add redundant field initializers that initialize to default values.
        // "expression.origin == null" means that the field is initialized when it is declared,
        // i.e., not in an initializer block or constructor body.
        val skip = irFunction is IrConstructor && irFunction.isPrimary &&
                expression.origin == null && expressionValue is IrConst<*> &&
                isDefaultValueForType(expression.symbol.owner.type.asmType, expressionValue.value)
        if (!skip) {
            expression.markLineNumber(startOffset = true)
            generateFieldValue(expression, data, true).store(expressionValue.accept(this, data), mv)
        }
        return none().coerce(expression.type)
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
        val index = frame.getIndex(irSymbol)
        if (index >= 0) {
            return index
        }
        if (irFunction.dispatchReceiverParameter != null && (irFunction.parent as? IrClass)?.thisReceiver?.symbol == irSymbol) {
            return 0
        }
        val dump = if (irSymbol.isBound) irSymbol.owner.dump() else irSymbol.descriptor.toString()
        throw AssertionError("Non-mapped local declaration: $dump\n in ${irFunction.dump()}")
    }

    override fun visitSetVariable(expression: IrSetVariable, data: BlockInfo): StackValue {
        expression.markLineNumber(startOffset = true)
        expression.value.markLineNumber(startOffset = true)
        val value = expression.value.accept(this, data)
        StackValue.local(findLocalIndex(expression.symbol), expression.descriptor.asmType).store(value, mv)
        return none().coerce(expression.type)
    }

    override fun <T> visitConst(expression: IrConst<T>, data: BlockInfo): StackValue {
        expression.markLineNumber(startOffset = true)
        return StackValue.constant(expression.value, expression.asmType).coerce(expression.type)
    }

    override fun visitExpressionBody(body: IrExpressionBody, data: BlockInfo): StackValue {
        return body.expression.accept(this, data)
    }

    override fun visitElement(element: IrElement, data: BlockInfo): StackValue {
        throw AssertionError(
            "Unexpected IR element found during code generation. Either code generation for it " +
                    "is not implemented, or it should have been lowered: ${element.render()}"
        )
    }

    // TODO maybe remove?
    override fun visitClass(declaration: IrClass, data: BlockInfo): StackValue {
        classCodegen.generateLocalClass(declaration)
        return none()
    }

    override fun visitVararg(expression: IrVararg, data: BlockInfo): StackValue {
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
            val elementKotlinType = classCodegen.context.builtIns.getArrayElementType(outType.toKotlinType())
            for ((i, element) in expression.elements.withIndex()) {
                mv.dup()
                val array = StackValue.onStack(elementType, outType.toKotlinType())
                val index = StackValue.constant(i).materialize()
                val value = element.accept(this, data).coerce(elementType).materialize()
                StackValue.arrayElement(elementType, elementKotlinType, array, index).store(value, mv)
            }
        }
        return expression.onStack
    }

    fun newArrayInstruction(arrayType: KotlinType) {
        if (KotlinBuiltIns.isArray(arrayType)) {
            val elementJetType = arrayType.arguments[0].type
//            putReifiedOperationMarkerIfTypeIsReifiedParameter(
//                    elementJetType,
//                    ReifiedTypeInliner.OperationKind.NEW_ARRAY
//            )
            mv.newarray(boxType(elementJetType.asmType))
        } else {
            val type = typeMapper.mapType(arrayType)
            mv.newarray(correctElementType(type))
        }
    }

    override fun visitReturn(expression: IrReturn, data: BlockInfo): StackValue {
        val returnType = typeMapper.mapReturnType(irFunction.descriptor)
        val afterReturnLabel = Label()
        expression.value.accept(this, data).coerce(returnType).materialize()
        unwindBlockStack(afterReturnLabel, data, null)
        expression.markLineNumber(startOffset = true)
        mv.areturn(returnType)
        mv.mark(afterReturnLabel)
        mv.nop()/*TODO check RESTORE_STACK_IN_TRY_CATCH processor*/
        return none()
    }

    override fun visitWhen(expression: IrWhen, data: BlockInfo): StackValue {
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
                BranchedValue.condJump(branch.condition.accept(this, data), elseLabel, true, mv)
            }
            val result = branch.result.accept(this, data).coerce(expression.type).materializeNonUnit()
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
        val result = none().coerce(expression.type).materializeNonUnit()
        mv.mark(endLabel)
        return result
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: BlockInfo): StackValue {
        val asmType = expression.typeOperand.toKotlinType().asmType
        return when (expression.operator) {
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> {
                val result = expression.argument.accept(this, data)
                expression.argument.markEndOfStatementIfNeeded()
                result.coerce(expression.type)
            }

            IrTypeOperator.IMPLICIT_CAST -> {
                expression.argument.accept(this, data).coerce(expression.type)
            }

            IrTypeOperator.CAST, IrTypeOperator.SAFE_CAST -> {
                expression.argument.accept(this, data).coerce(AsmTypes.OBJECT_TYPE).materialize()
                val boxedType = boxType(asmType)
                generateAsCast(
                    mv, expression.typeOperand.toKotlinType(), boxedType, expression.operator == IrTypeOperator.SAFE_CAST,
                    state.languageVersionSettings.isReleaseCoroutines()
                )
                onStack(boxedType).coerce(expression.type)
            }

            IrTypeOperator.INSTANCEOF, IrTypeOperator.NOT_INSTANCEOF -> {
                expression.argument.accept(this, data).coerce(AsmTypes.OBJECT_TYPE).materialize()
                val type = boxType(asmType)
                generateIsCheck(mv, expression.typeOperand.toKotlinType(), type, state.languageVersionSettings.isReleaseCoroutines())
                if (IrTypeOperator.NOT_INSTANCEOF == expression.operator) {
                    return StackValue.not(expression.onStack)
                }
                expression.onStack
            }

            IrTypeOperator.IMPLICIT_NOTNULL -> {
                val value = expression.argument.accept(this, data).materialize()
                mv.dup()
                mv.visitLdcInsn("TODO provide message for IMPLICIT_NOTNULL") /*TODO*/
                mv.invokestatic(
                    "kotlin/jvm/internal/Intrinsics", "checkExpressionValueIsNotNull",
                    "(Ljava/lang/Object;Ljava/lang/String;)V", false
                )
                // Unbox primitives.
                value.coerce(expression.type)
            }

            IrTypeOperator.IMPLICIT_INTEGER_COERCION -> {
                // Force-materialize the intermediate int to ensure that it is in fact an int.
                expression.argument.accept(this, data).coerce(Type.INT_TYPE).materialize().coerce(expression.type)
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

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: BlockInfo): StackValue {
        expression.markLineNumber(startOffset = true)
        return when (expression.arguments.size) {
            0 -> StackValue.constant("", expression.asmType)
            1 -> {
                // Convert single arg to string.
                val argStackValue = expression.arguments[0].accept(this, data).materialize()
                AsmUtil.genToString(argStackValue, argStackValue.type, argStackValue.kotlinType, typeMapper).put(expression.asmType, mv)
                expression.onStack
            }
            else -> {
                // Use StringBuilder to concatenate.
                AsmUtil.genStringBuilderConstructor(mv)
                expression.arguments.forEach {
                    val stackValue = it.accept(this, data).materialize()
                    AsmUtil.genInvokeAppendMethod(mv, stackValue.type, stackValue.kotlinType)
                }
                mv.invokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
                expression.onStack
            }
        }
    }

    // Avoid true condition generation for tailrec
    // ASM and the Java verifier assume that L1 is reachable which causes several verifications to fail.
    // To avoid them, trivial jump elimination is required.
    // L0
    // ICONST_1 //could be eliminated
    // IFEQ L1 //could be eliminated
    // .... // no jumps
    // GOTO L0
    // L1
    //TODO: write elimination lower
    private fun generateLoopJump(condition: IrExpression, data: BlockInfo, label: Label, jumpIfFalse: Boolean) {
        condition.markLineNumber(true)
        if (condition is IrConst<*> && condition.value == true) {
            if (jumpIfFalse) {
                mv.fakeAlwaysFalseIfeq(label)
            } else {
                mv.fakeAlwaysTrueIfeq(label)
            }
        } else {
            BranchedValue.condJump(condition.accept(this, data), label, jumpIfFalse, mv)
        }
    }

    override fun visitWhileLoop(loop: IrWhileLoop, data: BlockInfo): StackValue {
        val continueLabel = markNewLabel()
        val endLabel = Label()
        generateLoopJump(loop.condition, data, endLabel, true)

        data.withBlock(LoopInfo(loop, continueLabel, endLabel)) {
            loop.body?.accept(this, data)?.discard()
        }
        mv.goTo(continueLabel)
        mv.mark(endLabel)

        return StackValue.none()
    }

    private fun unwindBlockStack(endLabel: Label, data: BlockInfo, loop: IrLoop? = null): LoopInfo? {
        return data.handleBlock {
            when {
                it is TryInfo -> {
                    it.gaps.add(markNewLabel() to endLabel)
                    it.onExit.accept(this, data).discard()
                }
                it is LoopInfo && it.loop == loop -> return it
            }
            unwindBlockStack(endLabel, data, loop)
        }
    }

    override fun visitBreakContinue(jump: IrBreakContinue, data: BlockInfo): StackValue {
        jump.markLineNumber(startOffset = true)
        val endLabel = Label()
        val stackElement = unwindBlockStack(endLabel, data, jump.loop)
            ?: throw AssertionError("Target label for break/continue not found")
        mv.fixStackAndJump(if (jump is IrBreak) stackElement.breakLabel else stackElement.continueLabel)
        mv.mark(endLabel)
        return none()
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: BlockInfo): StackValue {
        val entry = markNewLabel()
        val endLabel = Label()
        val continueLabel = Label()

        mv.fakeAlwaysFalseIfeq(continueLabel)
        mv.fakeAlwaysFalseIfeq(endLabel)

        data.withBlock(LoopInfo(loop, continueLabel, endLabel)) {
            loop.body?.accept(this, data)?.discard()
        }

        mv.visitLabel(continueLabel)
        generateLoopJump(loop.condition, data, entry, false)
        mv.mark(endLabel)

        return StackValue.none()
    }

    override fun visitTry(aTry: IrTry, data: BlockInfo): StackValue {
        aTry.markLineNumber(startOffset = true)
        return if (aTry.finallyExpression != null)
            data.withBlock(TryInfo(aTry.finallyExpression!!)) { visitTryWithInfo(aTry, data, it) }
        else
            visitTryWithInfo(aTry, data, null)
    }

    private fun visitTryWithInfo(aTry: IrTry, data: BlockInfo, tryInfo: TryInfo?): StackValue {
        val tryBlockStart = markNewLabel()
        mv.nop()
        val tryResult = aTry.tryResult.accept(this, data).coerce(aTry.type).materializeNonUnit()
        val tryBlockEnd = markNewLabel()
        val tryBlockGaps = tryInfo?.gaps?.toList() ?: listOf()
        val tryCatchBlockEnd = Label()
        if (tryInfo != null) {
            data.handleBlock { tryInfo.onExit.accept(this, data).discard() }
            tryInfo.onExit.markLineNumber(startOffset = false)
            mv.goTo(tryCatchBlockEnd)
            tryInfo.gaps.add(tryBlockEnd to markNewLabel())
        } else {
            mv.goTo(tryCatchBlockEnd)
        }

        val catches = aTry.catches
        for (clause in catches) {
            val clauseStart = markNewLabel()
            val descriptor = clause.parameter
            val descriptorType = descriptor.asmType
            val index = frame.enter(clause.catchParameter, descriptorType)
            mv.store(index, descriptorType)

            val catchBody = clause.result
            catchBody.markLineNumber(true)
            catchBody.accept(this, data).coerce(aTry.type).materializeNonUnit()

            frame.leave(clause.catchParameter)

            val clauseEnd = markNewLabel()

            mv.visitLocalVariable(
                descriptor.name.asString(), descriptorType.descriptor, null, clauseStart, clauseEnd,
                index
            )

            if (tryInfo != null) {
                data.handleBlock { tryInfo.onExit.accept(this, data).discard() }
                tryInfo.onExit.markLineNumber(startOffset = false)
                mv.goTo(tryCatchBlockEnd)
                tryInfo.gaps.add(clauseEnd to markNewLabel())
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
            val savedException = frame.enterTemp(AsmTypes.JAVA_THROWABLE_TYPE)
            mv.store(savedException, AsmTypes.JAVA_THROWABLE_TYPE)

            val finallyStart = markNewLabel()
            // Nothing will cover anything after this point, so don't bother recording the gap here.
            data.handleBlock { tryInfo.onExit.accept(this, data).discard() }
            mv.load(savedException, AsmTypes.JAVA_THROWABLE_TYPE)
            frame.leaveTemp(AsmTypes.JAVA_THROWABLE_TYPE)
            mv.athrow()

            // Include the ASTORE into the covered region. That's what javac does as well.
            genTryCatchCover(defaultCatchStart, tryBlockStart, finallyStart, tryInfo.gaps, null)
        }

        mv.mark(tryCatchBlockEnd)
        // TODO: generate a common `finally` for try & catch blocks here? Right now this breaks the inliner.
        return tryResult
    }

    private fun genTryCatchCover(catchStart: Label, tryStart: Label, tryEnd: Label, tryGaps: List<Pair<Label, Label>>, type: String?) {
        val lastRegionStart = tryGaps.fold(tryStart) { regionStart, (gapStart, gapEnd) ->
            mv.visitTryCatchBlock(regionStart, gapStart, catchStart, type)
            gapEnd
        }
        mv.visitTryCatchBlock(lastRegionStart, tryEnd, catchStart, type)
    }

    override fun visitThrow(expression: IrThrow, data: BlockInfo): StackValue {
        expression.markLineNumber(startOffset = true)
        expression.value.accept(this, data).coerce(AsmTypes.JAVA_THROWABLE_TYPE).materialize()
        mv.athrow()
        return none()
    }

    override fun visitGetClass(expression: IrGetClass, data: BlockInfo): StackValue {
        generateClassLiteralReference(expression, true, data)
        return expression.onStack
    }

    override fun visitClassReference(expression: IrClassReference, data: BlockInfo): StackValue {
        generateClassLiteralReference(expression, true, data)
        return expression.onStack
    }

    fun generateClassLiteralReference(
        classReference: IrExpression,
        wrapIntoKClass: Boolean,
        data: BlockInfo
    ) {
        if (classReference !is IrClassReference /* && DescriptorUtils.isObjectQualifier(classReference.descriptor)*/) {
            assert(classReference is IrGetClass)
            JavaClassProperty.generateImpl(mv, (classReference as IrGetClass).argument.accept(this, data))
        } else {
            val classType = classReference.classType
            if (classType is CrIrType) {
                putJavaLangClassInstance(mv, classType.type, null, typeMapper)
                return
            } else {
                val kotlinType = classType.toKotlinType()
                if (TypeUtils.isTypeParameter(kotlinType)) {
                    assert(TypeUtils.isReifiedTypeParameter(kotlinType)) { "Non-reified type parameter under ::class should be rejected by type checker: " + kotlinType }
                    putReifiedOperationMarkerIfTypeIsReifiedParameter(kotlinType, ReifiedTypeInliner.OperationKind.JAVA_CLASS, mv, this)
                }

                putJavaLangClassInstance(mv, typeMapper.mapType(kotlinType), kotlinType, typeMapper)
            }
        }

        if (wrapIntoKClass) {
            wrapJavaClassIntoKClass(mv)
        }

    }

    private fun resolveToCallable(irCall: IrMemberAccessExpression, isSuper: Boolean): Callable {
        var descriptor = irCall.descriptor
        if (descriptor is TypeAliasConstructorDescriptor) {
            //TODO where is best to unwrap?
            descriptor = descriptor.underlyingConstructorDescriptor
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
        isDefaultCompilation: Boolean
    ): IrCallGenerator {
        if (element == null) return IrCallGenerator.DefaultCallGenerator

        // We should inline callable containing reified type parameters even if inline is disabled
        // because they may contain something to reify and straight call will probably fail at runtime
        val isInline = (!state.isInlineDisabled || InlineUtil.containsReifiedTypeParameters(descriptor)) &&
                (InlineUtil.isInline(descriptor) || InlineUtil.isArrayConstructorWithLambda(descriptor))

        if (!isInline) return IrCallGenerator.DefaultCallGenerator

        val original = unwrapInitialSignatureDescriptor(DescriptorUtils.unwrapFakeOverride(descriptor.original as FunctionDescriptor))
        return if (isDefaultCompilation) {
            TODO()
        } else {
            IrInlineCodegen(this, state, original, typeParameterMappings!!, IrSourceCompilerForInline(state, element, this))
        }
    }

    private fun getOrCreateCallGenerator(
        memberAccessExpression: IrMemberAccessExpression,
        descriptor: CallableDescriptor
    ): IrCallGenerator {
        val typeArguments =
            if (memberAccessExpression.typeArgumentsCount == 0) {
                //avoid ambiguity with type constructor type parameters
                emptyMap()
            } else descriptor.original.typeParameters.keysToMap {
                memberAccessExpression.getTypeArgumentOrDefault(it)
            }

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

        return getOrCreateCallGenerator(descriptor, memberAccessExpression, mappings, false)
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
