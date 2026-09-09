/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.builder

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.KaStandaloneInternalsProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@KtModuleBuilderDsl
public abstract class KtSourceModuleBuilder : KtModuleBuilder() {
    public lateinit var moduleName: String
    public var languageVersionSettings: LanguageVersionSettings =
        LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST)

    public var contentScope: GlobalSearchScope? = null

    public abstract fun addSourceRoot(path: Path)

    public abstract fun addSourceRoots(paths: Collection<Path>)

    public abstract fun addSourceVirtualFile(virtualFile: VirtualFile)

    public abstract fun addSourceVirtualFiles(virtualFiles: Collection<VirtualFile>)

    abstract override fun build(): KaSourceModule
}

@OptIn(ExperimentalContracts::class)
public inline fun KaModuleContainerBuilder.buildKtSourceModule(init: KtSourceModuleBuilder.() -> Unit): KaSourceModule {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return KaStandaloneInternalsProvider.instance.getSourceModuleBuilder(coreApplicationEnvironment, project).apply(init).build()
}
