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
 * @property all All the dependencies.
 *   If [stdlib] is not null, then it is expected as the very first element in [all].
 *   If [included] is not null, then it is expected as the very last element in [all].
 * @property stdlib The standard library dependency (if any).
 * @property included The "included library" dependency (if any).
 * @property fragmentNames The mapping from [IrModuleFragment] to its name. Used only in Kotlin/JS.
 */
data class IrModuleDependencies(
    val all: List<IrModuleFragment>,
    val stdlib: IrModuleFragment?,
    val included: IrModuleFragment?,
    val fragmentNames: Map<IrModuleFragment, String> = emptyMap()
)
