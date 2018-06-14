/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.backend.common.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.backend.common.descriptors.WrappedVariableDescriptor
import org.jetbrains.kotlin.backend.common.descriptors.WrappedPropertyDescriptor
import org.jetbrains.kotlin.backend.common.descriptors.substitute
import org.jetbrains.kotlin.backend.common.ir.copyParameterDeclarationsFrom
import org.jetbrains.kotlin.backend.konan.KonanBackendContext
import org.jetbrains.kotlin.backend.konan.KonanCompilationException
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.irasdescriptors.*
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.checkers.isRestrictsSuspensionReceiver
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.immediateSupertypes
import java.lang.reflect.Proxy

internal fun irBuilder(irBuiltIns: IrBuiltIns, scopeOwnerSymbol: IrSymbol): IrBuilderWithScope =
        object : IrBuilderWithScope(
                IrGeneratorContextBase(irBuiltIns),
                Scope(scopeOwnerSymbol),
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET
        ) {}

//TODO: delete file on next kotlin dependency update
internal fun IrExpression.isNullConst() = this is IrConst<*> && this.kind == IrConstKind.Null

private var topLevelInitializersCounter = 0

internal fun IrFile.addTopLevelInitializer(expression: IrExpression, context: KonanBackendContext, threadLocal: Boolean) {
    val descriptor = WrappedFieldDescriptor(
            if (threadLocal)
                Annotations.create(listOf(AnnotationDescriptorImpl(context.ir.symbols.threadLocal.defaultType,
                        emptyMap(), SourceElement.NO_SOURCE)))
            else
                Annotations.EMPTY
    )
    val irField = IrFieldImpl(
            expression.startOffset, expression.endOffset,
            IrDeclarationOrigin.DEFINED,
            IrFieldSymbolImpl(descriptor),
            "topLevelInitializer${topLevelInitializersCounter++}".synthesizedName,
            expression.type,
            Visibilities.PRIVATE,
            isFinal = true,
            isExternal = false,
            isStatic = true
    ).apply {
        descriptor.bind(this)

        initializer = IrExpressionBodyImpl(expression.startOffset, expression.endOffset, expression)
    }
    addChild(irField)
}

fun IrClass.addFakeOverrides(symbolTable: SymbolTable) {

    val startOffset = this.startOffset
    val endOffset = this.endOffset

    descriptor.unsubstitutedMemberScope.getContributedDescriptors()
            .filterIsInstance<CallableMemberDescriptor>()
            .filter { it.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE }
            .forEach {
                this.addChild(createFakeOverride(it, startOffset, endOffset, symbolTable))
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
            IrDeclarationOrigin.FAKE_OVERRIDE,
            this,
            symbolTable.translateErased(this@createFunction.returnType!!)
    ).apply {
        createParameterDeclarations(symbolTable)
    }

    return when (descriptor) {
        is FunctionDescriptor -> descriptor.createFunction()
        is PropertyDescriptor ->
            IrPropertyImpl(startOffset, endOffset, IrDeclarationOrigin.FAKE_OVERRIDE, descriptor).apply {
                // TODO: add field if getter is missing?
                getter = descriptor.getter?.createFunction()
                setter = descriptor.setter?.createFunction()
            }
        else -> TODO(descriptor.toString())
    }
}

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

fun IrSimpleFunction.setOverrides(symbolTable: ReferenceSymbolTable) {
    assert(this.overriddenSymbols.isEmpty())

    this.descriptor.overriddenDescriptors.mapTo(this.overriddenSymbols) {
        symbolTable.referenceSimpleFunction(it.original)
    }
}

fun IrClass.createParameterDeclarations(symbolTable: SymbolTable) {
    this.descriptor.declaredTypeParameters.mapTo(this.typeParameters) {
        IrTypeParameterImpl(startOffset, endOffset, IrDeclarationOrigin.DEFINED, it).apply {
            it.upperBounds.mapTo(this.superTypes, symbolTable::translateErased)
        }
    }

    this.thisReceiver = IrValueParameterImpl(
            startOffset, endOffset,
            IrDeclarationOrigin.DEFINED,
            this.descriptor.thisAsReceiverParameter,
            this.symbol.typeWith(this.typeParameters.map { it.defaultType }),
            null
    )
}

fun IrClass.setSuperSymbols(symbolTable: ReferenceSymbolTable) {
    assert(this.superTypes.isEmpty())
    this.descriptor.typeConstructor.supertypes.mapTo(this.superTypes) { symbolTable.translateErased(it) }

    this.simpleFunctions().forEach {
        it.setOverrides(symbolTable)
    }
}

fun IrClass.simpleFunctions() = declarations.flatMap {
    when (it) {
        is IrSimpleFunction -> listOf(it)
        is IrProperty -> listOfNotNull(it.getter, it.setter)
        else -> emptyList()
    }
}

