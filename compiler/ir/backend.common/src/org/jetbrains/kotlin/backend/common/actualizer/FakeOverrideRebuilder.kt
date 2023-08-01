/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageSupportForLinker
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideBuilder
import org.jetbrains.kotlin.backend.common.overrides.FileLocalAwareLinker
import org.jetbrains.kotlin.backend.common.serialization.CompatibilityMode
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyDeclarationBase
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrLocalDelegatedPropertyReference
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertySymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled


/**
 * After actualization, fake overrides can be incorrect.
 *
 * Fake overrides can change even in classes, which don't have expect/actuals in their superclasses.
 * And this change can be more untrivial than just substitute expect class with actual.
 *
 * For example, if an expect class is actualized to common class, or several expect classes are actualized to same class,
 * several fake overrides can be merged with each other, or with real functions.
 *
 * To fix that, we are just rebuilding fake overrides from stretch, after actualization is done.
 *
 * This is done in 3 steps:
 * 1. Remove all fake overrides from all classes
 * 2. Build new fake overrides and map old one to some function now existing in class (possible real function, not fake override)
 * 3. Remap callsites of all functions using the map collected on step 2.
 *
 * Steps 1 and 3 are kinda trivial, expect we should remove all fake overrides from symbol table.
 *
 * For matching in step 2, classes are processed in such order. That superclass is processed before class.
 * That allows us to match using overridden symbols. In particular, f/o is mapped to only function
 * overriding any of functions (possibly already mapped), which was overriden by initial fake override.
 *
 * Here we assume that functions can be only merged, not split, i.e. if the same fake override was overriding several
 * super-functions, it's impossible to have different function overriding them after actualization.
 */
class FakeOverrideRebuilder(
    val symbolTable: SymbolTable,
    val mangler: KotlinMangler.IrMangler,
    val typeSystemContext: IrTypeSystemContext,
    val irModule: IrModuleFragment,
    // TODO: drop this argument in favor of using [IrModuleDescriptor::shouldSeeInternalsOf] in FakeOverrideBuilder KT-61384
    friendModules: Map<String, List<String>>
) {
    private val removedFakeOverrides = mutableMapOf<IrClassSymbol, List<IrSymbol>>()
    private val processedClasses = mutableSetOf<IrClass>()
    private val fakeOverrideMap = mutableMapOf<IrSymbol, IrSymbol>()

    private val fakeOverrideBuilder = FakeOverrideBuilder(
        LocalFakeOverridesStorage(),
        symbolTable,
        mangler,
        typeSystemContext,
        friendModules,
        PartialLinkageSupportForLinker.DISABLED
    )

    fun rebuildFakeOverrides() {
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

    private fun rebuildClassFakeOverrides(irClass: IrClass) {
        if (irClass is IrLazyDeclarationBase) return
        if (!processedClasses.add(irClass)) return
        val oldList = removedFakeOverrides[irClass.symbol] ?: return
        for (c in irClass.superTypes) {
            c.getClass()?.let { rebuildClassFakeOverrides(it) }
        }
        fakeOverrideBuilder.provideFakeOverrides(irClass, CompatibilityMode.CURRENT)
        val overriddenMap = mutableMapOf<IrSymbol, IrSymbol>()
        irClass.properties
            .forEach { prop ->
                val allOverridden = mutableSetOf<IrOverridableDeclaration<*>>()
                collectOverriddenDeclarations(prop, allOverridden, mutableSetOf(), false)
                for (overridden in allOverridden) {
                    require(overriddenMap.put(overridden.symbol, prop.symbol) == null) { "Multiple overrides for ${overridden.render()} "}
                }
            }
        irClass.simpleFunctions()
            .forEach { func ->
                val allOverridden = mutableSetOf<IrOverridableDeclaration<*>>()
                collectOverriddenDeclarations(func, allOverridden, mutableSetOf(), false)
                for (overridden in allOverridden) {
                    require(overriddenMap.put(overridden.symbol, func.symbol) == null) { "Multiple overrides for ${overridden.render()} "}
                }
            }
        for (old in oldList) {
            fakeOverrideMap[old] = mutableSetOf<IrOverridableDeclaration<*>>()
                .apply { collectOverriddenDeclarations(old.owner as IrOverridableDeclaration<*>, this, mutableSetOf(), true) }
                .map { overriddenMap[it.symbol] }
                .distinct()
                .single() ?: error("${old.owner.render()} is not overridden in ${irClass.render()}")
        }
    }
}

private class LocalFakeOverridesStorage : FileLocalAwareLinker {
    val funStorage = mutableMapOf<Pair<IrDeclaration, IdSignature>, IrSimpleFunctionSymbolImpl>()
    val propertyStorage = mutableMapOf<Pair<IrDeclaration, IdSignature>, IrPropertySymbol>()
    override fun tryReferencingSimpleFunctionByLocalSignature(
        parent: IrDeclaration,
        idSignature: IdSignature,
    ): IrSimpleFunctionSymbol? {
        if (idSignature.isPubliclyVisible) return null
        return funStorage.getOrPut(parent to idSignature) {
            IrSimpleFunctionSymbolImpl()
        }
    }

    override fun tryReferencingPropertyByLocalSignature(parent: IrDeclaration, idSignature: IdSignature): IrPropertySymbol? {
        if (idSignature.isPubliclyVisible) return null
        return propertyStorage.getOrPut(parent to idSignature) {
            IrPropertySymbolImpl()
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


// TODO: KT-61561 it seams, this class is too generic to be here.
// By some reason, we don't have utility class doing that, but we probably should, as we have DeepCopy util classes
// Probably, it should also be generated, to avoid missing some plaves, where symbols can happen.
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