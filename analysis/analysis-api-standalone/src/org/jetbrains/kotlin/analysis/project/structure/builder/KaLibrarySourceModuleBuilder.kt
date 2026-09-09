/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.builder

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.KaStandaloneInternalsProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@KtModuleBuilderDsl
public abstract class KtLibrarySourceModuleBuilder : KtModuleBuilder() {
    public lateinit var libraryName: String
    public lateinit var binaryLibrary: KaLibraryModule
    public lateinit var contentScope: GlobalSearchScope

    abstract override fun build(): KaLibrarySourceModule
}

@OptIn(ExperimentalContracts::class)
public inline fun KaModuleContainerBuilder.buildKtLibrarySourceModule(init: KtLibrarySourceModuleBuilder.() -> Unit): KaLibrarySourceModule {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return KaStandaloneInternalsProvider.instance.getLibrarySourceModuleBuilder(project).apply(init).build()
}
