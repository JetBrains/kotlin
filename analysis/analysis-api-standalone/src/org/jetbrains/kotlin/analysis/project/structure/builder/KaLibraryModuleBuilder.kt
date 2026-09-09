/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.builder

import org.jetbrains.kotlin.analysis.KaStandaloneInternalsProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneWorkaroundApi
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@KtModuleBuilderDsl
public abstract class KtLibraryModuleBuilder : KtBinaryModuleBuilder() {
    public lateinit var libraryName: String
    public var librarySources: KaLibrarySourceModule? = null

    abstract override fun build(): KaLibraryModule
}

@OptIn(ExperimentalContracts::class, StandaloneWorkaroundApi::class)
public inline fun KaModuleContainerBuilder.buildKtLibraryModule(init: KtLibraryModuleBuilder.() -> Unit): KaLibraryModule {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val builder = KaStandaloneInternalsProvider.instance.getLibraryModuleBuilder(coreApplicationEnvironment, project, isSdk = false)
    builder.libraryScopeConstructionMode = libraryScopeConstructionMode
    return builder.apply(init).build()
}
