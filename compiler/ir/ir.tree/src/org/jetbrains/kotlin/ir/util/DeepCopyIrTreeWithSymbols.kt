/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.memoryOptimizedMap

inline fun <reified T : IrElement> T.deepCopyWithSymbols(
    initialParent: IrDeclarationParent? = null,
    createCopier: (SymbolRemapper, TypeRemapper) -> DeepCopyIrTreeWithSymbols = ::DeepCopyIrTreeWithSymbols
): T = deepCopyWithSymbols(initialParent, DeepCopySymbolRemapper(), createCopier)

inline fun <reified T : IrElement> T.deepCopyWithSymbols(
    initialParent: IrDeclarationParent?,
    symbolRemapper: DeepCopySymbolRemapper,
    createCopier: (SymbolRemapper, TypeRemapper) -> DeepCopyIrTreeWithSymbols = ::DeepCopyIrTreeWithSymbols
): T {
    acceptVoid(symbolRemapper)
    val typeRemapper = DeepCopyTypeRemapper(symbolRemapper)
    return transform(createCopier(symbolRemapper, typeRemapper), null).patchDeclarationParents(initialParent) as T
}

interface SymbolRenamer {
    fun getClassName(symbol: IrClassSymbol): Name = symbol.owner.name
    fun getFunctionName(symbol: IrSimpleFunctionSymbol): Name = symbol.owner.name
    fun getFieldName(symbol: IrFieldSymbol): Name = symbol.owner.name
    fun getFileName(symbol: IrFileSymbol): FqName = symbol.owner.packageFqName
    fun getExternalPackageFragmentName(symbol: IrExternalPackageFragmentSymbol): FqName = symbol.owner.packageFqName
    fun getEnumEntryName(symbol: IrEnumEntrySymbol): Name = symbol.owner.name
    fun getVariableName(symbol: IrVariableSymbol): Name = symbol.owner.name
    fun getTypeParameterName(symbol: IrTypeParameterSymbol): Name = symbol.owner.name
    fun getValueParameterName(symbol: IrValueParameterSymbol): Name = symbol.owner.name
    fun getTypeAliasName(symbol: IrTypeAliasSymbol): Name = symbol.owner.name

    object DEFAULT : SymbolRenamer
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
open class DeepCopyIrTreeWithSymbols(
    private val symbolRemapper: SymbolRemapper,
    private val typeRemapper: TypeRemapper,
    private val symbolRenamer: SymbolRenamer
) : IrElementTransformerVoid() {

    private var transformedModule: IrModuleFragment? = null

    constructor(symbolRemapper: SymbolRemapper, typeRemapper: TypeRemapper) : this(symbolRemapper, typeRemapper, SymbolRenamer.DEFAULT)

    init {
        // TODO refactor
        (typeRemapper as? DeepCopyTypeRemapper)?.let {
            it.deepCopy = this
        }
    }

    protected fun mapDeclarationOrigin(origin: IrDeclarationOrigin) = origin
    protected fun mapStatementOrigin(origin: IrStatementOrigin?) = origin

    protected open fun <D : IrAttributeContainer> D.processAttributes(other: IrAttributeContainer?): D =
        copyAttributes(other)

    protected inline fun <reified T : IrElement> T.transform() =
        transform(this@DeepCopyIrTreeWithSymbols, null) as T

    protected inline fun <reified T : IrElement> List<T>.transform() =
        memoryOptimizedMap { it.transform() }

    protected inline fun <reified T : IrElement> List<T>.transformTo(destination: MutableList<T>) =
        mapTo(destination) { it.transform() }

    protected fun <T : IrDeclarationContainer> T.transformDeclarationsTo(destination: T) =
        declarations.transformTo(destination.declarations)

    protected fun IrType.remapType() = typeRemapper.remapType(this)

    override fun visitElement(element: IrElement): IrElement =
        throw IllegalArgumentException("Unsupported element type: $element")

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val result = IrModuleFragmentImpl(
            declaration.descriptor,
            declaration.irBuiltins,
        )
        transformedModule = result
        result.files += declaration.files.transform()
        transformedModule = null
        return result
    }

