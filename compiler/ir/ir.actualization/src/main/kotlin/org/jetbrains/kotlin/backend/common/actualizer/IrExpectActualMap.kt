/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.backend.common.actualizer.ExpectActualLinkCollector.MatchingContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.parentsWithSelf
import org.jetbrains.kotlin.resolve.calls.mpp.AbstractExpectActualChecker
import org.jetbrains.kotlin.utils.addToStdlib.CombinedMap
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

@OptIn(IrExpectActualMap.MappingForCheckers::class)
class IrExpectActualMap() {
    /**
     * This map contains only expect-actual mapping for declarations in sources.
     */
    @MappingForCheckers
    val expectToActual: Map<IrSymbol, IrSymbol>
        field = mutableMapOf()

    /**
     * This map contains expect-actual and common-platform mapping for declarations from dependencies
     * for HMPP compilation scheme
     */
    private val symbolMapFromContributor: MutableMap<IrSymbol, IrSymbol> = mutableMapOf()

    /**
     * This map contains the complete symbols mapping, which should be used for actualization
     */
    val symbolMap: Map<IrSymbol, IrSymbol> = CombinedMap(expectToActual, symbolMapFromContributor)

    /**
     * Direct means "not through typealias".
     * ClassId of expect and actual symbols are the same.
     * For every actual, it's possible to have multiple expects (because of `actual typealias`).
     * But only a single "direct" expect is possible.
     */
    val actualToDirectExpect: Map<IrSymbol, IrSymbol>
        field = mutableMapOf()

    val propertyAccessorsActualizedByFields: MutableMap<IrSimpleFunctionSymbol, IrPropertySymbol> = mutableMapOf()

    private var sourceDeclarationMappingMode = true

    fun putRegular(expectSymbol: IrSymbol, actualSymbol: IrSymbol): IrSymbol? {
        val destination = when {
            sourceDeclarationMappingMode -> expectToActual
            else -> symbolMapFromContributor
        }
        val registeredActual = destination.put(expectSymbol, actualSymbol)
        val expect = expectSymbol.owner
        val actual = actualSymbol.owner
        if (sourceDeclarationMappingMode &&
            expect is IrDeclaration && actual is IrDeclaration &&
            expect.parentsWithSelf.firstIsInstanceOrNull<IrClass>()?.classId ==
            actual.parentsWithSelf.firstIsInstanceOrNull<IrClass>()?.classId
        ) actualToDirectExpect.put(actualSymbol, expectSymbol)
        return registeredActual
    }

    internal fun fillAdditionalMapping(
        actualizerMapContributor: IrActualizerMapContributor,
        context: MatchingContext
    ) {
        sourceDeclarationMappingMode = false
        val classMapping = actualizerMapContributor.collectClassesMap().classMapping
        symbolMapFromContributor += classMapping
        for ((expectClass, actualClass) in classMapping) {
            // Here we call check for two classes only to match the scopes of these classes.
            // Abstraction of matching leaked into checking in this place :sad:
            AbstractExpectActualChecker.checkSingleExpectTopLevelDeclarationAgainstMatchedActual(
                expectClass,
                actualClass,
                context,
                context.languageVersionSettings,
            )
        }
        symbolMapFromContributor += actualizerMapContributor.collectTopLevelCallablesMap()
        sourceDeclarationMappingMode = true
    }

    @RequiresOptIn("This property should be used only in checkers. For mapping purposes use `symbolMap`")
    annotation class MappingForCheckers
}
