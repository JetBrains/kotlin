/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.builder

import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.KtLibrarySourceModule
import org.jetbrains.kotlin.analysis.project.structure.impl.KtLibraryModuleImpl
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@KtModuleBuilderDsl
public class KtLibraryModuleBuilder : KtBinaryModuleBuilder() {
    public lateinit var libraryName: String
    public var librarySources: KtLibrarySourceModule? = null

    override fun build(): KtLibraryModule {
        return KtLibraryModuleImpl(
            directRegularDependencies,
            directDependsOnDependencies,
            directFriendDependencies,
            contentScope,
            platform,
            project,
            binaryRoots,
            libraryName,
            librarySources,
        )
    }
}

@OptIn(ExperimentalContracts::class)
public inline fun buildKtLibraryModule(init: KtLibraryModuleBuilder.() -> Unit): KtLibraryModule {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return KtLibraryModuleBuilder().apply(init).build()
}