fun IrClass.createParameterDeclarations() {
    assert (thisReceiver == null)

    thisReceiver = WrappedReceiverParameterDescriptor().let {
        IrValueParameterImpl(
                startOffset, endOffset,
                IrDeclarationOrigin.INSTANCE_RECEIVER,
                IrValueParameterSymbolImpl(it),
                Name.special("<this>"),
                0,
                symbol.typeWith(typeParameters.map { it.defaultType }),
                null,
                false,
                false
        ).apply {
            it.bind(this)
            parent = this@createParameterDeclarations
        }
    }
}

fun IrFunction.createDispatchReceiverParameter(origin: IrDeclarationOrigin? = null) {
    assert(dispatchReceiverParameter == null)

    dispatchReceiverParameter = IrValueParameterImpl(
            startOffset, endOffset,
            origin ?: parentAsClass.origin,
            IrValueParameterSymbolImpl(parentAsClass.thisReceiver!!.descriptor),
            Name.special("<this>"),
            0,
            parentAsClass.defaultType,
            null,
            false,
            false
    ).apply {
        parent = this@createDispatchReceiverParameter
    }
}

fun IrClass.addFakeOverrides() {
    fun IrDeclaration.toList() = when (this) {
        is IrSimpleFunction -> listOf(this)
        is IrProperty -> listOfNotNull(getter, setter)
        else -> emptyList()
    }

    val overriddenFunctions = declarations
            .flatMap { it.toList() }
            .flatMap { it.overriddenSymbols.map { it.owner } }
            .toSet()

    val unoverriddenSuperFunctions = superTypes
            .map { it.getClass()!! }
            .flatMap { irClass ->
                irClass.declarations
                        .flatMap { it.toList() }
                        .filter { it !in overriddenFunctions }
            }
            .toMutableSet()

    // TODO: A dirty hack.
    val groupedUnoverriddenSuperFunctions = unoverriddenSuperFunctions.groupBy { it.name.asString() + it.allParameters.size }

    fun createFakeOverride(overriddenFunctions: List<IrSimpleFunction>) =
            overriddenFunctions.first().let { irFunction ->
                val descriptor = WrappedSimpleFunctionDescriptor()
                IrFunctionImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        IrDeclarationOrigin.FAKE_OVERRIDE,
                        IrSimpleFunctionSymbolImpl(descriptor),
                        irFunction.name,
                        Visibilities.INHERITED,
                        Modality.OPEN,
                        irFunction.returnType,
                        irFunction.isInline,
                        irFunction.isExternal,
                        irFunction.isTailrec,
                        irFunction.isSuspend
                ).apply {
                    descriptor.bind(this)
                    parent = this@addFakeOverrides
                    overriddenSymbols += overriddenFunctions.map { it.symbol }
                    copyParameterDeclarationsFrom(irFunction)
                }
            }

    val fakeOverriddenFunctions = groupedUnoverriddenSuperFunctions
            .asSequence()
            .associate { it.value.first() to createFakeOverride(it.value) }
            .toMutableMap()

    declarations += fakeOverriddenFunctions.values
}

private fun IrElement.innerStartOffset(descriptor: DeclarationDescriptorWithSource): Int =
        descriptor.startOffset ?: this.startOffset

private fun IrElement.innerEndOffset(descriptor: DeclarationDescriptorWithSource): Int =
        descriptor.endOffset ?: this.endOffset

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

fun IrDeclarationContainer.addChildren(declarations: List<IrDeclaration>) {
    declarations.forEach { this.addChild(it) }
}

fun IrDeclarationContainer.addChild(declaration: IrDeclaration) {
    this.declarations += declaration
    declaration.accept(SetDeclarationsParentVisitor, this)
}

