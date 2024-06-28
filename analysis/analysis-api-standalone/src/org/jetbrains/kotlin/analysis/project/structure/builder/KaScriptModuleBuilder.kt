/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.builder

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptModule
import org.jetbrains.kotlin.analysis.project.structure.impl.KaScriptModuleImpl
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
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
public class KtScriptModuleBuilder(
    private val kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment
) : KtModuleBuilder() {
    public lateinit var file: KtFile

    public var languageVersionSettings: LanguageVersionSettings =
        LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST)

    override fun build(): KaScriptModule {
        return KaScriptModuleImpl(
            directRegularDependencies,
            directDependsOnDependencies,
            directFriendDependencies,
            platform,
            kotlinCoreProjectEnvironment.project,
            file,
            languageVersionSettings
        )
    }
}

@KaExperimentalApi
@OptIn(ExperimentalContracts::class)
public inline fun KtModuleProviderBuilder.buildKtScriptModule(init: KtScriptModuleBuilder.() -> Unit): KaScriptModule {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return KtScriptModuleBuilder(kotlinCoreProjectEnvironment).apply(init).build()
}
