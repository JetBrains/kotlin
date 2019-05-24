/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.intrinsics.IrIntrinsicMethods
import org.jetbrains.kotlin.backend.jvm.intrinsics.JavaClassProperty
import org.jetbrains.kotlin.backend.jvm.intrinsics.Not
import org.jetbrains.kotlin.backend.jvm.lower.CrIrType
import org.jetbrains.kotlin.backend.jvm.lower.constantValue
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.AsmUtil.*
import org.jetbrains.kotlin.codegen.inline.*
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner.OperationKind.AS
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner.OperationKind.SAFE_AS
import org.jetbrains.kotlin.codegen.pseudoInsns.fakeAlwaysFalseIfeq
import org.jetbrains.kotlin.codegen.pseudoInsns.fixStackAndJump
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter
import org.jetbrains.kotlin.config.isReleaseCoroutines
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE
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

    val typeMapper = classCodegen.typeMapper

    val context = classCodegen.context

    private val state = classCodegen.state

    private val fileEntry = classCodegen.context.psiSourceManager.getFileEntry(irFunction.fileParent)

    override val visitor: InstructionAdapter
        get() = mv

    override val inlineNameGenerator: NameGenerator = NameGenerator("${classCodegen.type.internalName}\$todo") // TODO

    override var lastLineNumber: Int = -1

    private val IrType.asmType: Type
        get() = typeMapper.mapType(this)

    val IrExpression.asmType: Type
        get() = type.asmType

    val IrVariable.asmType: Type
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
            val returnType = typeMapper.mapReturnType(irFunction)
            result.coerce(returnType).materialize()
            mv.areturn(returnType)
        }
        val endLabel = markNewLabel()
        writeLocalVariablesInTable(info, endLabel)
        writeParameterInLocalVariableTable(startLabel, endLabel)
    }

    private fun generateNonNullAssertions() {
        val isSyntheticOrBridge = irFunction.origin.isSynthetic ||
                // Although these are accessible from Java, the functions they bridge to already have the assertions.
                irFunction.origin == IrDeclarationOrigin.BRIDGE_SPECIAL ||
                irFunction.origin == JvmLoweredDeclarationOrigin.DEFAULT_IMPLS_BRIDGE ||
                irFunction.origin == JvmLoweredDeclarationOrigin.JVM_STATIC_WRAPPER
        if (!isInlineLambda && !isSyntheticOrBridge && !Visibilities.isPrivate(irFunction.visibility)) {
            irFunction.extensionReceiverParameter?.let { generateNonNullAssertion(it) }
            irFunction.valueParameters.forEach(::generateNonNullAssertion)
        }
    }

    private fun generateNonNullAssertion(param: IrValueParameter) {
        val asmType = param.type.asmType
        if (!param.type.isNullable() && !isPrimitive(asmType)) {
            mv.load(findLocalIndex(param.symbol), asmType)
            mv.aconst(param.name.asString())
            mv.invokestatic("kotlin/jvm/internal/Intrinsics", "checkParameterIsNotNull", "(Ljava/lang/Object;Ljava/lang/String;)V", false)
        }
    }

    private fun writeParameterInLocalVariableTable(startLabel: Label, endLabel: Label) {
        if (!irFunction.isStatic) {
            mv.visitLocalVariable("this", classCodegen.type.descriptor, null, startLabel, endLabel, 0)
        }
        val extensionReceiverParameter = irFunction.extensionReceiverParameter
        if (extensionReceiverParameter != null) {
            writeValueParameterInLocalVariableTable(extensionReceiverParameter, startLabel, endLabel)
        }
        for (param in irFunction.valueParameters) {
            writeValueParameterInLocalVariableTable(param, startLabel, endLabel)
        }
    }

    private fun writeValueParameterInLocalVariableTable(param: IrValueParameter, startLabel: Label, endLabel: Label) {
        // TODO: old code has a special treatment for destructuring lambda parameters.
        // There is no (easy) way to reproduce it with IR structures.
        // Does not show up in tests, but might come to bite us at some point.
        val name = param.name.asString()

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
        container.statements.fold(voidValue as PromisedValue) { prev, exp ->
            prev.discard()
            exp.accept(this, data).also { (exp as? IrExpression)?.markEndOfStatementIfNeeded() }
        }

    override fun visitBlockBody(body: IrBlockBody, data: BlockInfo) =
        visitStatementContainer(body, data).discard()

    override fun visitContainerExpression(expression: IrContainerExpression, data: BlockInfo) =
        visitStatementContainer(expression, data).coerce(expression.asmType)

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: BlockInfo): PromisedValue {
        classCodegen.context.irIntrinsics.getIntrinsic(expression.symbol)
            ?.invoke(expression, this, data)?.let { return it.coerce(expression.asmType) }

        val isSuperCall = (expression as? IrCall)?.superQualifier != null
        val callable = resolveToCallable(expression, isSuperCall)
        val callee = expression.symbol.owner
        val callGenerator = getOrCreateCallGenerator(expression, data)

        when {
            expression is IrConstructorCall -> {
                // IR constructors have no receiver and return the new instance, but on JVM they are void-returning
                // instance methods named <init>.
                mv.anew(expression.asmType)
                mv.dup()
            }
            expression is IrDelegatingConstructorCall ->
                // In this case the receiver is `this` (not specified in IR) and the return value is discarded anyway.
                mv.load(0, OBJECT_TYPE)
            expression.descriptor is ConstructorDescriptor ->
                throw AssertionError("IrCall with ConstructorDescriptor: ${expression.javaClass.simpleName}")
        }

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
        val extraArgsShift =
            when {
                callee is IrConstructor && callee.parentAsClass.isEnumClass -> 2
                callee is IrConstructor && callee.parentAsClass.isInner -> 1 // skip the `$outer` parameter
                else -> 0
            }
        val defaultMask = DefaultCallArgs(callable.valueParameterTypes.size - extraArgsShift)
        val typeParameters = if (callee is IrConstructor)
            callee.parentAsClass.typeParameters + callee.typeParameters
        else
            callee.typeParameters
        val typeArguments = (0 until typeParameters.size).map { expression.getTypeArgument(it)!! }
        val typeSubstitutionMap = typeParameters.map { it.symbol }.zip(typeArguments).toMap()
        expression.symbol.owner.valueParameters.forEachIndexed { i, irParameter ->
            val arg = expression.getValueArgument(i)
            val parameterType = callable.valueParameterTypes[i]
            when {
                arg != null -> {
                    callGenerator.genValueAndPut(irParameter, arg, parameterType, i, this@ExpressionCodegen, data)
                }
                irParameter.hasDefaultValue() -> {
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
                    assert(irParameter.varargElementType != null)
                    val type = typeMapper.mapType(
                        irParameter.type.substitute(typeSubstitutionMap)
                    )
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

        expression.markLineNumber(true)
        callGenerator.genCall(
            callable,
            defaultMask.generateOnStackIfNeeded(callGenerator, callee is IrConstructor, this),
            this,
            expression
        )

        val returnType = callee.returnType.substitute(typeSubstitutionMap)
        return when {
            returnType.isNothing() -> {
                mv.aconst(null)
                mv.athrow()
                voidValue
            }
            expression is IrConstructorCall -> expression.onStack
            expression is IrDelegatingConstructorCall -> voidValue
            expression.type.isUnit() ->
                // NewInference allows casting `() -> T` to `() -> Unit`. A CHECKCAST here will fail.
                MaterialValue(mv, callable.returnType).discard().coerce(expression.asmType)
            else -> MaterialValue(mv, callable.returnType).coerce(expression.asmType)
        }
    }

    override fun visitVariable(declaration: IrVariable, data: BlockInfo): PromisedValue {
        val varType = typeMapper.mapType(declaration)
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

        val realField = expression.symbol.owner.resolveFakeOverride()!!
        val fieldType = typeMapper.mapType(realField.type)
        val ownerType = typeMapper.mapImplementationOwner(expression.symbol.owner).internalName
        val fieldName = realField.name.asString()
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
        if (index >= 0)
            return index
        val dump = if (irSymbol.isBound) irSymbol.owner.dump() else irSymbol.descriptor.toString()
        throw AssertionError("Non-mapped local declaration: $dump\n in ${irFunction.dump()}")
    }

    override fun visitSetVariable(expression: IrSetVariable, data: BlockInfo): PromisedValue {
        expression.markLineNumber(startOffset = true)
        expression.value.markLineNumber(startOffset = true)
        expression.value.accept(this, data).coerce(expression.symbol.owner.asmType).materialize()
        mv.store(findLocalIndex(expression.symbol), expression.symbol.owner.asmType)
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
            val arrayOfReferences = outType.makeNotNull().isArray()
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
                    newArrayInstruction(outType)
                    mv.invokevirtual(owner, "toArray", toArrayDescriptor, false)
                    mv.checkcast(type)
                } else {
                    mv.invokevirtual(owner, "toArray", toArrayDescriptor, false)
                }
            }
        } else {
            mv.iconst(size)
            newArrayInstruction(expression.type)
            for ((i, element) in expression.elements.withIndex()) {
                mv.dup()
                mv.iconst(i)
                element.accept(this, data).coerce(elementType).materialize()
                mv.astore(elementType)
            }
        }
        return expression.onStack
    }

    fun newArrayInstruction(arrayType: IrType) {
        if (arrayType.isArray()) {
            val elementIrType = arrayType.safeAs<IrSimpleType>()!!.arguments[0].safeAs<IrTypeProjection>()!!.type
            putReifiedOperationMarkerIfTypeIsReifiedParameter(
                elementIrType,
                ReifiedTypeInliner.OperationKind.NEW_ARRAY,
                mv,
                this
            )
            mv.newarray(boxType(elementIrType.asmType))
        } else {
            val type = typeMapper.mapType(arrayType)
            mv.newarray(correctElementType(type))
        }
    }

    override fun visitReturn(expression: IrReturn, data: BlockInfo): PromisedValue {
        val owner = expression.returnTargetSymbol.owner
        //TODO: should be owner != irFunction
        val isNonLocalReturn =
            typeMapper.mapFunctionName(owner, OwnerKind.IMPLEMENTATION) != typeMapper.mapFunctionName(irFunction, OwnerKind.IMPLEMENTATION)
        if (isNonLocalReturn && state.isInlineDisabled) {
            //TODO: state.diagnostics.report(Errors.NON_LOCAL_RETURN_IN_DISABLED_INLINE.on(expression))
            genThrow(
                mv, "java/lang/UnsupportedOperationException",
                "Non-local returns are not allowed with inlining disabled"
            )
            return voidValue
        }

        val target = data.findBlock<ReturnableBlockInfo> { it.returnSymbol == expression.returnTargetSymbol }
        val returnType = typeMapper.mapReturnType(owner)
        val afterReturnLabel = Label()
        expression.value.accept(this, data).coerce(returnType).materialize()
        generateFinallyBlocksIfNeeded(returnType, afterReturnLabel, data, target)
        expression.markLineNumber(startOffset = true)
        if (target != null) {
            target.returnTemporary?.let { mv.store(it, returnType) }
            mv.fixStackAndJump(target.returnLabel)
        } else {
            if (isNonLocalReturn) {
                generateGlobalReturnFlag(mv, (owner as IrFunction).name.asString())
            }
            mv.areturn(returnType)
        }
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
        val typeOperand = expression.typeOperand
        val asmType = typeOperand.asmType
        val kotlinType = typeOperand.toKotlinType()
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

                if (typeOperand.isReifiedTypeParameter) {
                    putReifiedOperationMarkerIfTypeIsReifiedParameter(
                        typeOperand, if (IrTypeOperator.SAFE_CAST == expression.operator) SAFE_AS else AS, mv, this
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
                if (typeOperand.isReifiedTypeParameter) {
                    putReifiedOperationMarkerIfTypeIsReifiedParameter(typeOperand, ReifiedTypeInliner.OperationKind.IS, mv, this)
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
                    AsmUtil.genToString(StackValue.onStack(type), type, null, typeMapper.kotlinTypeMapper).put(expression.asmType, mv)
            }
            arity == 2 && expression.arguments[0].type.isStringClassType() -> {
                // Call the stringPlus intrinsic
                expression.arguments.forEach {
                    val type = it.accept(this, data).materialized.type
                    if (type.sort != Type.OBJECT)
                        AsmUtil.genToString(StackValue.onStack(type), type, null, typeMapper.kotlinTypeMapper).put(expression.asmType, mv)
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
            val parameter = clause.catchParameter
            val descriptorType = parameter.asmType
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
                putJavaLangClassInstance(mv, classType.type, null, typeMapper.kotlinTypeMapper)
                return classReference.onStack
            } else {
                val kotlinType = classType.toKotlinType()
                val classifier = classType.classifierOrNull
                if (classifier is IrTypeParameterSymbol) {
                    assert(classifier.owner.isReified) { "Non-reified type parameter under ::class should be rejected by type checker: ${classifier.owner.dump()}" }
                    putReifiedOperationMarkerIfTypeIsReifiedParameter(classType, ReifiedTypeInliner.OperationKind.JAVA_CLASS, mv, this)
                }

                putJavaLangClassInstance(mv, typeMapper.mapType(classType), kotlinType, typeMapper.kotlinTypeMapper)
            }
        } else {
            throw AssertionError("not an IrGetClass or IrClassReference: ${classReference.dump()}")
        }

        if (wrapIntoKClass) {
            wrapJavaClassIntoKClass(mv)
        }
        return classReference.onStack
    }

    private fun resolveToCallable(irCall: IrFunctionAccessExpression, isSuper: Boolean): Callable {
        return typeMapper.mapToCallableMethod(irCall.symbol.owner, isSuper)
    }

    private fun getOrCreateCallGenerator(element: IrFunctionAccessExpression, data: BlockInfo): IrCallGenerator {
        if (!element.symbol.owner.isInlineFunctionCall(context)) {
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
    private fun putReifiedOperationMarkerIfTypeIsReifiedParameter(
        type: IrType, operationKind: ReifiedTypeInliner.OperationKind, v: InstructionAdapter,
        codegen: BaseExpressionCodegen?
    ) {
        val typeParameterAndReificationArgument = extractReificationArgumentWithParameter(type)
        if (typeParameterAndReificationArgument != null && typeParameterAndReificationArgument.first.isReified) {
            val irTypeParameter = typeParameterAndReificationArgument.first
            codegen?.consumeReifiedOperationMarker(irTypeParameter.descriptor)
            ReifiedTypeInliner.putReifiedOperationMarker(
                operationKind, typeParameterAndReificationArgument.second.toReificationArgument(), v
            )
        }
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