    override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment): IrExternalPackageFragment =
        IrExternalPackageFragmentImpl(
            symbolRemapper.getDeclaredExternalPackageFragment(declaration.symbol),
            symbolRenamer.getExternalPackageFragmentName(declaration.symbol)
        ).apply {
            declaration.transformDeclarationsTo(this)
        }

    override fun visitFile(declaration: IrFile): IrFile =
        IrFileImpl(
            declaration.fileEntry,
            symbolRemapper.getDeclaredFile(declaration.symbol),
            symbolRenamer.getFileName(declaration.symbol),
            transformedModule ?: declaration.module
        ).apply {
            transformAnnotations(declaration)
            declaration.transformDeclarationsTo(this)
        }

    override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement =
        throw IllegalArgumentException("Unsupported declaration type: $declaration")

    override fun visitScript(declaration: IrScript): IrStatement {
        return IrScriptImpl(
            symbolRemapper.getDeclaredScript(declaration.symbol),
            declaration.name,
            declaration.factory,
            declaration.startOffset,
            declaration.endOffset
        ).also { scriptCopy ->
            scriptCopy.thisReceiver = declaration.thisReceiver?.transform()
            declaration.statements.mapTo(scriptCopy.statements) { it.transform() }
            scriptCopy.importedScripts = declaration.importedScripts
            scriptCopy.earlierScripts = declaration.earlierScripts
            scriptCopy.earlierScriptsParameter = declaration.earlierScriptsParameter
            scriptCopy.explicitCallParameters = declaration.explicitCallParameters.memoryOptimizedMap { it.transform() }
            scriptCopy.implicitReceiversParameters = declaration.implicitReceiversParameters.memoryOptimizedMap { it.transform() }
            scriptCopy.providedPropertiesParameters = declaration.providedPropertiesParameters.memoryOptimizedMap { it.transform() }
        }
    }

    override fun visitClass(declaration: IrClass): IrClass =
        declaration.factory.createClass(
            startOffset = declaration.startOffset,
            endOffset = declaration.endOffset,
            origin = mapDeclarationOrigin(declaration.origin),
            name = symbolRenamer.getClassName(declaration.symbol),
            visibility = declaration.visibility,
            symbol = symbolRemapper.getDeclaredClass(declaration.symbol),
            kind = declaration.kind,
            modality = declaration.modality,
            isExternal = declaration.isExternal,
            isCompanion = declaration.isCompanion,
            isInner = declaration.isInner,
            isData = declaration.isData,
            isValue = declaration.isValue,
            isExpect = declaration.isExpect,
            isFun = declaration.isFun,
            hasEnumEntries = declaration.hasEnumEntries,
        ).apply {
            transformAnnotations(declaration)
            copyTypeParametersFrom(declaration)
            superTypes = declaration.superTypes.memoryOptimizedMap {
                it.remapType()
            }
            sealedSubclasses = declaration.sealedSubclasses.memoryOptimizedMap {
                symbolRemapper.getReferencedClass(it)
            }
            thisReceiver = declaration.thisReceiver?.transform()
            valueClassRepresentation = declaration.valueClassRepresentation?.mapUnderlyingType { it.remapType() as IrSimpleType }
            declaration.transformDeclarationsTo(this)
        }.processAttributes(declaration)

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrSimpleFunction =
        declaration.factory.createSimpleFunction(
            startOffset = declaration.startOffset,
            endOffset = declaration.endOffset,
            origin = mapDeclarationOrigin(declaration.origin),
            name = symbolRenamer.getFunctionName(declaration.symbol),
            visibility = declaration.visibility,
            isInline = declaration.isInline,
            isExpect = declaration.isExpect,
            returnType = declaration.returnType,
            modality = declaration.modality,
            symbol = symbolRemapper.getDeclaredFunction(declaration.symbol),
            isTailrec = declaration.isTailrec,
            isSuspend = declaration.isSuspend,
            isOperator = declaration.isOperator,
            isInfix = declaration.isInfix,
            isExternal = declaration.isExternal,
            containerSource = declaration.containerSource,
            isFakeOverride = declaration.isFakeOverride,
        ).apply {
            overriddenSymbols = declaration.overriddenSymbols.memoryOptimizedMap {
                symbolRemapper.getReferencedFunction(it) as IrSimpleFunctionSymbol
            }
            contextReceiverParametersCount = declaration.contextReceiverParametersCount
            processAttributes(declaration)
            transformFunctionChildren(declaration)
        }

    override fun visitConstructor(declaration: IrConstructor): IrConstructor =
        declaration.factory.createConstructor(
            startOffset = declaration.startOffset,
            endOffset = declaration.endOffset,
            origin = mapDeclarationOrigin(declaration.origin),
            name = declaration.name,
            visibility = declaration.visibility,
            isInline = declaration.isInline,
            isExpect = declaration.isExpect,
            returnType = declaration.returnType,
            symbol = symbolRemapper.getDeclaredConstructor(declaration.symbol),
            isPrimary = declaration.isPrimary,
            isExternal = declaration.isExternal,
            containerSource = declaration.containerSource,
        ).apply {
            transformFunctionChildren(declaration)
        }

    private fun <T : IrFunction> T.transformFunctionChildren(declaration: T): T =
        apply {
            transformAnnotations(declaration)
            copyTypeParametersFrom(declaration)
            typeRemapper.withinScope(this) {
                dispatchReceiverParameter = declaration.dispatchReceiverParameter?.transform()
                extensionReceiverParameter = declaration.extensionReceiverParameter?.transform()
                returnType = typeRemapper.remapType(declaration.returnType)
                valueParameters = declaration.valueParameters.transform()
                body = declaration.body?.transform()
            }
        }

    protected fun IrMutableAnnotationContainer.transformAnnotations(declaration: IrAnnotationContainer) {
        annotations = declaration.annotations.transform()
    }

    override fun visitProperty(declaration: IrProperty): IrProperty =
        declaration.factory.createProperty(
            startOffset = declaration.startOffset,
            endOffset = declaration.endOffset,
            origin = mapDeclarationOrigin(declaration.origin),
            name = declaration.name,
            visibility = declaration.visibility,
            modality = declaration.modality,
            symbol = symbolRemapper.getDeclaredProperty(declaration.symbol),
            isVar = declaration.isVar,
            isConst = declaration.isConst,
            isLateinit = declaration.isLateinit,
            isDelegated = declaration.isDelegated,
            isExternal = declaration.isExternal,
            containerSource = declaration.containerSource,
            isExpect = declaration.isExpect,
        ).apply {
            transformAnnotations(declaration)
            processAttributes(declaration)
            this.backingField = declaration.backingField?.transform()?.also {
                it.correspondingPropertySymbol = symbol
            }
            this.getter = declaration.getter?.transform()?.also {
                it.correspondingPropertySymbol = symbol
            }
            this.setter = declaration.setter?.transform()?.also {
                it.correspondingPropertySymbol = symbol
            }
            this.overriddenSymbols = declaration.overriddenSymbols.memoryOptimizedMap {
                symbolRemapper.getReferencedProperty(it)
            }
        }

    override fun visitField(declaration: IrField): IrField =
        declaration.factory.createField(
            startOffset = declaration.startOffset,
            endOffset = declaration.endOffset,
            origin = mapDeclarationOrigin(declaration.origin),
            name = symbolRenamer.getFieldName(declaration.symbol),
            visibility = declaration.visibility,
            symbol = symbolRemapper.getDeclaredField(declaration.symbol),
            type = declaration.type.remapType(),
            isFinal = declaration.isFinal,
            isStatic = declaration.isStatic,
            isExternal = declaration.isExternal,
        ).apply {
            transformAnnotations(declaration)
            initializer = declaration.initializer?.transform()
        }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrLocalDelegatedProperty =
        declaration.factory.createLocalDelegatedProperty(
            startOffset = declaration.startOffset,
            endOffset = declaration.endOffset,
            origin = mapDeclarationOrigin(declaration.origin),
            name = declaration.name,
            symbol = symbolRemapper.getDeclaredLocalDelegatedProperty(declaration.symbol),
            type = declaration.type.remapType(),
            isVar = declaration.isVar,
        ).apply {
            transformAnnotations(declaration)
            delegate = declaration.delegate.transform()
            getter = declaration.getter.transform()
            setter = declaration.setter?.transform()
        }

    override fun visitEnumEntry(declaration: IrEnumEntry): IrEnumEntry =
        declaration.factory.createEnumEntry(
            startOffset = declaration.startOffset,
            endOffset = declaration.endOffset,
            origin = mapDeclarationOrigin(declaration.origin),
            name = symbolRenamer.getEnumEntryName(declaration.symbol),
            symbol = symbolRemapper.getDeclaredEnumEntry(declaration.symbol),
        ).apply {
            transformAnnotations(declaration)
            correspondingClass = declaration.correspondingClass?.transform()
            initializerExpression = declaration.initializerExpression?.transform()
        }

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer): IrAnonymousInitializer =
        declaration.factory.createAnonymousInitializer(
            declaration.startOffset, declaration.endOffset,
            mapDeclarationOrigin(declaration.origin),
            IrAnonymousInitializerSymbolImpl(declaration.descriptor)
        ).apply {
            transformAnnotations(declaration)
            body = declaration.body.transform()
        }

    override fun visitVariable(declaration: IrVariable): IrVariable =
        IrVariableImpl(
            declaration.startOffset, declaration.endOffset,
            mapDeclarationOrigin(declaration.origin),
            symbolRemapper.getDeclaredVariable(declaration.symbol),
            symbolRenamer.getVariableName(declaration.symbol),
            declaration.type.remapType(),
            declaration.isVar,
            declaration.isConst,
            declaration.isLateinit
        ).apply {
            transformAnnotations(declaration)
            initializer = declaration.initializer?.transform()
        }

    override fun visitTypeParameter(declaration: IrTypeParameter): IrTypeParameter =
        copyTypeParameter(declaration).apply {
            // TODO type parameter scopes?
            superTypes = declaration.superTypes.memoryOptimizedMap { it.remapType() }
        }

    private fun copyTypeParameter(declaration: IrTypeParameter): IrTypeParameter =
        declaration.factory.createTypeParameter(
            startOffset = declaration.startOffset,
            endOffset = declaration.endOffset,
            origin = mapDeclarationOrigin(declaration.origin),
            name = symbolRenamer.getTypeParameterName(declaration.symbol),
            symbol = symbolRemapper.getDeclaredTypeParameter(declaration.symbol),
            variance = declaration.variance,
            index = declaration.index,
            isReified = declaration.isReified,
        ).apply {
            transformAnnotations(declaration)
        }

    protected fun IrTypeParametersContainer.copyTypeParametersFrom(other: IrTypeParametersContainer) {
        this.typeParameters = other.typeParameters.memoryOptimizedMap {
            copyTypeParameter(it)
        }

        typeRemapper.withinScope(this) {
            for ((thisTypeParameter, otherTypeParameter) in this.typeParameters.zip(other.typeParameters)) {
                thisTypeParameter.superTypes = otherTypeParameter.superTypes.memoryOptimizedMap {
                    typeRemapper.remapType(it)
                }
            }
        }
    }

    override fun visitValueParameter(declaration: IrValueParameter): IrValueParameter =
        declaration.factory.createValueParameter(
            startOffset = declaration.startOffset,
            endOffset = declaration.endOffset,
            origin = mapDeclarationOrigin(declaration.origin),
            name = symbolRenamer.getValueParameterName(declaration.symbol),
            type = declaration.type.remapType(),
            isAssignable = declaration.isAssignable,
            symbol = symbolRemapper.getDeclaredValueParameter(declaration.symbol),
            index = declaration.index,
            varargElementType = declaration.varargElementType?.remapType(),
            isCrossinline = declaration.isCrossinline,
            isNoinline = declaration.isNoinline,
            isHidden = declaration.isHidden,
        ).apply {
            transformAnnotations(declaration)
            defaultValue = declaration.defaultValue?.transform()
        }

    override fun visitTypeAlias(declaration: IrTypeAlias): IrTypeAlias =
        declaration.factory.createTypeAlias(
            startOffset = declaration.startOffset,
            endOffset = declaration.endOffset,
            origin = mapDeclarationOrigin(declaration.origin),
            name = symbolRenamer.getTypeAliasName(declaration.symbol),
            visibility = declaration.visibility,
            symbol = symbolRemapper.getDeclaredTypeAlias(declaration.symbol),
            isActual = declaration.isActual,
            expandedType = declaration.expandedType.remapType(),
        ).apply {
            transformAnnotations(declaration)
            copyTypeParametersFrom(declaration)
        }

    override fun visitBody(body: IrBody): IrBody =
        throw IllegalArgumentException("Unsupported body type: $body")

    override fun visitExpressionBody(body: IrExpressionBody): IrExpressionBody =
        body.factory.createExpressionBody(body.expression.transform())

    override fun visitBlockBody(body: IrBlockBody): IrBlockBody =
        body.factory.createBlockBody(
            body.startOffset, body.endOffset,
            body.statements.memoryOptimizedMap { it.transform() }
        )

    override fun visitSyntheticBody(body: IrSyntheticBody): IrSyntheticBody =
        IrSyntheticBodyImpl(body.startOffset, body.endOffset, body.kind)

    override fun visitExpression(expression: IrExpression): IrExpression =
        throw IllegalArgumentException("Unsupported expression type: $expression")

    override fun visitConst(expression: IrConst<*>): IrConst<*> =
        expression.shallowCopy().processAttributes(expression)

    override fun visitConstantObject(expression: IrConstantObject): IrConstantValue =
        IrConstantObjectImpl(
            expression.startOffset, expression.endOffset,
            symbolRemapper.getReferencedConstructor(expression.constructor),
            expression.valueArguments.transform(),
            expression.typeArguments.memoryOptimizedMap { it.remapType() },
            expression.type.remapType()
        ).processAttributes(expression)

    override fun visitConstantPrimitive(expression: IrConstantPrimitive): IrConstantValue =
        IrConstantPrimitiveImpl(
            expression.startOffset, expression.endOffset,
            expression.value.transform()
        ).processAttributes(expression)

    override fun visitConstantArray(expression: IrConstantArray): IrConstantValue =
        IrConstantArrayImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            expression.elements.transform(),
        ).processAttributes(expression)

    override fun visitVararg(expression: IrVararg): IrVararg =
        IrVarargImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(), expression.varargElementType.remapType(),
            expression.elements.transform()
        ).processAttributes(expression)

    override fun visitSpreadElement(spread: IrSpreadElement): IrSpreadElement =
        IrSpreadElementImpl(
            spread.startOffset, spread.endOffset,
            spread.expression.transform()
        )

    override fun visitBlock(expression: IrBlock): IrBlock =
        if (expression is IrReturnableBlock)
            IrReturnableBlockImpl(
                expression.startOffset, expression.endOffset,
                expression.type.remapType(),
                symbolRemapper.getReferencedReturnableBlock(expression.symbol),
                mapStatementOrigin(expression.origin),
                expression.statements.memoryOptimizedMap { it.transform() }
            ).processAttributes(expression)
        else if (expression is IrInlinedFunctionBlock)
            IrInlinedFunctionBlockImpl(
                expression.startOffset, expression.endOffset,
                expression.type.remapType(),
                expression.inlineCall, expression.inlinedElement,
                mapStatementOrigin(expression.origin),
                statements = expression.statements.memoryOptimizedMap { it.transform() },
            ).processAttributes(expression)
        else
            IrBlockImpl(
                expression.startOffset, expression.endOffset,
                expression.type.remapType(),
                mapStatementOrigin(expression.origin),
                expression.statements.memoryOptimizedMap { it.transform() }
            ).processAttributes(expression)

    override fun visitComposite(expression: IrComposite): IrComposite =
        IrCompositeImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            mapStatementOrigin(expression.origin),
            expression.statements.memoryOptimizedMap { it.transform() }
        ).processAttributes(expression)

    override fun visitStringConcatenation(expression: IrStringConcatenation): IrStringConcatenation =
        IrStringConcatenationImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            expression.arguments.memoryOptimizedMap { it.transform() }
        ).processAttributes(expression)

    override fun visitGetObjectValue(expression: IrGetObjectValue): IrGetObjectValue =
        IrGetObjectValueImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            symbolRemapper.getReferencedClass(expression.symbol)
        ).processAttributes(expression)

    override fun visitGetEnumValue(expression: IrGetEnumValue): IrGetEnumValue =
        IrGetEnumValueImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            symbolRemapper.getReferencedEnumEntry(expression.symbol)
        ).processAttributes(expression)

    override fun visitGetValue(expression: IrGetValue): IrGetValue =
        IrGetValueImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            symbolRemapper.getReferencedValue(expression.symbol),
            mapStatementOrigin(expression.origin)
        ).processAttributes(expression)

    override fun visitSetValue(expression: IrSetValue): IrSetValue =
        IrSetValueImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            symbolRemapper.getReferencedValue(expression.symbol),
            expression.value.transform(),
            mapStatementOrigin(expression.origin)
        ).processAttributes(expression)

    override fun visitGetField(expression: IrGetField): IrGetField =
        IrGetFieldImpl(
            expression.startOffset, expression.endOffset,
            symbolRemapper.getReferencedField(expression.symbol),
            expression.type.remapType(),
            expression.receiver?.transform(),
            mapStatementOrigin(expression.origin),
            symbolRemapper.getReferencedClassOrNull(expression.superQualifierSymbol)
        ).processAttributes(expression)

    override fun visitSetField(expression: IrSetField): IrSetField =
        IrSetFieldImpl(
            expression.startOffset, expression.endOffset,
            symbolRemapper.getReferencedField(expression.symbol),
            expression.receiver?.transform(),
            expression.value.transform(),
            expression.type.remapType(),
            mapStatementOrigin(expression.origin),
            symbolRemapper.getReferencedClassOrNull(expression.superQualifierSymbol)
        ).processAttributes(expression)

    override fun visitCall(expression: IrCall): IrCall =
        shallowCopyCall(expression).apply {
            copyRemappedTypeArgumentsFrom(expression)
            transformValueArguments(expression)
        }

    override fun visitConstructorCall(expression: IrConstructorCall): IrConstructorCall {
        val constructorSymbol = symbolRemapper.getReferencedConstructor(expression.symbol)
        return IrConstructorCallImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            constructorSymbol,
            expression.typeArgumentsCount,
            expression.constructorTypeArgumentsCount,
            expression.valueArgumentsCount,
            mapStatementOrigin(expression.origin)
        ).apply {
            copyRemappedTypeArgumentsFrom(expression)
            transformValueArguments(expression)
        }.processAttributes(expression)
    }

    private fun IrMemberAccessExpression<*>.copyRemappedTypeArgumentsFrom(other: IrMemberAccessExpression<*>) {
        assert(typeArgumentsCount == other.typeArgumentsCount) {
            "Mismatching type arguments: $typeArgumentsCount vs ${other.typeArgumentsCount} "
        }
        for (i in 0 until typeArgumentsCount) {
            putTypeArgument(i, other.getTypeArgument(i)?.remapType())
        }
    }

    private fun shallowCopyCall(expression: IrCall): IrCall {
        val newCallee = symbolRemapper.getReferencedSimpleFunction(expression.symbol)
        return IrCallImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            newCallee,
            expression.typeArgumentsCount,
            expression.valueArgumentsCount,
            mapStatementOrigin(expression.origin),
            symbolRemapper.getReferencedClassOrNull(expression.superQualifierSymbol)
        ).apply {
            copyRemappedTypeArgumentsFrom(expression)
        }.processAttributes(expression)
    }

    private fun <T : IrMemberAccessExpression<*>> T.transformReceiverArguments(original: T): T =
        apply {
            dispatchReceiver = original.dispatchReceiver?.transform()
            extensionReceiver = original.extensionReceiver?.transform()
        }

    private fun <T : IrMemberAccessExpression<*>> T.transformValueArguments(original: T) {
        transformReceiverArguments(original)
        for (i in 0 until original.valueArgumentsCount) {
            putValueArgument(i, original.getValueArgument(i)?.transform())
        }
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrDelegatingConstructorCall {
        val newConstructor = symbolRemapper.getReferencedConstructor(expression.symbol)
        return IrDelegatingConstructorCallImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            newConstructor,
            expression.typeArgumentsCount,
            expression.valueArgumentsCount
        ).apply {
            copyRemappedTypeArgumentsFrom(expression)
            transformValueArguments(expression)
        }.processAttributes(expression)
    }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall): IrEnumConstructorCall {
        val newConstructor = symbolRemapper.getReferencedConstructor(expression.symbol)
        return IrEnumConstructorCallImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            newConstructor,
            expression.typeArgumentsCount,
            expression.valueArgumentsCount
        ).apply {
            copyRemappedTypeArgumentsFrom(expression)
            transformValueArguments(expression)
        }.processAttributes(expression)
    }

    override fun visitGetClass(expression: IrGetClass): IrGetClass =
        IrGetClassImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            expression.argument.transform()
        ).processAttributes(expression)

    override fun visitFunctionReference(expression: IrFunctionReference): IrFunctionReference {
        val symbol = symbolRemapper.getReferencedFunction(expression.symbol)
        val reflectionTarget = expression.reflectionTarget?.let { symbolRemapper.getReferencedFunction(it) }
        return IrFunctionReferenceImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            symbol,
            expression.typeArgumentsCount,
            expression.valueArgumentsCount,
            reflectionTarget,
            mapStatementOrigin(expression.origin)
        ).apply {
            copyRemappedTypeArgumentsFrom(expression)
            transformValueArguments(expression)
        }.processAttributes(expression)
    }

    override fun visitRawFunctionReference(expression: IrRawFunctionReference): IrRawFunctionReference {
        val symbol = symbolRemapper.getReferencedFunction(expression.symbol)
        return IrRawFunctionReferenceImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            symbol
        ).processAttributes(expression)
    }

    override fun visitPropertyReference(expression: IrPropertyReference): IrPropertyReference =
        IrPropertyReferenceImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            symbolRemapper.getReferencedProperty(expression.symbol),
            expression.typeArgumentsCount,
            expression.field?.let { symbolRemapper.getReferencedField(it) },
            expression.getter?.let { symbolRemapper.getReferencedSimpleFunction(it) },
            expression.setter?.let { symbolRemapper.getReferencedSimpleFunction(it) },
            mapStatementOrigin(expression.origin)
        ).apply {
            copyRemappedTypeArgumentsFrom(expression)
            transformReceiverArguments(expression)
        }.processAttributes(expression)

    override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference): IrLocalDelegatedPropertyReference =
        IrLocalDelegatedPropertyReferenceImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            symbolRemapper.getReferencedLocalDelegatedProperty(expression.symbol),
            symbolRemapper.getReferencedVariable(expression.delegate),
            symbolRemapper.getReferencedSimpleFunction(expression.getter),
            expression.setter?.let { symbolRemapper.getReferencedSimpleFunction(it) },
            mapStatementOrigin(expression.origin)
        ).processAttributes(expression)

    override fun visitFunctionExpression(expression: IrFunctionExpression): IrFunctionExpression =
        IrFunctionExpressionImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            expression.function.transform(),
            mapStatementOrigin(expression.origin)!!
        ).processAttributes(expression)

    override fun visitClassReference(expression: IrClassReference): IrClassReference =
        IrClassReferenceImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            symbolRemapper.getReferencedClassifier(expression.symbol),
            expression.classType.remapType()
        ).processAttributes(expression)

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall): IrInstanceInitializerCall =
        IrInstanceInitializerCallImpl(
            expression.startOffset, expression.endOffset,
            symbolRemapper.getReferencedClass(expression.classSymbol),
            expression.type.remapType()
        ).processAttributes(expression)

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrTypeOperatorCall =
        IrTypeOperatorCallImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            expression.operator,
            expression.typeOperand.remapType(),
            expression.argument.transform()
        ).processAttributes(expression)

    override fun visitWhen(expression: IrWhen): IrWhen =
        IrWhenImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            mapStatementOrigin(expression.origin),
            expression.branches.memoryOptimizedMap { it.transform() }
        ).processAttributes(expression)

    override fun visitBranch(branch: IrBranch): IrBranch =
        IrBranchImpl(
            branch.startOffset, branch.endOffset,
            branch.condition.transform(),
            branch.result.transform()
        )

    override fun visitElseBranch(branch: IrElseBranch): IrElseBranch =
        IrElseBranchImpl(
            branch.startOffset, branch.endOffset,
            branch.condition.transform(),
            branch.result.transform()
        )

    private val transformedLoops = HashMap<IrLoop, IrLoop>()

    private fun getTransformedLoop(irLoop: IrLoop): IrLoop =
        transformedLoops.getOrDefault(irLoop, irLoop)

    override fun visitWhileLoop(loop: IrWhileLoop): IrWhileLoop =
        IrWhileLoopImpl(loop.startOffset, loop.endOffset, loop.type.remapType(), mapStatementOrigin(loop.origin)).also { newLoop ->
            transformedLoops[loop] = newLoop
            newLoop.label = loop.label
            newLoop.condition = loop.condition.transform()
            newLoop.body = loop.body?.transform()
        }.processAttributes(loop)

    override fun visitDoWhileLoop(loop: IrDoWhileLoop): IrDoWhileLoop =
        IrDoWhileLoopImpl(loop.startOffset, loop.endOffset, loop.type.remapType(), mapStatementOrigin(loop.origin)).also { newLoop ->
            transformedLoops[loop] = newLoop
            newLoop.label = loop.label
            newLoop.condition = loop.condition.transform()
            newLoop.body = loop.body?.transform()
        }.processAttributes(loop)

    override fun visitBreak(jump: IrBreak): IrBreak =
        IrBreakImpl(
            jump.startOffset, jump.endOffset,
            jump.type.remapType(),
            getTransformedLoop(jump.loop)
        ).apply { label = jump.label }.processAttributes(jump)

    override fun visitContinue(jump: IrContinue): IrContinue =
        IrContinueImpl(
            jump.startOffset, jump.endOffset,
            jump.type.remapType(),
            getTransformedLoop(jump.loop)
        ).apply { label = jump.label }.processAttributes(jump)

    override fun visitTry(aTry: IrTry): IrTry =
        IrTryImpl(
            aTry.startOffset, aTry.endOffset,
            aTry.type.remapType(),
            aTry.tryResult.transform(),
            aTry.catches.memoryOptimizedMap { it.transform() },
            aTry.finallyExpression?.transform()
        ).processAttributes(aTry)

    override fun visitCatch(aCatch: IrCatch): IrCatch =
        IrCatchImpl(
            aCatch.startOffset, aCatch.endOffset,
            aCatch.catchParameter.transform(),
            aCatch.result.transform()
        )

    override fun visitReturn(expression: IrReturn): IrReturn =
        IrReturnImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            symbolRemapper.getReferencedReturnTarget(expression.returnTargetSymbol),
            expression.value.transform()
        ).processAttributes(expression)

    private fun SymbolRemapper.getReferencedReturnTarget(returnTarget: IrReturnTargetSymbol): IrReturnTargetSymbol =
        when (returnTarget) {
            is IrFunctionSymbol -> getReferencedFunction(returnTarget)
            is IrReturnableBlockSymbol -> getReferencedReturnableBlock(returnTarget)
        }

    override fun visitThrow(expression: IrThrow): IrThrow =
        IrThrowImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            expression.value.transform()
        ).processAttributes(expression)

    override fun visitDynamicOperatorExpression(expression: IrDynamicOperatorExpression): IrDynamicOperatorExpression =
        IrDynamicOperatorExpressionImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            expression.operator
        ).apply {
            receiver = expression.receiver.transform()
            expression.arguments.mapTo(arguments) { it.transform() }
        }.processAttributes(expression)

    override fun visitDynamicMemberExpression(expression: IrDynamicMemberExpression): IrDynamicMemberExpression =
        IrDynamicMemberExpressionImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            expression.memberName,
            expression.receiver.transform()
        ).processAttributes(expression)

    override fun visitErrorDeclaration(declaration: IrErrorDeclaration): IrErrorDeclaration =
        declaration.factory.createErrorDeclaration(declaration.startOffset, declaration.endOffset, declaration.descriptor)

    override fun visitErrorExpression(expression: IrErrorExpression): IrErrorExpression =
        IrErrorExpressionImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            expression.description
        ).processAttributes(expression)

    override fun visitErrorCallExpression(expression: IrErrorCallExpression): IrErrorCallExpression =
        IrErrorCallExpressionImpl(
            expression.startOffset, expression.endOffset,
            expression.type.remapType(),
            expression.description
        ).apply {
            explicitReceiver = expression.explicitReceiver?.transform()
            expression.arguments.transformTo(arguments)
        }.processAttributes(expression)
}
