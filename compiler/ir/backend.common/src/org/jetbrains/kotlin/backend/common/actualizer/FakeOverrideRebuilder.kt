/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyDeclarationBase
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrLocalDelegatedPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.overrides.IrFakeOverrideBuilder
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import org.jetbrains.kotlin.utils.newHashSetWithExpectedSize

/**
 * After actualization, fake overrides can be incorrect.
 *
 * Fake overrides can change even in classes, which don't have expect/actuals in their superclasses.
 * And this change can be more non-trivial than just substituting expect class with actual.
 *
 * For example, if an expect class is actualized to a common class, or several expect classes are actualized to the same class,
 * several fake overrides can be merged with each other, or with real functions.
 *
 * To fix that, we are just rebuilding fake overrides from scratch, after actualization is done.
 *
 * This is done in 3 steps:
 * 1. Remove all fake overrides from all declarations, and remove their symbols from the symbol table.
 * 2. Build new fake overrides and map the old one to some function now existing in the class (which can also be a real function).
 * 3. Remap call-sites of all functions using the map collected on step 2.
 *
 * Steps 1 and 3 are kinda trivial, except we should remove all fake overrides from symbol table.
 *
 * For matching in step 2, classes are processed in such order, that the superclass is processed before each class.
 * This allows us to match using `overriddenSymbols`. In particular, f/o is mapped to only function
 * overriding any of functions (possibly already mapped), which was overridden by initial fake override.
 *
 * Here we assume that functions can only be merged, not split, i.e. if the same fake override was overriding several
 * super-functions, it's impossible to have different function overriding them after actualization.
 */
class FakeOverrideRebuilder(
    val symbolTable: SymbolTable,
    val fakeOverrideBuilder: IrFakeOverrideBuilder,
) {
    private val removedFakeOverrides = mutableMapOf<IrClassSymbol, List<IrSymbol>>()
    private val processedClasses = hashSetOf<IrClass>()

    // Map from the old fake override symbol to the new (rebuilt) symbol.
    private val fakeOverrideMap = hashMapOf<IrSymbol, IrSymbol>()


    fun rebuildFakeOverrides(irModule: IrModuleFragment) {
        irModule.acceptVoid(RemoveFakeOverridesVisitor(removedFakeOverrides, symbolTable))
        for (clazz in removedFakeOverrides.keys) {
            rebuildClassFakeOverrides(clazz.owner)
        }
        irModule.acceptVoid(RemapFakeOverridesVisitor(fakeOverrideMap))
    }

    private fun collectOverriddenDeclarations(
        member: IrOverridableDeclaration<*>,
        result: MutableSet<IrOverridableDeclaration<*>>,
        visited: MutableSet<IrOverridableDeclaration<*>>,
        stopAtReal: Boolean,
    ) {
        if (!visited.add(member)) return
        if (member.isReal) {
            result.add(member)
            if (stopAtReal) return
        } else {
            require(member.overriddenSymbols.isNotEmpty()) { "No overridden symbols found for (fake override) ${member.render()}" }
        }
        for (overridden in member.overriddenSymbols) {
            collectOverriddenDeclarations(overridden.owner as IrOverridableDeclaration<*>, result, visited, stopAtReal)
        }
    }

    private fun IrOverridableDeclaration<*>.getOverriddenDeclarations(stopAtReal: Boolean): Set<IrOverridableDeclaration<*>> =
        mutableSetOf<IrOverridableDeclaration<*>>().also { result ->
            collectOverriddenDeclarations(this, result, hashSetOf(), stopAtReal)
        }

    private fun MutableMap<IrSymbol, IrSymbol>.processDeclaration(declaration: IrOverridableDeclaration<*>, irClass: IrClass) {
        for (overridden in declaration.getOverriddenDeclarations(false)) {
            val previousSymbol = put(overridden.symbol, declaration.symbol)
            if (previousSymbol != null) {
                val previous = previousSymbol.owner as IrDeclaration
                error(
                    "Multiple overrides in class ${irClass.fqNameWhenAvailable} for ${overridden.render()}:\n" +
                            "  previous: ${previous.render()}\n" +
                            "            declared in ${previous.parentAsClass.fqNameWhenAvailable}\n" +
                            "       new: ${declaration.render()}\n" +
                            "            declared in ${declaration.parentAsClass.fqNameWhenAvailable}"
                )
            }
        }
    }

    private fun rebuildClassFakeOverrides(irClass: IrClass) {
        if (irClass is IrLazyDeclarationBase) return
        if (!processedClasses.add(irClass)) return
        val oldList = removedFakeOverrides[irClass.symbol] ?: return
        for (c in irClass.superTypes) {
            c.getClass()?.let { rebuildClassFakeOverrides(it) }
        }
        fakeOverrideBuilder.buildFakeOverridesForClass(irClass, false)

        val overriddenMap = mutableMapOf<IrSymbol, IrSymbol>()

        for (declaration in irClass.declarations) {
            when (declaration) {
                is IrSimpleFunction -> overriddenMap.processDeclaration(declaration, irClass)
                is IrProperty -> {
                    overriddenMap.processDeclaration(declaration, irClass)
                    declaration.getter?.let { overriddenMap.processDeclaration(it, irClass) }
                    declaration.setter?.let { overriddenMap.processDeclaration(it, irClass) }
                }
            }
        }

        for (old in oldList) {
            val overridden = mutableSetOf<IrOverridableDeclaration<*>>()
            collectOverriddenDeclarations(old.owner as IrOverridableDeclaration<*>, overridden, hashSetOf(), true)
            val newSymbols = overridden.mapTo(newHashSetWithExpectedSize(1)) {
                overriddenMap[it.symbol]
                    ?: error("No new fake override recorded for declaration in class ${irClass.fqNameWhenAvailable}: ${it.render()}")
            }
            when (newSymbols.size) {
                0 -> error("No overridden found for declaration in class ${irClass.fqNameWhenAvailable}: ${old.owner.render()}")
                1 -> fakeOverrideMap[old] = newSymbols.single()
                else -> error(
                    "Multiple overridden found for declaration in class ${irClass.fqNameWhenAvailable}: ${old.owner.render()}\n" +
                            newSymbols.joinToString("\n") {
                                "  - ${it.owner.render()} (declared in ${(it.owner as IrDeclaration).parentAsClass.fqNameWhenAvailable}"
                            }
                )
            }
        }
    }
}

