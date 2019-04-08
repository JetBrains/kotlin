/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.common.descriptors.propertyIfAccessor
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.intrinsics.ComparisonIntrinsic
import org.jetbrains.kotlin.backend.jvm.intrinsics.IrIntrinsicFunction
import org.jetbrains.kotlin.backend.jvm.intrinsics.IrIntrinsicMethods
import org.jetbrains.kotlin.backend.jvm.lower.CrIrType
import org.jetbrains.kotlin.backend.jvm.lower.JvmBuiltinOptimizationLowering.Companion.isNegation
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.AsmUtil.*
import org.jetbrains.kotlin.codegen.ExpressionCodegen.putReifiedOperationMarkerIfTypeIsReifiedParameter
import org.jetbrains.kotlin.codegen.StackValue.*
import org.jetbrains.kotlin.codegen.inline.*
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner.OperationKind.AS
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner.OperationKind.SAFE_AS
import org.jetbrains.kotlin.codegen.intrinsics.JavaClassProperty
import org.jetbrains.kotlin.codegen.pseudoInsns.fakeAlwaysFalseIfeq
import org.jetbrains.kotlin.codegen.pseudoInsns.fakeAlwaysTrueIfeq
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
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils.isEnumClass
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.JAVA_THROWABLE_TYPE
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

open class ExpressionInfo(val expression: IrExpression)

class LoopInfo(val loop: IrLoop, val continueLabel: Label, val breakLabel: Label) : ExpressionInfo(loop)

class TryInfo(val tryBlock: IrTry) : ExpressionInfo(tryBlock) {
    val gaps = mutableListOf<Label>()
}

class BlockInfo private constructor(val parent: BlockInfo?) {
    val variables = mutableListOf<VariableInfo>()
    val infos = Stack<ExpressionInfo>()

    fun create() = BlockInfo(this).apply {
        this@apply.infos.addAll(this@BlockInfo.infos)
    }

    fun hasFinallyBlocks(): Boolean = infos.firstIsInstanceOrNull<TryInfo>() != null

