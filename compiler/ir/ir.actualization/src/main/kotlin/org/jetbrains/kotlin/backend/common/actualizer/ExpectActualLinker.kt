/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldFakeOverrideSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFunctionFakeOverrideSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertyFakeOverrideSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.util.DeepCopyIrTreeWithSymbols
import org.jetbrains.kotlin.ir.util.SymbolRemapper
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.utils.memoryOptimizedMap
import org.jetbrains.kotlin.utils.memoryOptimizedMapNotNull
import org.jetbrains.kotlin.utils.setSize

internal class ActualizerSymbolRemapper(private val expectActualMap: IrExpectActualMap) : SymbolRemapper.Empty() {
    override fun getReferencedClass(symbol: IrClassSymbol): IrClassSymbol = symbol.actualizeSymbol()

    override fun getReferencedScript(symbol: IrScriptSymbol): IrScriptSymbol = symbol.actualizeSymbol()

    override fun getReferencedEnumEntry(symbol: IrEnumEntrySymbol): IrEnumEntrySymbol = symbol.actualizeSymbol()

    override fun getReferencedVariable(symbol: IrVariableSymbol): IrVariableSymbol = symbol.actualizeSymbol()

    override fun getReferencedLocalDelegatedProperty(symbol: IrLocalDelegatedPropertySymbol): IrLocalDelegatedPropertySymbol = symbol.actualizeSymbol()

    override fun getReferencedField(symbol: IrFieldSymbol): IrFieldSymbol = symbol.actualizeMaybeFakeOverrideSymbol()

    override fun getReferencedConstructor(symbol: IrConstructorSymbol): IrConstructorSymbol = symbol.actualizeSymbol()

    override fun getReferencedValue(symbol: IrValueSymbol): IrValueSymbol = symbol.actualizeSymbol()

    override fun getReferencedValueParameter(symbol: IrValueParameterSymbol): IrValueSymbol = symbol.actualizeSymbol<IrValueSymbol>()

    override fun getReferencedFunction(symbol: IrFunctionSymbol): IrFunctionSymbol = symbol.actualizeMaybeFakeOverrideSymbol()

    override fun getReferencedProperty(symbol: IrPropertySymbol): IrPropertySymbol = symbol.actualizeMaybeFakeOverrideSymbol()

    override fun getReferencedSimpleFunction(symbol: IrSimpleFunctionSymbol): IrSimpleFunctionSymbol = symbol.actualizeMaybeFakeOverrideSymbol()

    override fun getReferencedClassifier(symbol: IrClassifierSymbol): IrClassifierSymbol = symbol.actualizeSymbol()

    override fun getReferencedTypeParameter(symbol: IrTypeParameterSymbol): IrClassifierSymbol = symbol.actualizeSymbol<IrClassifierSymbol>()

    override fun getReferencedReturnTarget(symbol: IrReturnTargetSymbol): IrReturnTargetSymbol = symbol.actualizeSymbol()

    override fun getReferencedReturnableBlock(symbol: IrReturnableBlockSymbol): IrReturnTargetSymbol = symbol.actualizeSymbol<IrReturnTargetSymbol>()

    private inline fun <reified S : IrSymbol> S.actualizeMaybeFakeOverrideSymbol(): S {
        val actualizedSymbol = this.actualizeSymbol()
        return when (actualizedSymbol) {
            is IrFunctionFakeOverrideSymbol -> IrFunctionFakeOverrideSymbol(
                originalSymbol = actualizedSymbol.originalSymbol.actualizeSymbol(),
                containingClassSymbol = actualizedSymbol.containingClassSymbol.actualizeSymbol(),
                idSignature = null
            )
            is IrPropertyFakeOverrideSymbol -> IrPropertyFakeOverrideSymbol(
                originalSymbol = actualizedSymbol.originalSymbol.actualizeSymbol(),
                containingClassSymbol = actualizedSymbol.containingClassSymbol.actualizeSymbol(),
                idSignature = null
            )
            is IrFieldFakeOverrideSymbol -> IrFieldFakeOverrideSymbol(
                originalSymbol = actualizedSymbol.originalSymbol.actualizeSymbol(),
                containingClassSymbol = actualizedSymbol.containingClassSymbol.actualizeSymbol(),
                idSignature = null,
                correspondingPropertySymbol = getReferencedProperty(actualizedSymbol.correspondingPropertySymbol)
            )
            else -> actualizedSymbol
        } as S
    }

    private inline fun <reified S : IrSymbol> S.actualizeSymbol(): S {
        val actualSymbol = expectActualMap.symbolMap[this] ?: return this
        return actualSymbol as? S
            ?: error("Unexpected type of actual symbol. Expected: ${S::class.java.simpleName}, got ${actualSymbol.javaClass.simpleName}")
    }
}

