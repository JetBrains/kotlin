/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.jvm

import org.jetbrains.kotlin.backend.common.ir.SharedVariablesManager
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.bir.BirBuiltIns
import org.jetbrains.kotlin.bir.BirDynamicPropertiesManager
import org.jetbrains.kotlin.bir.BirDatabase
import org.jetbrains.kotlin.bir.CompressedSourceSpanManager
import org.jetbrains.kotlin.bir.backend.BirBackendContext
import org.jetbrains.kotlin.bir.backend.BirLoweringPhase
import org.jetbrains.kotlin.bir.types.BirTypeSystemContext
import org.jetbrains.kotlin.bir.types.BirTypeSystemContextImpl
import org.jetbrains.kotlin.bir.util.Ir2BirConverter
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.JvmBackendConfig
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.name.FqName

@OptIn(ObsoleteDescriptorBasedAPI::class)
class JvmBirBackendContext @OptIn(ObsoleteDescriptorBasedAPI::class) constructor(
    irContext: JvmBackendContext,
    module: ModuleDescriptor,
    compiledBir: BirDatabase,
    externalModulesBir: BirDatabase,
    val ir2BirConverter: Ir2BirConverter,
    dynamicPropertyManager: BirDynamicPropertiesManager,
    compressedSourceSpanManager: CompressedSourceSpanManager,
    phaseConfig: List<(JvmBirBackendContext) -> BirLoweringPhase>,
) : BirBackendContext(compiledBir, externalModulesBir, dynamicPropertyManager, compressedSourceSpanManager, irContext.configuration) {
    val config: JvmBackendConfig = irContext.state.config
    override val builtIns = irContext.builtIns
    val generationState: GenerationState = irContext.state
    override val internalPackageFqn = FqName("kotlin.jvm")
    
    val loweringPhases = phaseConfig.map { it(this) }

    override val birBuiltIns: BirBuiltIns = BirBuiltIns(irContext.irBuiltIns, ir2BirConverter)
    override val builtInSymbols: JvmBirBuiltInSymbols = JvmBirBuiltInSymbols(irContext.ir.symbols, ir2BirConverter)
    override val typeSystem: BirTypeSystemContext = BirTypeSystemContextImpl(birBuiltIns)

    override val sharedVariablesManager: SharedVariablesManager
        get() = TODO("Not yet implemented")
}