    inline fun <R> withBlock(info: ExpressionInfo, f: (ExpressionInfo) -> R): R {
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

@Suppress("IMPLICIT_CAST_TO_ANY")
class ExpressionCodegen(
    val irFunction: IrFunction,
    val frame: IrFrameMap,
    val mv: InstructionAdapter,
    val classCodegen: ClassCodegen,
    val isInlineLambda: Boolean = false
) : IrElementVisitor<StackValue, BlockInfo>, BaseExpressionCodegen {

    var finallyDepth = 0

    val typeMapper = classCodegen.typeMapper

    val returnType = typeMapper.mapReturnType(irFunction.descriptor)

    val state = classCodegen.state

    private val sourceManager = classCodegen.context.psiSourceManager

    private val fileEntry = sourceManager.getFileEntry(irFunction.fileParent)

    private val KotlinType.asmType: Type
        get() = typeMapper.mapType(this)

    private val IrType.asmType: Type
        get() = toKotlinType().asmType

    private val CallableDescriptor.asmType: Type
        get() = typeMapper.mapType(this)

    private val IrExpression.asmType: Type
        get() = type.asmType

    val IrExpression.onStack: StackValue
        get() = StackValue.onStack(asmType, type.toKotlinType())

    private fun StackValue.discard(): StackValue {
        coerce(type, Type.VOID_TYPE, mv)
        return none()
    }

    private fun StackValue.coerce(toKotlinType: KotlinType): StackValue {
        coerce(type, kotlinType, toKotlinType.asmType, toKotlinType, mv)
        return onStack(toKotlinType.asmType, toKotlinType)
    }

    internal fun coerceNotToUnit(fromType: Type, fromKotlinType: KotlinType?, toKotlinType: KotlinType): StackValue {
        // A void should still be materialized as a Unit to avoid stack depth mismatches.
        if (toKotlinType.isUnit() && fromType != Type.VOID_TYPE)
            return onStack(fromType, fromKotlinType)
        return onStack(fromType, fromKotlinType).coerce(toKotlinType)
    }

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

    fun generate() {
        mv.visitCode()
        val startLabel = markNewLabel()
        val info = BlockInfo.create()
        val body = irFunction.body!!
        val result = gen(body, info)
        // If this function has an expression body, return the result of that expression.
        // Otherwise, if it does not end in a return statement, it must be void-returning,
        // and an explicit return instruction at the end is still required to pass validation.
        if (body !is IrStatementContainer || body.statements.lastOrNull() !is IrReturn) {
            val returnType = typeMapper.mapReturnType(irFunction.descriptor)
            // Allow setting a breakpoint on the closing brace of a void-returning function
            // without an explicit return, or the `class Something(` line of a primary constructor.
            if (irFunction.origin != JvmLoweredDeclarationOrigin.CLASS_STATIC_INITIALIZER) {
                irFunction.markLineNumber(startOffset = irFunction is IrConstructor && irFunction.isPrimary)
            }
            coerce(result.type, returnType, mv)
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

    private fun visitStatementContainer(container: IrStatementContainer, data: BlockInfo): StackValue {
        return container.statements.fold(none()) { prev, exp ->
            prev.discard()
            gen(exp, data).also {
                (exp as? IrExpression)?.markEndOfStatementIfNeeded()
            }
        }
    }

    override fun visitBlockBody(body: IrBlockBody, data: BlockInfo): StackValue {
        return visitStatementContainer(body, data).discard()
    }

    override fun visitContainerExpression(expression: IrContainerExpression, data: BlockInfo): StackValue {
        val result = visitStatementContainer(expression, data)
        return coerceNotToUnit(result.type, result.kotlinType, expression.type.toKotlinType())
    }

    override fun visitMemberAccess(expression: IrMemberAccessExpression, data: BlockInfo): StackValue {
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
            //noinspection ConstantConditions
            return generateNewArray(expression, data)
        }

        mv.anew(expression.asmType)
        mv.dup()
        generateCall(expression, expression.superQualifier, data)
        return expression.onStack
    }

    fun generateNewArray(
        expression: IrCall, data: BlockInfo
    ): StackValue {
        val args = expression.descriptor.valueParameters
        assert(args.size == 1 || args.size == 2) { "Unknown constructor called: " + args.size + " arguments" }

        if (args.size == 1) {
            val sizeExpression = expression.getValueArgument(0)!!
            gen(sizeExpression, Type.INT_TYPE, data)
            newArrayInstruction(expression.type.toKotlinType())
            return expression.onStack
        }

        return generateCall(expression, expression.superQualifier, data)
    }

    private fun generateCall(expression: IrMemberAccessExpression, superQualifier: ClassDescriptor?, data: BlockInfo): StackValue {
        val isSuperCall = superQualifier != null
        val callable = resolveToCallable(expression, isSuperCall)
        return if (callable is IrIntrinsicFunction) {
            callable.invoke(mv, this, data)
        } else {
            generateCall(expression, callable, data, isSuperCall)
        }
    }

    fun generateCall(expression: IrMemberAccessExpression, callable: Callable, data: BlockInfo, isSuperCall: Boolean = false): StackValue {
        val callGenerator = getOrCreateCallGenerator(expression, expression.descriptor, data)

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
        if (returnType != null && KotlinBuiltIns.isNothing(returnType)) {
            mv.aconst(null)
            mv.athrow()
            return expression.onStack
        } else if (expression.descriptor is ConstructorDescriptor) {
            return none()
        }

        return coerceNotToUnit(callable.returnType, returnType, expression.type.toKotlinType())
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall, data: BlockInfo): StackValue {
        throw AssertionError("Instruction should've been lowered before code generation: ${expression.render()}")
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

        declaration.initializer?.apply {
            StackValue.local(index, varType).store(gen(this, varType, data), mv)
            this.markLineNumber(startOffset = true)
        }

        val info = VariableInfo(
            declaration,
            index,
            varType,
            markNewLabel()
        )
        data.variables.add(info)

        return none()
    }

    fun gen(expression: IrElement, type: Type, data: BlockInfo): StackValue {
        gen(expression, data).put(type, mv)
        return onStack(type)
    }

    fun gen(expression: IrElement, data: BlockInfo): StackValue {
        return expression.accept(this, data)
    }

    override fun visitGetValue(expression: IrGetValue, data: BlockInfo): StackValue {
        // Do not generate line number information for loads from compiler-generated
        // temporary variables. They do not correspond to variable loads in user code.
        if (expression.symbol.owner.origin != IrDeclarationOrigin.IR_TEMPORARY_VARIABLE)
            expression.markLineNumber(startOffset = true)
        return generateLocal(expression.symbol, expression.asmType)
    }

    private fun generateFieldValue(expression: IrFieldAccessExpression, data: BlockInfo): StackValue {
        val receiverValue = expression.receiver?.accept(this, data) ?: StackValue.none()
        val propertyDescriptor = expression.descriptor

        val realDescriptor = DescriptorUtils.unwrapFakeOverride(propertyDescriptor)
        val fieldKotlinType = realDescriptor.original.type
        val fieldType = typeMapper.mapType(fieldKotlinType)
        val ownerType = typeMapper.mapImplementationOwner(propertyDescriptor)
        val fieldName = propertyDescriptor.name.asString()
        val isStatic = expression.receiver == null // TODO

        return StackValue.field(fieldType, fieldKotlinType, ownerType, fieldName, isStatic, receiverValue, realDescriptor)
    }

    override fun visitGetField(expression: IrGetField, data: BlockInfo): StackValue {
        expression.markLineNumber(startOffset = true)
        val value = generateFieldValue(expression, data)
        value.put(mv)
        return onStack(value.type)
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
            val fieldValue = generateFieldValue(expression, data)
            fieldValue.store(expressionValue.accept(this, data), mv)
        }
        // Assignments can be used as expressions, so return a value. Redundant pushes
        // will be eliminated by the peephole optimizer.
        putUnitInstance(mv)
        return onStack(AsmTypes.UNIT_TYPE)
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

    private fun generateLocal(symbol: IrSymbol, type: Type): StackValue {
        val index = findLocalIndex(symbol)
        StackValue.local(index, type).put(mv)
        return onStack(type)
    }

    private fun findLocalIndex(irSymbol: IrSymbol): Int {
        return frame.getIndex(irSymbol).apply {
            if (this < 0) {
                if (irFunction.dispatchReceiverParameter != null) {
                    (irFunction.parent as? IrClass)?.takeIf { it.thisReceiver?.symbol == irSymbol }?.let { return 0 }
                }
                throw AssertionError(
                    "Non-mapped local declaration: " +
                            "${if (irSymbol.isBound) irSymbol.owner.dump() else irSymbol.descriptor} \n in ${irFunction.dump()}"
                )
            }
        }
    }

    override fun visitSetVariable(expression: IrSetVariable, data: BlockInfo): StackValue {
        expression.markLineNumber(startOffset = true)
        expression.value.markLineNumber(startOffset = true)
        val value = expression.value.accept(this, data)
        StackValue.local(findLocalIndex(expression.symbol), expression.descriptor.asmType).store(value, mv)
        // Assignments can be used as expressions, so return a value. Redundant pushes
        // will be eliminated by the peephole optimizer.
        putUnitInstance(mv)
        return onStack(AsmTypes.UNIT_TYPE)
    }

    override fun <T> visitConst(expression: IrConst<T>, data: BlockInfo): StackValue {
        expression.markLineNumber(startOffset = true)
        val value = expression.value
        val type = expression.asmType
        StackValue.constant(value, type).put(mv)
        return expression.onStack
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
                gen(argument, type, data)
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
                        gen(argument.expression, OBJECT_TYPE, data)
                        mv.invokevirtual(owner, "addSpread", "(Ljava/lang/Object;)V", false)
                    } else {
                        gen(argument, elementType, data)
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
                StackValue.constant(i).put(mv)
                val rightSide = gen(element, elementType, data)
                StackValue
                    .arrayElement(
                        elementType,
                        elementKotlinType,
                        StackValue.onStack(elementType, outType.toKotlinType()),
                        StackValue.onStack(Type.INT_TYPE)
                    )
                    .store(rightSide, mv)
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

    override fun visitReturn(expression: IrReturn, data: BlockInfo): StackValue {
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
            return none()
        }

        val actualReturn =
            if (isNonLocalReturn) {
                typeMapper.mapReturnType(owner.descriptor)
            } else {
                returnType
            }

        expression.value.apply {
            gen(this, actualReturn, data)
        }

        val afterReturnLabel = Label()
        generateFinallyBlocksIfNeeded(actualReturn, afterReturnLabel, data)

        expression.markLineNumber(startOffset = true)
        if (isNonLocalReturn) {
            val nonLocalReturnType = typeMapper.mapReturnType(owner.descriptor)
            val labelName = (owner as IrFunction).name.asString()
            generateGlobalReturnFlag(mv, labelName)
            mv.areturn(nonLocalReturnType)
        } else {
            mv.areturn(actualReturn)
        }
        mv.mark(afterReturnLabel)
        mv.nop()/*TODO check RESTORE_STACK_IN_TRY_CATCH processor*/
        return expression.onStack
    }

    override fun visitWhen(expression: IrWhen, data: BlockInfo): StackValue {
        expression.markLineNumber(startOffset = true)
        SwitchGenerator(expression, data, this).generate()?.let { return it }

        val type = expression.type.toKotlinType()
        val endLabel = Label()
        var exhaustive = false
        for (branch in expression.branches) {
            val elseLabel = Label()
            if (branch.condition.isFalseConst() || branch.condition.isTrueConst()) {
                // True or false conditions known at compile time need not be generated. A linenumber and nop are still required
                // for a debugger to break on the line of the condition.
                if (branch !is IrElseBranch) {
                    branch.condition.markLineNumber(startOffset = true)
                    mv.nop()
                }
                if (branch.condition.isFalseConst())
                    continue // The branch body is dead code.
            } else {
                genConditionalJumpWithOptimizationsIfPossible(branch.condition, data, elseLabel)
            }
            gen(branch.result, data).let {
                coerceNotToUnit(it.type, it.kotlinType, type)
            }
            if (branch.condition.isTrueConst()) {
                exhaustive = true
                break // The rest of the expression is dead code.
            }
            mv.goTo(endLabel)
            mv.mark(elseLabel)
        }

        if (!exhaustive) {
            // TODO: make all non-exhaustive `if`/`when` return Nothing.
            if (type.isUnit())
                putUnitInstance(mv)
            else if (!type.isNothing())
                throw AssertionError("non-exhaustive `if`/`when` wants to return $type")
        }

        mv.mark(endLabel)
        return if (type.isNothing()) none() else expression.onStack
    }

    private fun genConditionalJumpWithOptimizationsIfPossible(
        originalCondition: IrExpression,
        data: BlockInfo,
        jumpToLabel: Label,
        originalJumpIfFalse: Boolean = true
    ) {
        var condition = originalCondition
        var jumpIfFalse = originalJumpIfFalse

        // Instead of materializing a negated value when used for control flow, flip the branch
        // targets instead. This significantly cuts down the amount of branches and loads of
        // const_0 and const_1 in the generated java bytecode.
        if (isNegation(condition, classCodegen.context)) {
            condition = (condition as IrCall).dispatchReceiver!!
            jumpIfFalse = !jumpIfFalse
        }

        // Do not materialize null constants to check for null. Instead use the JVM bytecode
        // ifnull and ifnonnull instructions.
        if (isNullCheck(condition)) {
            val call = condition as IrCall
            val left = call.getValueArgument(0)!!
            val right = call.getValueArgument(1)!!
            val other = if (left.isNullConst()) right else left
            gen(other, data).put(other.asmType, mv)
            if (jumpIfFalse) {
                mv.ifnonnull(jumpToLabel)
            } else {
                mv.ifnull(jumpToLabel)
            }
            return
        }

        // For comparison intrinsics, branch directly based on the comparison instead of
        // materializing a boolean and performing and extra jump.
        if (condition is IrCall) {
            val intrinsic = classCodegen.context.irIntrinsics.getIntrinsic(condition.descriptor.original as CallableMemberDescriptor)
            if (intrinsic is ComparisonIntrinsic) {
                val callable = resolveToCallable(condition, false)
                (callable as IrIntrinsicFunction).loadArguments(this, data)
                val stackValue = intrinsic.genStackValue(condition, classCodegen.context)
                BranchedValue.condJump(stackValue, jumpToLabel, jumpIfFalse, mv)
                return
            }
        }

        // For instance of type operators, branch directly on the instanceof result instead
        // of materializing a boolean and performing an extra jump.
        if (condition is IrTypeOperatorCall &&
            (condition.operator == IrTypeOperator.NOT_INSTANCEOF || condition.operator == IrTypeOperator.INSTANCEOF)
        ) {
            val asmType = condition.typeOperand.toKotlinType().asmType
            gen(condition.argument, OBJECT_TYPE, data)
            val type = boxType(asmType)
            generateIsCheck(mv, condition.typeOperand.toKotlinType(), type, state.languageVersionSettings.isReleaseCoroutines())
            val stackValue =
                if (condition.operator == IrTypeOperator.INSTANCEOF)
                    onStack(Type.BOOLEAN_TYPE)
                else
                    StackValue.not(onStack(Type.BOOLEAN_TYPE))
            BranchedValue.condJump(stackValue, jumpToLabel, jumpIfFalse, mv)
            return
        }

        gen(condition, data).put(condition.asmType, mv)
        BranchedValue.condJump(onStack(condition.asmType), jumpToLabel, jumpIfFalse, mv)
    }

    private fun isNullCheck(expression: IrExpression): Boolean {
        return expression is IrCall
                && expression.symbol == classCodegen.context.irBuiltIns.eqeqSymbol
                && (expression.getValueArgument(0)!!.isNullConst() || expression.getValueArgument(1)!!.isNullConst())
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: BlockInfo): StackValue {
        val kotlinType = expression.typeOperand.toKotlinType()
        val asmType = kotlinType.asmType
        when (expression.operator) {
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> {
                val result = expression.argument.accept(this, data)
                expression.argument.markEndOfStatementIfNeeded()
                return result.discard()
            }

            IrTypeOperator.IMPLICIT_CAST -> {
                gen(expression.argument, asmType, data)
            }

            IrTypeOperator.CAST, IrTypeOperator.SAFE_CAST -> {
                val value = expression.argument.accept(this, data)
                value.put(boxType(value.type), mv)
                if (value.type === Type.VOID_TYPE) {
                    StackValue.putUnitInstance(mv)
                }
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
                return onStack(boxedType)
            }

            IrTypeOperator.INSTANCEOF, IrTypeOperator.NOT_INSTANCEOF -> {
                gen(expression.argument, OBJECT_TYPE, data)
                val type = boxType(asmType)
                if (TypeUtils.isReifiedTypeParameter(kotlinType)) {
                    putReifiedOperationMarkerIfTypeIsReifiedParameter(kotlinType, ReifiedTypeInliner.OperationKind.IS, mv, this)
                    v.instanceOf(type)
                } else {
                    generateIsCheck(mv, kotlinType, type, state.languageVersionSettings.isReleaseCoroutines())
                }
                if (IrTypeOperator.NOT_INSTANCEOF == expression.operator) {
                    StackValue.not(StackValue.onStack(Type.BOOLEAN_TYPE)).put(mv)
                }
            }

            IrTypeOperator.IMPLICIT_NOTNULL -> {
                val value = gen(expression.argument, data)
                mv.dup()
                mv.visitLdcInsn("TODO provide message for IMPLICIT_NOTNULL") /*TODO*/
                mv.invokestatic(
                    "kotlin/jvm/internal/Intrinsics", "checkExpressionValueIsNotNull",
                    "(Ljava/lang/Object;Ljava/lang/String;)V", false
                )
                StackValue.onStack(value.type).put(asmType, mv)
            }

            IrTypeOperator.IMPLICIT_INTEGER_COERCION -> {
                gen(expression.argument, Type.INT_TYPE, data)
                StackValue.coerce(Type.INT_TYPE, typeMapper.mapType(expression.type.toKotlinType()), mv)
            }
        }
        return expression.onStack
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
                val arg = expression.arguments[0]
                val argStackValue = gen(arg, arg.asmType, data)
                AsmUtil.genToString(argStackValue, argStackValue.type, argStackValue.kotlinType, typeMapper).put(expression.asmType, mv)
                expression.onStack
            }
            else -> {
                // Use StringBuilder to concatenate.
                AsmUtil.genStringBuilderConstructor(mv)
                expression.arguments.forEach {
                    val stackValue = gen(it, it.asmType, data)
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
            genConditionalJumpWithOptimizationsIfPossible(condition, data, label, jumpIfFalse)
        }
    }

    override fun visitWhileLoop(loop: IrWhileLoop, data: BlockInfo): StackValue {
        val continueLabel = markNewLabel()
        val endLabel = Label()
        generateLoopJump(loop.condition, data, endLabel, true)

        data.withBlock(LoopInfo(loop, continueLabel, endLabel)) {
            loop.body?.let { gen(it, data).discard() }
        }
        mv.goTo(continueLabel)
        mv.mark(endLabel)

        return StackValue.none()
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
            loop.body?.let { gen(it, data).discard() }
        }

        mv.visitLabel(continueLabel)
        generateLoopJump(loop.condition, data, entry, false)
        mv.mark(endLabel)

        return StackValue.none()
    }

    override fun visitTry(aTry: IrTry, data: BlockInfo): StackValue {
        aTry.markLineNumber(startOffset = true)
        return if (aTry.finallyExpression != null)
            data.withBlock(TryInfo(aTry)) { visitTryWithInfo(aTry, data, it as TryInfo) }
        else
            visitTryWithInfo(aTry, data, null)
    }

    private fun visitTryWithInfo(aTry: IrTry, data: BlockInfo, tryInfo: TryInfo?): StackValue {
        val tryBlockStart = markNewLabel()
        mv.nop()
        gen(aTry.tryResult, aTry.asmType, data)

        val isExpression = true //TODO: more wise check is required
        var savedValue: Local? = null
        if (isExpression) {
            savedValue = local(frame.enterTemp(aTry.asmType), aTry.asmType)
            savedValue.store(onStack(aTry.asmType), mv)
        }

        val tryBlockEnd = markNewLabel()

        val tryRegions = getCurrentTryIntervals(tryInfo, tryBlockStart, tryBlockEnd)

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
            val index = frame.enter(clause.catchParameter, descriptorType)
            mv.store(index, descriptorType)

            val catchBody = clause.result
            catchBody.markLineNumber(true)
            gen(catchBody, catchBody.asmType, data)

            savedValue?.let {
                savedValue.store(onStack(aTry.asmType), mv)
            }

            frame.leave(clause.catchParameter)

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

            generateExceptionTable(clauseStart, tryRegions, descriptorType.internalName)
        }

        //for default catch clause
        if (tryInfo != null) {
            val defaultCatchStart = markNewLabel()
            val savedException = frame.enterTemp(JAVA_THROWABLE_TYPE)
            mv.store(savedException, JAVA_THROWABLE_TYPE)
            val defaultCatchEnd = markNewLabel()

            //do it before finally block generation
            //javac also generates entry in exception table for default catch clause too!!!! so defaultCatchEnd as end parameter
            val defaultCatchRegions = getCurrentTryIntervals(tryInfo, tryBlockStart, defaultCatchEnd)

            data.handleBlock { genFinallyBlock(tryInfo, null, null, data) }

            mv.load(savedException, JAVA_THROWABLE_TYPE)
            frame.leaveTemp(JAVA_THROWABLE_TYPE)

            mv.athrow()

            generateExceptionTable(defaultCatchStart, defaultCatchRegions, null)
        }

        mv.mark(tryCatchBlockEnd)

        savedValue?.let {
            savedValue.put(mv)
            frame.leaveTemp(aTry.asmType)
        }
        return aTry.onStack
    }

    private fun getCurrentTryIntervals(
        finallyBlockStackElement: TryInfo?,
        blockStart: Label,
        blockEnd: Label
    ): List<Label> {
        val gapsInBlock = if (finallyBlockStackElement != null) ArrayList<Label>(finallyBlockStackElement.gaps) else emptyList<Label>()
        assert(gapsInBlock.size % 2 == 0)
        val blockRegions = ArrayList<Label>(gapsInBlock.size + 2)
        blockRegions.add(blockStart)
        blockRegions.addAll(gapsInBlock)
        blockRegions.add(blockEnd)
        return blockRegions
    }

    private fun generateExceptionTable(catchStart: Label, catchedRegions: List<Label>, exception: String?) {
        var i = 0
        while (i < catchedRegions.size) {
            val startRegion = catchedRegions[i]
            val endRegion = catchedRegions[i + 1]
            mv.visitTryCatchBlock(startRegion, endRegion, catchStart, exception)
            i += 2
        }
    }

    private fun genFinallyBlock(tryInfo: TryInfo, tryCatchBlockEnd: Label?, afterJumpLabel: Label?, data: BlockInfo) {
        assert(tryInfo.gaps.size % 2 == 0) { "Finally block gaps are inconsistent" }
        tryInfo.gaps.add(markNewLabel())
        finallyDepth++
        if (isFinallyMarkerRequired()) {
            generateFinallyMarker(mv, finallyDepth, true)
        }
        gen(tryInfo.tryBlock.finallyExpression!!, data).discard()

        if (isFinallyMarkerRequired()) {
            generateFinallyMarker(mv, finallyDepth, false)
        }
        finallyDepth--

        if (tryCatchBlockEnd != null) {
            tryInfo.tryBlock.finallyExpression!!.markLineNumber(startOffset = false)
            mv.goTo(tryCatchBlockEnd)
        }
        tryInfo.gaps.add(afterJumpLabel ?: markNewLabel())
    }

    fun generateFinallyBlocksIfNeeded(returnType: Type, afterReturnLabel: Label, data: BlockInfo) {
        if (data.hasFinallyBlocks()) {
            if (Type.VOID_TYPE != returnType) {
                val returnValIndex = frame.enterTemp(returnType)
                val localForReturnValue = StackValue.local(returnValIndex, returnType)
                localForReturnValue.store(StackValue.onStack(returnType), mv)
                unwindBlockStack(afterReturnLabel, data, null)
                localForReturnValue.put(returnType, mv)
                frame.leaveTemp(returnType)
            } else {
                unwindBlockStack(afterReturnLabel, data, null)
            }
        }
    }

    override fun visitThrow(expression: IrThrow, data: BlockInfo): StackValue {
        expression.markLineNumber(startOffset = true)
        gen(expression.value, JAVA_THROWABLE_TYPE, data)
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
            JavaClassProperty.generateImpl(mv, gen((classReference as IrGetClass).argument, data))
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
        val intrinsic = classCodegen.context.irIntrinsics.getIntrinsic(irCall.descriptor.original as CallableMemberDescriptor)
        if (intrinsic != null) {
            return intrinsic.toCallable(
                irCall,
                typeMapper.mapSignatureSkipGeneric(irCall.descriptor as FunctionDescriptor),
                classCodegen.context
            )
        }

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

    internal fun getOrCreateCallGenerator(
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

    override val frameMap: IrFrameMap
        get() = frame
    override val visitor: InstructionAdapter
        get() = mv
    override val inlineNameGenerator: NameGenerator = NameGenerator("${classCodegen.type.internalName}\$todo")

    override var lastLineNumber: Int = -1

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
