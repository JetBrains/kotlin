/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ir

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.createDispatchReceiverParameter
import org.jetbrains.kotlin.ir.util.endOffset
import org.jetbrains.kotlin.ir.util.startOffset
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.resolve.OverridingStrategy
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.immediateSupertypes
import java.lang.reflect.Proxy

object JsIrBuilder {

    object SYNTHESIZED_STATEMENT : IrStatementOriginImpl("SYNTHESIZED_STATEMENT")
    object SYNTHESIZED_DECLARATION : IrDeclarationOriginImpl("SYNTHESIZED_DECLARATION")

    fun buildCall(target: IrFunctionSymbol, type: IrType? = null, typeArguments: List<IrType>? = null): IrCall =
        IrCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            type ?: target.owner.returnType,
            target,
            target.descriptor,
            target.descriptor.typeParametersCount,
            SYNTHESIZED_STATEMENT
        ).apply {
            typeArguments?.let {
                assert(typeArguments.size == typeArgumentsCount)
                it.withIndex().forEach { (i, t) -> putTypeArgument(i, t) }
            }
        }

    fun buildReturn(targetSymbol: IrFunctionSymbol, value: IrExpression, type: IrType) =
        IrReturnImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, targetSymbol, value)

    fun buildThrow(type: IrType, value: IrExpression) = IrThrowImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, value)

    fun buildValueParameter(symbol: IrValueParameterSymbol, type: IrType? = null) =
        IrValueParameterImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, SYNTHESIZED_DECLARATION, symbol, type ?: symbol.owner.type, null)

    fun buildFunction(symbol: IrSimpleFunctionSymbol, returnType: IrType) =
        IrFunctionImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, SYNTHESIZED_DECLARATION, symbol).apply {
            this.returnType = returnType
        }

    fun buildGetObjectValue(type: IrType, classSymbol: IrClassSymbol) =
        IrGetObjectValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, classSymbol)

    fun buildGetClass(expression: IrExpression, type: IrType) = IrGetClassImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, expression)

    fun buildGetValue(symbol: IrValueSymbol) = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbol.owner.type, symbol, SYNTHESIZED_STATEMENT)
    fun buildSetVariable(symbol: IrVariableSymbol, value: IrExpression, type: IrType) =
        IrSetVariableImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, symbol, value, SYNTHESIZED_STATEMENT)

    fun buildGetField(symbol: IrFieldSymbol, receiver: IrExpression?, superQualifierSymbol: IrClassSymbol? = null, type: IrType? = null) =
        IrGetFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbol, type ?: symbol.owner.type, receiver, SYNTHESIZED_STATEMENT, superQualifierSymbol)

    fun buildSetField(symbol: IrFieldSymbol, receiver: IrExpression?, value: IrExpression, type: IrType, superQualifierSymbol: IrClassSymbol? = null) =
        IrSetFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, symbol, receiver, value, type, SYNTHESIZED_STATEMENT, superQualifierSymbol)

    fun buildBlockBody(statements: List<IrStatement>) = IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, statements)

    fun buildBlock(type: IrType) = IrBlockImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, SYNTHESIZED_STATEMENT)
    fun buildBlock(type: IrType, statements: List<IrStatement>) =
        IrBlockImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, SYNTHESIZED_STATEMENT, statements)

    fun buildComposite(type: IrType, statements: List<IrStatement> = emptyList()) =
        IrCompositeImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, SYNTHESIZED_STATEMENT, statements)

    fun buildFunctionReference(type: IrType, symbol: IrFunctionSymbol) =
        IrFunctionReferenceImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, symbol, symbol.descriptor, 0, null)

    fun buildVar(symbol: IrVariableSymbol, initializer: IrExpression? = null, type: IrType) =
        IrVariableImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, SYNTHESIZED_DECLARATION, symbol, type)
            .apply { this.initializer = initializer }

    fun buildBreak(type: IrType, loop: IrLoop) = IrBreakImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, loop)
    fun buildContinue(type: IrType, loop: IrLoop) = IrContinueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, loop)

    fun buildIfElse(type: IrType, cond: IrExpression, thenBranch: IrExpression, elseBranch: IrExpression? = null): IrWhen = buildIfElse(
        UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, cond, thenBranch, elseBranch, SYNTHESIZED_STATEMENT
    )

    fun buildIfElse(
        startOffset: Int,
        endOffset: Int,
        type: IrType,
        cond: IrExpression,
        thenBranch: IrExpression,
        elseBranch: IrExpression? = null,
        origin: IrStatementOrigin? = null
    ): IrWhen {
        val element = IrIfThenElseImpl(startOffset, endOffset, type, origin)
        element.branches.add(IrBranchImpl(cond, thenBranch))
        if (elseBranch != null) {
            val irTrue = IrConstImpl.boolean(UNDEFINED_OFFSET, UNDEFINED_OFFSET, cond.type, true)
            element.branches.add(IrElseBranchImpl(irTrue, elseBranch))
        }

        return element
    }

    fun buildWhen(type: IrType, branches: List<IrBranch>, origin: IrStatementOrigin = SYNTHESIZED_STATEMENT) =
        IrWhenImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, origin, branches)

    fun buildTypeOperator(type: IrType, operator: IrTypeOperator, argument: IrExpression, toType: IrType, symbol: IrClassifierSymbol) =
        IrTypeOperatorCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, operator, toType, symbol, argument)

    fun buildNull(type: IrType) = IrConstImpl.constNull(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type)
    fun buildBoolean(type: IrType, v: Boolean) = IrConstImpl.boolean(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, v)
    fun buildInt(type: IrType, v: Int) = IrConstImpl.int(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, v)
    fun buildString(type: IrType, s: String) = IrConstImpl.string(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, s)
    fun buildCatch(ex: IrVariable, block: IrBlockImpl) = IrCatchImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, ex, block)
}