internal open class ActualizerVisitor(
    private val symbolRemapper: SymbolRemapper,
    private val membersActualization: Boolean,
) : DeepCopyIrTreeWithSymbols(symbolRemapper) {
    // All callables inside an expect declaration marked with `@OptionalExpectation` annotation should be actualized anyway.
    private var insideDeclarationWithOptionalExpectation = false

    // We shouldn't touch attributes, because Fir2Ir wouldn't set them to anything meaningful anyway.
    // So it would be better to have them as is, i.e. referring to `this`, not some random node removed from the tree
    override fun <D : IrElement> D.processAttributes(other: IrElement) {}

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment =
        declaration.also { it.transformChildren(this, null) }

    override fun visitExternalPackageFragment(declaration: IrExternalPackageFragment): IrExternalPackageFragment =
        declaration.also { it.transformChildren(this, null) }

    override fun visitFile(declaration: IrFile): IrFile =
        declaration.also {
            it.transformChildren(this, null)
            it.actualizeAnnotations()
        }

    override fun visitScript(declaration: IrScript): IrScript =
        declaration.also {
            it.baseClass = it.baseClass?.remapType()
            it.transformChildren(this, null)
        }

    override fun visitClass(declaration: IrClass): IrClass =
        declaration.also {
            val oldInsideDeclarationWithOptionalExpectation = insideDeclarationWithOptionalExpectation
            insideDeclarationWithOptionalExpectation =
                oldInsideDeclarationWithOptionalExpectation || declaration.containsOptionalExpectation()
            if (declaration.isExpect && !insideDeclarationWithOptionalExpectation) return@also
            it.superTypes = it.superTypes.map { superType -> superType.remapType() }
            it.transformChildren(this, null)
            it.actualizeAnnotations()
            it.valueClassRepresentation = it.valueClassRepresentation?.mapUnderlyingType { type ->
                type.remapType() as? IrSimpleType ?: error("Value class underlying type is not a simple type: ${it.render()}")
            }
            insideDeclarationWithOptionalExpectation = oldInsideDeclarationWithOptionalExpectation
        }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrSimpleFunction =
        (visitFunction(declaration) as IrSimpleFunction).also {
            if (declaration.isExpect && !insideDeclarationWithOptionalExpectation) return@also
            it.overriddenSymbols = it.overriddenSymbols.memoryOptimizedMap { symbol ->
                symbolRemapper.getReferencedFunction(symbol) as IrSimpleFunctionSymbol
            }
        }

    override fun visitConstructor(declaration: IrConstructor): IrConstructor = visitFunction(declaration) as IrConstructor

    override fun visitFunction(declaration: IrFunction): IrFunction =
        declaration.also {
            if (declaration.isExpect && !insideDeclarationWithOptionalExpectation) return@also
            it.returnType = it.returnType.remapType()
            it.transformChildren(this, null)
            it.actualizeAnnotations()
        }

    override fun visitProperty(declaration: IrProperty): IrProperty =
        declaration.also {
            if (declaration.isExpect && !insideDeclarationWithOptionalExpectation) return@also
            it.transformChildren(this, null)
            it.overriddenSymbols = it.overriddenSymbols.memoryOptimizedMap { symbol ->
                symbolRemapper.getReferencedProperty(symbol)
            }
            it.actualizeAnnotations()
        }

    override fun visitField(declaration: IrField): IrField =
        declaration.also {
            it.type = it.type.remapType()
            it.transformChildren(this, null)
            it.actualizeAnnotations()
        }

    override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrLocalDelegatedProperty =
        declaration.also {
            it.type = it.type.remapType()
            it.transformChildren(this, null)
        }

    override fun visitEnumEntry(declaration: IrEnumEntry): IrEnumEntry =
        declaration.also {
            it.transformChildren(this, null)
            it.actualizeAnnotations()
        }

    override fun visitTypeParameter(declaration: IrTypeParameter): IrTypeParameter =
        declaration.also {
            it.superTypes = it.superTypes.map { superType -> superType.remapType() }
            it.transformChildren(this, null)
            it.actualizeAnnotations()
        }

    override fun visitValueParameter(declaration: IrValueParameter): IrValueParameter =
        declaration.also {
            it.type = it.type.remapType()
            it.varargElementType = it.varargElementType?.remapType()
            it.transformChildren(this, null)
            it.actualizeAnnotations()
        }

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer): IrAnonymousInitializer =
        declaration.also { it.transformChildren(this, null) }

    override fun visitVariable(declaration: IrVariable): IrVariable =
        declaration.also {
            it.type = it.type.remapType()
            it.transformChildren(this, null)
            it.actualizeAnnotations()
        }

    override fun visitTypeAlias(declaration: IrTypeAlias): IrTypeAlias =
        declaration.also {
            it.expandedType = it.expandedType.remapType()
            it.transformChildren(this, null)
            it.actualizeAnnotations()
        }

    override fun visitConstructorCall(expression: IrConstructorCall): IrConstructorCall {
        val constructorSymbol = symbolRemapper.getReferencedConstructor(expression.symbol)

        return IrConstructorCallImpl(
            expression.startOffset,
            expression.endOffset,
            expression.type.remapType(),
            constructorSymbol,
            expression.typeArguments.size,
            expression.constructorTypeArgumentsCount,
            expression.origin,
        ).apply {
            copyRemappedTypeArgumentsFrom(expression)
            transformValueArguments(expression)
            processAttributes(expression)

            // This is a hack to allow actualizing annotation constructors without parameters with constructors with default arguments.
            // Without it, attempting to call such a constructor in common code will result in either a backend exception or in linkage error.
            // See KT-67488 for details.
            if (constructorSymbol.isBound) {
                arguments.setSize(constructorSymbol.owner.parameters.size)
            }
        }
    }

    /**
     * Actualizes annotation calls and removes optional expectation annotations which don't have an actual pair
     */
    private fun IrMutableAnnotationContainer.actualizeAnnotations() {
        transformAnnotations(this)
        if (!membersActualization) return
        val newAnnotations = annotations.memoryOptimizedMapNotNull { annotation ->
            val annotationClass = annotation.symbol.owner.constructedClass
            when {
                annotationClass.isExpect && annotationClass.containsOptionalExpectation() -> null
                else -> annotation
            }
        }
        if (newAnnotations.size != annotations.size) {
            annotations = newAnnotations
        }
    }
}
