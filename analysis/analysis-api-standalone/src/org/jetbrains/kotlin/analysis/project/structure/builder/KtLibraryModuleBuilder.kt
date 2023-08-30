/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.builder

import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.KtLibrarySourceModule
import org.jetbrains.kotlin.analysis.project.structure.impl.KtLibraryModuleImpl
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@KtModuleBuilderDsl
public class KtLibraryModuleBuilder(
    private val kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment
) : KtBinaryModuleBuilder() {
    public lateinit var libraryName: String
    public var librarySources: KtLibrarySourceModule? = null

    override fun build(): KtLibraryModule {
        val binaryRoots = getBinaryRoots()
        val contentScope = StandaloneProjectFactory.createSearchScopeByLibraryRoots(binaryRoots, kotlinCoreProjectEnvironment)
        return KtLibraryModuleImpl(
            directRegularDependencies,
            directDependsOnDependencies,
            directFriendDependencies,
            contentScope,
            platform,
            kotlinCoreProjectEnvironment.project,
            binaryRoots,
            libraryName,
            librarySources,
        )
    }
}

@OptIn(ExperimentalContracts::class)
public inline fun KtModuleProviderBuilder.buildKtLibraryModule(init: KtLibraryModuleBuilder.() -> Unit): KtLibraryModule {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return KtLibraryModuleBuilder(kotlinCoreProjectEnvironment).apply(init).build()
}