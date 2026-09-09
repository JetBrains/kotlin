/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.builder

import org.jetbrains.kotlin.analysis.KaStandaloneInternalsProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneWorkaroundApi
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@KtModuleBuilderDsl
public abstract class KtSdkModuleBuilder : KtLibraryModuleBuilder() {
    public abstract fun addBinaryRootsFromJdkHome(jdkHome: Path, isJre: Boolean)
}

@OptIn(ExperimentalContracts::class, StandaloneWorkaroundApi::class)
public inline fun KaModuleContainerBuilder.buildKtSdkModule(init: KtSdkModuleBuilder.() -> Unit): KaLibraryModule {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val builder = KaStandaloneInternalsProvider.instance.getSdkModuleBuilder(coreApplicationEnvironment, project)
    builder.libraryScopeConstructionMode = libraryScopeConstructionMode
    return builder.apply(init).build()
}
