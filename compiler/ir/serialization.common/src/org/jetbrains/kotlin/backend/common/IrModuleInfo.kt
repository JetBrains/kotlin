/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.serialization.KotlinIrLinker
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.SymbolTable

data class IrModuleInfo(
    val module: IrModuleFragment,
    val dependencies: IrModuleDependencies,
    val bultins: IrBuiltIns,
    val symbolTable: SymbolTable,
    val deserializer: KotlinIrLinker,
)

data class IrModuleDependencies(
    val all: List<IrModuleFragment>,
    val stdlib: IrModuleFragment?,
    val included: IrModuleFragment?,
    val fragmentNames: Map<IrModuleFragment, String> = emptyMap()
)
