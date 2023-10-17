/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.jvm

import org.jetbrains.kotlin.backend.common.ir.SharedVariablesManager
import org.jetbrains.kotlin.bir.BirBuiltIns
import org.jetbrains.kotlin.bir.backend.BirBackendContext
import org.jetbrains.kotlin.bir.backend.BirBuiltInSymbols
import org.jetbrains.kotlin.bir.backend.BirLoweringPhase
import org.jetbrains.kotlin.bir.types.BirTypeSystemContext
import org.jetbrains.kotlin.bir.types.BirTypeSystemContextImpl
import org.jetbrains.kotlin.bir.util.Ir2BirConverter
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.name.FqName

@OptIn(ObsoleteDescriptorBasedAPI::class)
class JvmBirBackendContext @OptIn(ObsoleteDescriptorBasedAPI::class) constructor(
    override val builtIns: KotlinBuiltIns,
    irBuiltIns: IrBuiltIns,
    symbolTable: SymbolTable,
    module: ModuleDescriptor,
    override val configuration: CompilerConfiguration,
    converter: Ir2BirConverter,
    phaseConfig: List<(JvmBirBackendContext) -> BirLoweringPhase>,
) : BirBackendContext() {
    init {
        converter.birForest = compiledBir
    }

    override val birBuiltIns: BirBuiltIns = BirBuiltIns(irBuiltIns, converter)
    override val typeSystem: BirTypeSystemContext = BirTypeSystemContextImpl(birBuiltIns, compiledBir)

    val loweringPhases = phaseConfig.map { it(this) }

    override val internalPackageFqn = FqName("kotlin.jvm")
    override val sharedVariablesManager: SharedVariablesManager
        get() = TODO("Not yet implemented")
    override val builtInSymbols: BirBuiltInSymbols
        get() = TODO("Not yet implemented")
}