object SetDeclarationsParentVisitor : IrElementVisitor<Unit, IrDeclarationParent> {
    override fun visitElement(element: IrElement, data: IrDeclarationParent) {
        if (element !is IrDeclarationParent) {
            element.acceptChildren(this, data)
        }
    }

    override fun visitDeclaration(declaration: IrDeclaration, data: IrDeclarationParent) {
        declaration.parent = data
        super.visitDeclaration(declaration, data)
    }
}

fun SymbolTable.translateErased(type: KotlinType): IrSimpleType {
    val descriptor = TypeUtils.getClassDescriptor(type)
    if (descriptor == null) return translateErased(type.immediateSupertypes().first())
    val classSymbol = this.referenceClass(descriptor)

    val nullable = type.isMarkedNullable
    val arguments = type.arguments.map { IrStarProjectionImpl }

    return classSymbol.createType(nullable, arguments)
}

fun IrDeclarationContainer.addChildren(declarations: List<IrDeclaration>) {
    declarations.forEach { this.addChild(it) }
}

fun IrDeclarationContainer.addChild(declaration: IrDeclaration) {
    this.declarations += declaration
    declaration.accept(SetDeclarationsParentVisitor, this)
}

fun IrClass.simpleFunctions(): List<IrSimpleFunction> = this.declarations.flatMap {
    when (it) {
        is IrSimpleFunction -> listOf(it)
        is IrProperty -> listOfNotNull(it.getter as IrSimpleFunction?, it.setter as IrSimpleFunction?)
        else -> emptyList()
    }
}

fun IrClass.setSuperSymbols(superTypes: List<IrType>) {
    val supers = superTypes.map { it.getClass()!! }
    assert(this.superDescriptors().toSet() == supers.map { it.descriptor }.toSet())
    assert(this.superTypes.isEmpty())
    this.superTypes += superTypes

    val superMembers = supers.flatMap {
        it.simpleFunctions()
    }.associateBy { it.descriptor }

    this.simpleFunctions().forEach {
        assert(it.overriddenSymbols.isEmpty())

        it.descriptor.overriddenDescriptors.mapTo(it.overriddenSymbols) {
            val superMember = superMembers[it.original] ?: error(it.original)
            superMember.symbol
        }
    }
}

