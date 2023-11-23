/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrLocalDelegatedPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFakeOverrideSymbolBase
import org.jetbrains.kotlin.ir.util.SymbolRemapper
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

/**
 * This class provides utility to resolve [org.jetbrains.kotlin.ir.symbols.impl.IrFunctionFakeOverrideSymbol]
 * and [org.jetbrains.kotlin.ir.symbols.impl.IrPropertyFakeOverrideSymbol] to normal symbols.
 *
 * It can be used after classifiers are actualized and fake overrides are built.
 *
 * Conceptually, a fake override symbol is a pair of real symbol and class in which we need to find this fake override.
 *
 * When the first remapping request comes for a class, all its supertypes are traversed recursively and for all declarations inside,
 * all overrides are cached.
 *
 * This approach is quadratic over the height of class hierarchy. Unfortunately, we need all overrides, not only direct ones
 * (and not only direct-real ones). Because some intermediate overrides can appear in the process of actualization,
 * and we can't guarantee that all real symbols are some specific preferred overrides, as they were right after Fir2Ir.
 *
 */
class SpecialFakeOverrideSymbolsResolver(private val expectActualMap: Map<IrSymbol, IrSymbol>) : SymbolRemapper.Empty() {
    /**
     * Map from (class, declaration) -> declarationInsideClass
     *
     * Means that declarationInsideClass is the one overriding this declaration in this class.
     * [processClass] function add all valid pairs for this class to the map.
     */
    private val cachedFakeOverrides = mutableMapOf<Pair<IrClassSymbol, IrSymbol>, IrSymbol>()
    private val processedClasses = mutableSetOf<IrClass>()

    override fun getReferencedFunction(symbol: IrFunctionSymbol): IrFunctionSymbol {
        return symbol.remap()
    }

    override fun getReferencedSimpleFunction(symbol: IrSimpleFunctionSymbol): IrSimpleFunctionSymbol {
        return symbol.remap()
    }

    override fun getReferencedProperty(symbol: IrPropertySymbol): IrPropertySymbol {
        return symbol.remap()
    }

    private inline fun <reified S : IrSymbol> S.remap(): S {
        if (this !is IrFakeOverrideSymbolBase<*, *, *>) {
            return this
        }
        val actualizedClassSymbol = containingClassSymbol.actualize()
        val actualizedOriginalSymbol = originalSymbol.actualize()
        processClass(actualizedClassSymbol.owner)
        when (val result = cachedFakeOverrides[actualizedClassSymbol to actualizedOriginalSymbol]) {
            null -> error("No override for $actualizedOriginalSymbol in $actualizedClassSymbol")
            !is S -> error("Override for $actualizedOriginalSymbol in $actualizedClassSymbol has incompatible type: $result")
            else -> return result
        }
    }

    private fun IrClassSymbol.actualize(): IrClassSymbol {
        return (this as IrSymbol).actualize() as IrClassSymbol
    }

    private fun IrSymbol.actualize(): IrSymbol {
        return expectActualMap[this] ?: this
    }

    private fun processClass(irClass: IrClass) {
        require(!irClass.isExpect) { "There should be no references to expect classes at this point" }
        if (!processedClasses.add(irClass)) return
        for (declaration in irClass.declarations) {
            if (declaration !is IrOverridableDeclaration<*>) continue
            processDeclaration(irClass.symbol, declaration)
            if (declaration is IrProperty) {
                declaration.getter?.let { processDeclaration(irClass.symbol, it) }
                declaration.setter?.let { processDeclaration(irClass.symbol, it) }
            }
        }
    }

    private fun processDeclaration(classSymbol: IrClassSymbol, declaration: IrOverridableDeclaration<*>) {
        for (overridden in declaration.collectOverrides(mutableSetOf())) {
            cachedFakeOverrides[classSymbol to overridden] = declaration.symbol
        }
    }

    private fun IrOverridableDeclaration<*>.collectOverrides(visited: MutableSet<IrSymbol>): Sequence<IrSymbol> = sequence {
        if (visited.add(symbol)) {
            if (!isFakeOverride) {
                yield(symbol)
            }
            for (overridden in overriddenSymbols) {
                yieldAll((overridden.remap().owner as IrOverridableDeclaration<*>).collectOverrides(visited))
            }
        }
    }
}


class SpecialFakeOverrideSymbolsResolverVisitor(private val resolver: SpecialFakeOverrideSymbolsResolver) : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        declaration.overriddenSymbols = declaration.overriddenSymbols.map(resolver::getReferencedSimpleFunction)
        declaration.acceptChildrenVoid(this)
    }

    override fun visitProperty(declaration: IrProperty) {
        declaration.overriddenSymbols = declaration.overriddenSymbols.map(resolver::getReferencedProperty)
        declaration.acceptChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall) {
        expression.symbol = resolver.getReferencedSimpleFunction(expression.symbol)
        expression.acceptChildrenVoid(this)
    }

    override fun visitFunctionReference(expression: IrFunctionReference) {
        expression.symbol = resolver.getReferencedFunction(expression.symbol)
        expression.reflectionTarget = expression.reflectionTarget?.let(resolver::getReferencedFunction)
        expression.acceptChildrenVoid(this)
    }

    override fun visitPropertyReference(expression: IrPropertyReference) {
        expression.symbol = expression.symbol.let(resolver::getReferencedProperty)
        expression.getter = expression.getter?.let(resolver::getReferencedSimpleFunction)
        expression.setter = expression.setter?.let(resolver::getReferencedSimpleFunction)
        expression.acceptChildrenVoid(this)
    }

    override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference) {
        expression.getter = expression.getter.let(resolver::getReferencedSimpleFunction)
        expression.setter = expression.setter?.let(resolver::getReferencedSimpleFunction)
        expression.acceptChildrenVoid(this)
    }
}
