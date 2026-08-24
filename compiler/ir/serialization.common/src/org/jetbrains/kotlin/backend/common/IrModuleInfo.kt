/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.serialization.KotlinIrLinker
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.SymbolTable

/**
 * The representation of a module that is being compiled.
 */
data class IrModuleInfo(
    val module: IrModuleFragment,
    val dependencies: IrModuleDependencies,
    val bultins: IrBuiltIns,
    val symbolTable: SymbolTable,
    val deserializer: KotlinIrLinker,
)

/**
 * The dependencies of [IrModuleInfo]
 *
 * @property allDependencies All the dependencies.
 * @property fragmentNames The mapping from [IrModuleFragment] to its name. Used only in Kotlin/JS.
 */
data class IrModuleDependencies(
    val allDependencies: List<IrModuleFragment>,
    val fragmentNames: Map<IrModuleFragment, String> = emptyMap()
) {
    init {
        val extraFragments = fragmentNames.keys - allDependencies.toSet()
        require(extraFragments.isEmpty()) {
            buildString {
                appendLine("The set of module fragments in 'fragmentNames' is wider than in 'allDependencies'.")
                appendLine()
                appendLine("Extra fragments in 'fragmentNames' (${extraFragments.size}):")
                extraFragments.map { it.name }.sorted().joinTo(this, separator = "\n") { "- $it" }
            }
        }
    }
}