fun IrSimpleFunction.setOverrides(symbolTable: SymbolTable) {
    assert(this.overriddenSymbols.isEmpty())

    this.descriptor.overriddenDescriptors.mapTo(this.overriddenSymbols) {
        symbolTable.referenceSimpleFunction(it.original)
    }
}


private fun IrClass.superDescriptors() =
    this.descriptor.typeConstructor.supertypes.map { it.constructor.declarationDescriptor as ClassDescriptor }

fun IrClass.setSuperSymbols(symbolTable: SymbolTable) {
    assert(this.superTypes.isEmpty())
    this.descriptor.typeConstructor.supertypes.mapTo(this.superTypes) { symbolTable.translateErased(it) }

    this.simpleFunctions().forEach {
        it.setOverrides(symbolTable)
    }
}

private fun IrElement.innerStartOffset(descriptor: DeclarationDescriptorWithSource): Int =
    descriptor.startOffset ?: this.startOffset

private fun IrElement.innerEndOffset(descriptor: DeclarationDescriptorWithSource): Int =
    descriptor.endOffset ?: this.endOffset

fun IrFunction.createParameterDeclarations(symbolTable: SymbolTable) {

    fun ParameterDescriptor.irValueParameter() = IrValueParameterImpl(
        innerStartOffset(this), innerEndOffset(this),
        IrDeclarationOrigin.DEFINED,
        this, symbolTable.translateErased(this.type),
        (this as? ValueParameterDescriptor)?.varargElementType?.let { symbolTable.translateErased(it) }
    ).also {
        it.parent = this@createParameterDeclarations
    }

    dispatchReceiverParameter = descriptor.dispatchReceiverParameter?.irValueParameter()
    extensionReceiverParameter = descriptor.extensionReceiverParameter?.irValueParameter()

    assert(valueParameters.isEmpty())
    descriptor.valueParameters.mapTo(valueParameters) { it.irValueParameter() }

    assert(typeParameters.isEmpty())
    descriptor.typeParameters.mapTo(typeParameters) {
        IrTypeParameterImpl(
            innerStartOffset(it), innerEndOffset(it),
            IrDeclarationOrigin.DEFINED,
            it
        ).also { typeParameter ->
            typeParameter.parent = this
            typeParameter.descriptor.upperBounds.mapTo(typeParameter.superTypes, symbolTable::translateErased)
        }
    }
}

private fun createFakeOverride(
    descriptor: CallableMemberDescriptor,
    startOffset: Int,
    endOffset: Int,
    symbolTable: SymbolTable
): IrDeclaration {

    fun FunctionDescriptor.createFunction(): IrSimpleFunction = IrFunctionImpl(
        startOffset, endOffset,
        IrDeclarationOrigin.FAKE_OVERRIDE, this
    ).apply {
        returnType = symbolTable.translateErased(this@createFunction.returnType!!)
        createParameterDeclarations(symbolTable)
    }

    return when (descriptor) {
        is FunctionDescriptor -> descriptor.createFunction()
        is PropertyDescriptor ->
            IrPropertyImpl(startOffset, endOffset, IrDeclarationOrigin.FAKE_OVERRIDE, descriptor).apply {
                // TODO: add field if getter is missing?
                getter = descriptor.getter?.createFunction() as IrSimpleFunction?
                setter = descriptor.setter?.createFunction() as IrSimpleFunction?
            }
        else -> TODO(descriptor.toString())
    }
}