fun <T: IrElement> T.setDeclarationsParent(parent: IrDeclarationParent): T {
    accept(SetDeclarationsParentVisitor, parent)
    return this
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

fun IrModuleFragment.checkDeclarationParents() {
    this.accept(CheckDeclarationParentsVisitor, null)
}

object CheckDeclarationParentsVisitor : IrElementVisitor<Unit, IrDeclarationParent?> {

    override fun visitElement(element: IrElement, data: IrDeclarationParent?) {
        element.acceptChildren(this, element as? IrDeclarationParent ?: data)
    }

    override fun visitDeclaration(declaration: IrDeclaration, data: IrDeclarationParent?) {
        if (declaration !is IrVariable && declaration !is IrValueParameter && declaration !is IrTypeParameter) {
            checkParent(declaration, data)
        } else {
            // Don't check IrVariable parent.
        }

        super.visitDeclaration(declaration, data)
    }

    private fun checkParent(declaration: IrDeclaration, expectedParent: IrDeclarationParent?) {
        val parent = try {
            declaration.parent
        } catch (e: Throwable) {
            error("$declaration for ${declaration.descriptor} has no parent")
        }

        if (parent != expectedParent) {
            error("$declaration for ${declaration.descriptor} has unexpected parent $parent")
        }
    }
}

tailrec fun IrDeclaration.getContainingFile(): IrFile? {
    val parent = this.parent

    return when (parent) {
        is IrFile -> parent
        is IrDeclaration -> parent.getContainingFile()
        else -> null
    }
}

internal fun KonanBackendContext.report(declaration: IrDeclaration, message: String, isError: Boolean) {
    val irFile = declaration.getContainingFile()
    this.report(
            declaration,
            irFile,
            if (irFile != null) {
                message
            } else {
                val renderer = org.jetbrains.kotlin.renderer.DescriptorRenderer.COMPACT_WITH_SHORT_TYPES
                "$message\n${renderer.render(declaration.descriptor)}"
            },
            isError
    )
    if (isError) throw KonanCompilationException()
}

fun IrBuilderWithScope.irForceNotNull(expression: IrExpression): IrExpression {
    if (!expression.type.containsNull()) {
        return expression
    }

    return irBlock {
        val temporary = irTemporaryVar(expression)
        +irIfNull(
                expression.type,
                subject = irGet(temporary),
                thenPart = irThrowNpe(IrStatementOrigin.EXCLEXCL),
                elsePart = irGet(temporary)
        )
    }
}

fun IrFunctionAccessExpression.addArguments(args: Map<IrValueParameter, IrExpression>) {
    val unhandledParameters = args.keys.toMutableSet()
    fun getArg(parameter: IrValueParameter) = args[parameter]?.also { unhandledParameters -= parameter }

    symbol.owner.dispatchReceiverParameter?.let {
        val arg = getArg(it)
        if (arg != null) {
            this.dispatchReceiver = arg
        }
    }

    symbol.owner.extensionReceiverParameter?.let {
        val arg = getArg(it)
        if (arg != null) {
            this.extensionReceiver = arg
        }
    }

    symbol.owner.valueParameters.forEach {
        val arg = getArg(it)
        if (arg != null) {
            this.putValueArgument(it.index, arg)
        }
    }
}

private fun FunctionDescriptor.substitute(
        typeArguments: List<IrType>
): FunctionDescriptor = if (typeArguments.isEmpty()) {
    this
} else {
    this.substitute(*typeArguments.map { it.toKotlinType() }.toTypedArray())
}

fun IrType.substitute(map: Map<IrTypeParameterSymbol, IrType>): IrType {
    if (this !is IrSimpleType) return this

    val classifier = this.classifier
    return when (classifier) {
        is IrTypeParameterSymbol ->
            map[classifier]?.let { if (this.hasQuestionMark) it.makeNullable() else it }
                    ?: this

        is IrClassSymbol -> if (this.arguments.isEmpty()) {
            this // Fast path.
        } else {
            val newArguments = this.arguments.map {
                when (it) {
                    is IrTypeProjection -> makeTypeProjection(it.type.substitute(map), it.variance)
                    is IrStarProjection -> it
                    else -> error(it)
                }
            }
            IrSimpleTypeImpl(classifier, hasQuestionMark, newArguments, annotations)
        }
        else -> error(classifier)
    }

}

private fun IrFunction.substitutedReturnType(typeArguments: List<IrType>): IrType {
    val unsubstituted = this.returnType
    if (typeArguments.isEmpty()) return unsubstituted // Fast path.
    if (this is IrConstructor) {
        // Workaround for missing type parameters in constructors. TODO: remove.
        return this.returnType.classifierOrFail.typeWith(typeArguments)
    }

    assert(this.typeParameters.size >= typeArguments.size) // TODO: check equality.
    // TODO: receiver type must also be considered.
    return unsubstituted.substitute(this.typeParameters.map { it.symbol }.zip(typeArguments).toMap())
}

// TODO: this function must be avoided since it takes symbol's owner implicitly.
fun IrBuilderWithScope.irCall(symbol: IrFunctionSymbol, typeArguments: List<IrType> = emptyList()) =
        irCall(symbol.owner, typeArguments)

fun IrBuilderWithScope.irCall(
        irFunction: IrFunction,
        typeArguments: List<IrType> = emptyList()
): IrCall = irCall(this.startOffset, this.endOffset, irFunction, typeArguments)

internal fun irCall(startOffset: Int, endOffset: Int, irFunction: IrFunction, typeArguments: List<IrType>): IrCall =
        IrCallImpl(
                startOffset, endOffset, irFunction.substitutedReturnType(typeArguments),
                irFunction.symbol, irFunction.descriptor.substitute(typeArguments), typeArguments.size
        ).apply {
            typeArguments.forEachIndexed { index, irType ->
                this.putTypeArgument(index, irType)
            }
        }

fun IrBuilderWithScope.irCallOp(
        callee: IrFunction,
        dispatchReceiver: IrExpression,
        argument: IrExpression
): IrCall =
        irCall(callee).apply {
            this.dispatchReceiver = dispatchReceiver
            putValueArgument(0, argument)
        }

fun IrBuilderWithScope.irSetVar(variable: IrVariable, value: IrExpression) =
        irSetVar(variable.symbol, value)

fun IrBuilderWithScope.irCatch(type: IrType) =
        IrCatchImpl(
                startOffset, endOffset,
                WrappedVariableDescriptor().let { descriptor ->
                    IrVariableImpl(
                            startOffset,
                            endOffset,
                            IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
                            IrVariableSymbolImpl(descriptor),
                            Name.identifier("e"),
                            type,
                            false,
                            false,
                            false
                    ).apply {
                        descriptor.bind(this)
                    }
                }
        )

/**
 * Binds the arguments explicitly represented in the IR to the parameters of the accessed function.
 * The arguments are to be evaluated in the same order as they appear in the resulting list.
 */
fun IrMemberAccessExpression.getArgumentsWithIr(): List<Pair<IrValueParameter, IrExpression>> {
    val res = mutableListOf<Pair<IrValueParameter, IrExpression>>()
    val irFunction = when (this) {
        is IrFunctionAccessExpression -> this.symbol.owner
        is IrFunctionReference -> this.symbol.owner
        else -> error(this)
    }

    dispatchReceiver?.let {
        res += (irFunction.dispatchReceiverParameter!! to it)
    }

    extensionReceiver?.let {
        res += (irFunction.extensionReceiverParameter!! to it)
    }

    irFunction.valueParameters.forEachIndexed { index, it ->
        val arg = getValueArgument(index)
        if (arg != null) {
            res += (it to arg)
        }
    }

    return res
}

fun ReferenceSymbolTable.translateErased(type: KotlinType): IrSimpleType {
    val descriptor = TypeUtils.getClassDescriptor(type)
    if (descriptor == null) return translateErased(type.immediateSupertypes().first())
    val classSymbol = this.referenceClass(descriptor)

    val nullable = type.isMarkedNullable
    val arguments = type.arguments.map { IrStarProjectionImpl }

    return classSymbol.createType(nullable, arguments)
}

fun CommonBackendContext.createArrayOfExpression(
        startOffset: Int, endOffset: Int,
        arrayElementType: IrType,
        arrayElements: List<IrExpression>
): IrExpression {

    val arrayType = ir.symbols.array.typeWith(arrayElementType)
    val arg0 = IrVarargImpl(startOffset, endOffset, arrayType, arrayElementType, arrayElements)

    return irCall(startOffset, endOffset, ir.symbols.arrayOf.owner, listOf(arrayElementType)).apply {
        putValueArgument(0, arg0)
    }
}

fun createField(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        type: IrType,
        name: Name,
        isMutable: Boolean,
        owner: IrClass
) = WrappedFieldDescriptor().let {
    IrFieldImpl(
            startOffset, endOffset,
            origin,
            IrFieldSymbolImpl(it),
            name,
            type,
            Visibilities.PRIVATE,
            !isMutable,
            false,
            false
    ).apply {
        it.bind(this)
        owner.declarations += this
        parent = owner
    }
}

fun IrValueParameter.copy(newDescriptor: ParameterDescriptor): IrValueParameter {
    // Aggressive use of WrappedDescriptors during deserialization
    // makes these types different.
    // Let's hope they not really used afterwards.
    //assert(this.descriptor.type == newDescriptor.type) {
     //   "type1 = ${this.descriptor.type} != type2 = ${newDescriptor.type}"
    //}

    return IrValueParameterImpl(
            startOffset,
            endOffset,
            IrDeclarationOrigin.DEFINED,
            newDescriptor,
            type,
            varargElementType
    )
}

val IrTypeArgument.typeOrNull: IrType? get() = (this as? IrTypeProjection)?.type

val IrType.isSimpleTypeWithQuestionMark: Boolean
    get() = this is IrSimpleType && this.hasQuestionMark

fun IrClass.defaultOrNullableType(hasQuestionMark: Boolean) =
        if (hasQuestionMark) this.defaultType.makeNullable(false) else this.defaultType

fun IrFunction.isRestrictedSuspendFunction(languageVersionSettings: LanguageVersionSettings): Boolean =
        this.descriptor.extensionReceiverParameter?.type?.isRestrictsSuspensionReceiver(languageVersionSettings) == true
