/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.builder

import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.KtLibrarySourceModule
import org.jetbrains.kotlin.analysis.project.structure.impl.KtLibrarySourceModuleImpl
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@KtModuleBuilderDsl
public class KtLibrarySourceModuleBuilder : KtModuleBuilder() {
    public lateinit var libraryName: String
    public lateinit var binaryLibrary: KtLibraryModule

    override fun build(): KtLibrarySourceModule {
        return KtLibrarySourceModuleImpl(
            directRegularDependencies,
            directDependsOnDependencies,
            directFriendDependencies,
            contentScope,
            platform,
            project,
            libraryName,
            binaryLibrary
        )
    }
}

@OptIn(ExperimentalContracts::class)
public inline fun buildKtLibrarySourceModule(init: KtLibrarySourceModuleBuilder.() -> Unit): KtLibrarySourceModule {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return KtLibrarySourceModuleBuilder().apply(init).build()
}