private fun createFakeOverride(
    descriptor: CallableMemberDescriptor,
    overriddenDeclarations: List<IrDeclaration>,
    irClass: IrClass
): IrDeclaration {

    // TODO: this function doesn't substitute types.
    fun IrSimpleFunction.copyFake(descriptor: FunctionDescriptor): IrSimpleFunction = IrFunctionImpl(
        irClass.startOffset, irClass.endOffset, IrDeclarationOrigin.FAKE_OVERRIDE, descriptor
    ).also {
        it.returnType = returnType
        it.parent = irClass
        it.createDispatchReceiverParameter()

        it.extensionReceiverParameter = this.extensionReceiverParameter?.let {
            IrValueParameterImpl(
                it.startOffset,
                it.endOffset,
                IrDeclarationOrigin.DEFINED,
                it.descriptor.extensionReceiverParameter!!,
                it.type,
                null
            )
        }

        this.valueParameters.mapTo(it.valueParameters) { oldParameter ->
            IrValueParameterImpl(
                oldParameter.startOffset,
                oldParameter.endOffset,
                IrDeclarationOrigin.DEFINED,
                it.descriptor.valueParameters[oldParameter.index],
                oldParameter.type,
                (oldParameter as? IrValueParameter)?.varargElementType
            )
        }

        this.typeParameters.mapTo(it.typeParameters) { oldParameter ->
            IrTypeParameterImpl(
                irClass.startOffset,
                irClass.endOffset,
                IrDeclarationOrigin.DEFINED,
                it.descriptor.typeParameters[oldParameter.index]
            ).apply {
                superTypes += oldParameter.superTypes
            }
        }
    }

    val copiedDeclaration = overriddenDeclarations.first()

    return when (copiedDeclaration) {
        is IrSimpleFunction -> copiedDeclaration.copyFake(descriptor as FunctionDescriptor)
        is IrProperty -> IrPropertyImpl(
            irClass.startOffset,
            irClass.endOffset,
            IrDeclarationOrigin.FAKE_OVERRIDE,
            descriptor as PropertyDescriptor
        ).apply {
            parent = irClass
            getter = copiedDeclaration.getter?.copyFake(descriptor.getter!!)
            setter = copiedDeclaration.setter?.copyFake(descriptor.setter!!)
        }
        else -> error(copiedDeclaration)
    }
}

fun IrClass.setSuperSymbolsAndAddFakeOverrides(superTypes: List<IrType>) {
    val overriddenSuperMembers = this.declarations.map { it.descriptor }
        .filterIsInstance<CallableMemberDescriptor>().flatMap { it.overriddenDescriptors.map { it.original } }.toSet()

    val unoverriddenSuperMembers = superTypes.map { it.getClass()!! }.flatMap {
        it.declarations.filter { it.descriptor !in overriddenSuperMembers }.mapNotNull {
            when (it) {
                is IrSimpleFunction -> it.descriptor to it
                is IrProperty -> it.descriptor to it
                else -> null
            }
        }
    }.toMap()

    val irClass = this

    val overridingStrategy = object : OverridingStrategy() {
        override fun addFakeOverride(fakeOverride: CallableMemberDescriptor) {
            val overriddenDeclarations =
                fakeOverride.overriddenDescriptors.map { unoverriddenSuperMembers[it]!! }

            assert(overriddenDeclarations.isNotEmpty())

            irClass.declarations.add(createFakeOverride(fakeOverride, overriddenDeclarations, irClass))
        }

        override fun inheritanceConflict(first: CallableMemberDescriptor, second: CallableMemberDescriptor) {
            error("inheritance conflict in synthesized class ${irClass.descriptor}:\n  $first\n  $second")
        }

        override fun overrideConflict(fromSuper: CallableMemberDescriptor, fromCurrent: CallableMemberDescriptor) {
            error("override conflict in synthesized class ${irClass.descriptor}:\n  $fromSuper\n  $fromCurrent")
        }
    }

    unoverriddenSuperMembers.keys.groupBy { it.name }.forEach { (name, members) ->
        OverridingUtil.generateOverridesInFunctionGroup(
            name,
            members,
            emptyList(),
            this.descriptor,
            overridingStrategy
        )
    }

    this.setSuperSymbols(superTypes)
}

inline fun <reified T> stub(name: String): T {
    return Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) {
            _ /* proxy */, method, _ /* methodArgs */ ->
        if (method.name == "toString" && method.parameterCount == 0) {
            "${T::class.simpleName} stub for $name"
        } else {
            error("${T::class.simpleName}.${method.name} is not supported for $name")
        }
    } as T
}