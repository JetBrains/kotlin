/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.builder

import org.jetbrains.kotlin.analysis.KaStandaloneInternalsProvider
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptModule
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.psi.KtFile
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@KaExperimentalApi
@KtModuleBuilderDsl
public abstract class KtScriptModuleBuilder : KtModuleBuilder() {
    public lateinit var file: KtFile

    abstract override fun build(): KaScriptModule

    public var languageVersionSettings: LanguageVersionSettings =
        LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST)
}

@KaExperimentalApi
@OptIn(ExperimentalContracts::class)
public inline fun KaModuleContainerBuilder.buildKtScriptModule(init: KtScriptModuleBuilder.() -> Unit): KaScriptModule {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return KaStandaloneInternalsProvider.instance.getScriptModuleBuilder(project).apply(init).build()
}
