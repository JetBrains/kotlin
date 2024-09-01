/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.builder

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.project.structure.impl.KaLibraryModuleImpl
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@KtModuleBuilderDsl
public open class KtLibraryModuleBuilder(
    private val kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment
) : KtBinaryModuleBuilder() {
    public lateinit var libraryName: String
    public var librarySources: KaLibrarySourceModule? = null

    override fun build(): KaLibraryModule = build(isSdk = false)

    @OptIn(KaExperimentalApi::class)
    protected fun build(isSdk: Boolean): KaLibraryModule {
        val binaryRoots = getBinaryRoots()
        val binaryVirtualFiles = getBinaryVirtualFiles()
        val contentScope =
            StandaloneProjectFactory.createSearchScopeByLibraryRoots(binaryRoots, binaryVirtualFiles, kotlinCoreProjectEnvironment)
        return KaLibraryModuleImpl(
            directRegularDependencies,
            directDependsOnDependencies,
            directFriendDependencies,
            contentScope,
            platform,
            kotlinCoreProjectEnvironment.project,
            binaryRoots,
            binaryVirtualFiles,
            libraryName,
            librarySources,
            isSdk,
        )
    }
}

@OptIn(ExperimentalContracts::class)
public inline fun KtModuleProviderBuilder.buildKtLibraryModule(init: KtLibraryModuleBuilder.() -> Unit): KaLibraryModule {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return KtLibraryModuleBuilder(kotlinCoreProjectEnvironment).apply(init).build()
}