private class RemoveFakeOverridesVisitor(
    val removedOverrides: MutableMap<IrClassSymbol, List<IrSymbol>>,
    val symbolTable: SymbolTable
) : IrElementVisitorVoid {

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    @OptIn(DelicateSymbolTableApi::class)
    override fun visitClass(declaration: IrClass) {
        val curList = mutableListOf<IrSymbol>()
        declaration.declarations.removeIf {
            if (it is IrOverridableDeclaration<*> && it.isFakeOverride) {
                when (it) {
                    is IrProperty -> {
                        curList.add(it.symbol)
                        symbolTable.removeProperty(it.symbol)
                        it.getter?.symbol?.let { getter ->
                            curList.add(getter)
                            symbolTable.removeSimpleFunction(getter)
                        }
                        it.setter?.symbol?.let { setter ->
                            curList.add(setter)
                            symbolTable.removeSimpleFunction(setter)
                        }
                    }
                    is IrSimpleFunction -> {
                        curList.add(it.symbol)
                        symbolTable.removeSimpleFunction(it.symbol)
                    }
                    else -> shouldNotBeCalled("Only simple functions and properties can be overridden")
                }
                true
            } else {
                false
            }
        }
        if (curList.isNotEmpty()) {
            removedOverrides[declaration.symbol] = curList
        }
        declaration.acceptChildrenVoid(this)
    }
}


// TODO: KT-61561 it seems this class is too generic to be here.
// For some reason, we don't have utility class doing that, but we probably should, as we have DeepCopy util classes
// Probably, it should also be generated, to avoid missing some places where symbols can happen.
private class RemapFakeOverridesVisitor(val fakeOverridesMap: Map<IrSymbol, IrSymbol>) : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        declaration.overriddenSymbols = declaration.overriddenSymbols.map {
            (fakeOverridesMap[it] as? IrSimpleFunctionSymbol) ?: it
        }
        declaration.acceptChildrenVoid(this)
    }

    override fun visitProperty(declaration: IrProperty) {
        declaration.overriddenSymbols = declaration.overriddenSymbols.map {
            (fakeOverridesMap[it] as? IrPropertySymbol) ?: it
        }
        declaration.acceptChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall) {
        expression.symbol = (fakeOverridesMap[expression.symbol] as? IrSimpleFunctionSymbol) ?: expression.symbol
        expression.acceptChildrenVoid(this)
    }

    override fun visitFunctionReference(expression: IrFunctionReference) {
        expression.symbol = (fakeOverridesMap[expression.symbol] as? IrSimpleFunctionSymbol) ?: expression.symbol
        expression.acceptChildrenVoid(this)
    }

    override fun visitPropertyReference(expression: IrPropertyReference) {
        expression.symbol = (fakeOverridesMap[expression.symbol] as? IrPropertySymbol) ?: expression.symbol
        expression.getter = (expression.getter?.let { fakeOverridesMap[it] } as? IrSimpleFunctionSymbol) ?: expression.getter
        expression.setter = (expression.setter?.let { fakeOverridesMap[it] } as? IrSimpleFunctionSymbol) ?: expression.setter
        expression.acceptChildrenVoid(this)
    }

    override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference) {
        expression.getter = (fakeOverridesMap[expression.getter] as? IrSimpleFunctionSymbol) ?: expression.getter
        expression.setter = (expression.setter?.let { fakeOverridesMap[it] } as? IrSimpleFunctionSymbol) ?: expression.setter
        expression.acceptChildrenVoid(this)
    }
}