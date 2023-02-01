/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.backend.common.ir.isProperExpect
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.name.FqName

object IrActualizer {
    fun actualize(mainFragment: IrModuleFragment, dependentFragments: List<IrModuleFragment>) {
        val (expectActualMap, typeAliasMap) = ExpectActualCollector(mainFragment, dependentFragments).collect()
        removeExpectDeclaration(dependentFragments) // TODO: consider removing this call. See ExpectDeclarationRemover.kt
        addMissingFakeOverrides(expectActualMap, dependentFragments, typeAliasMap)
        linkExpectToActual(expectActualMap, dependentFragments)
        mergeIrFragments(mainFragment, dependentFragments)
    }

    private fun removeExpectDeclaration(dependentFragments: List<IrModuleFragment>) {
        for (fragment in dependentFragments) {
            for (file in fragment.files) {
                file.declarations.removeAll { it.isProperExpect }
            }
        }
    }

    private fun addMissingFakeOverrides(
        expectActualMap: Map<IrSymbol, IrSymbol>,
        dependentFragments: List<IrModuleFragment>,
        typeAliasMap: Map<FqName, FqName>
    ) {
        MissingFakeOverridesAdder(expectActualMap, typeAliasMap).apply { dependentFragments.forEach { visitModuleFragment(it) } }
    }

    private fun linkExpectToActual(expectActualMap: Map<IrSymbol, IrSymbol>, dependentFragments: List<IrModuleFragment>) {
        ExpectActualLinker(expectActualMap).apply { dependentFragments.forEach { actualize(it) } }
    }

    private fun mergeIrFragments(mainFragment: IrModuleFragment, dependentFragments: List<IrModuleFragment>) {
        mainFragment.files.addAll(0, dependentFragments.flatMap { it.files })
    }
}