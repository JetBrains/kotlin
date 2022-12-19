/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.builder

import com.intellij.psi.PsiFileSystemItem
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.project.structure.impl.KtSourceModuleImpl
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@KtModuleBuilderDsl
public class KtSourceModuleBuilder : KtModuleBuilder() {
    public lateinit var moduleName: String
    public var languageVersionSettings: LanguageVersionSettings =
        LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST)
    private val sourceRoots: MutableList<PsiFileSystemItem> = mutableListOf()

    public fun addSourceRoot(file: PsiFileSystemItem) {
        sourceRoots.add(file)
    }

    public fun addSourceRoots(files: Collection<PsiFileSystemItem>) {
        sourceRoots.addAll(files)
    }

    override fun build(): KtSourceModule {
        return KtSourceModuleImpl(
            directRegularDependencies,
            directDependsOnDependencies,
            directFriendDependencies,
            contentScope,
            platform,
            project,
            moduleName,
            languageVersionSettings,
            sourceRoots,
        )
    }
}

@OptIn(ExperimentalContracts::class)
public inline fun buildKtSourceModule(init: KtSourceModuleBuilder.() -> Unit): KtSourceModule {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return KtSourceModuleBuilder().apply(init).build()
}