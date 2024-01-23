/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend

import org.jetbrains.kotlin.backend.common.ir.SharedVariablesManager
import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.expressions.BirCall
import org.jetbrains.kotlin.bir.types.BirTypeSystemContext
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.name.FqName

abstract class BirBackendContext(
    val compiledBir: BirDatabase,
    val externalModulesBir: BirDatabase,
    val dynamicPropertyManager: BirDynamicPropertiesManager,
    override val compressedSourceSpanManager: CompressedSourceSpanManager,
    val configuration: CompilerConfiguration
) : CompressedSourceSpanManagerScope {
    abstract val builtIns: KotlinBuiltIns
    abstract val birBuiltIns: BirBuiltIns
    abstract val typeSystem: BirTypeSystemContext
    abstract val internalPackageFqn: FqName
    abstract val sharedVariablesManager: SharedVariablesManager
    abstract val builtInSymbols: BirBuiltInSymbols

    val languageVersionSettings = configuration.languageVersionSettings

    open fun isSideEffectFree(call: BirCall): Boolean {
        return false
    }
}

