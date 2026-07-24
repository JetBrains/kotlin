/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.builder

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneWorkaroundApi
import org.jetbrains.kotlin.analysis.api.standalone.projectStructure.toInternalLibraryScopeConstructionMode
import org.jetbrains.kotlin.analysis.project.structure.impl.KaLibraryModuleImpl
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@KtModuleBuilderDsl
public open class KtLibraryModuleBuilder(
    private val coreApplicationEnvironment: CoreApplicationEnvironment,
    private val project: Project,
    private val isSdk: Boolean,
) : KtBinaryModuleBuilder() {
    public lateinit var libraryName: String
    public var librarySources: KaLibrarySourceModule? = null

    @OptIn(KaExperimentalApi::class, KaImplementationDetail::class, StandaloneWorkaroundApi::class)
    override fun build(): KaLibraryModule {
        val binaryRoots = getBinaryRoots()
        val binaryVirtualFiles = getBinaryVirtualFiles()

        val contentScope = contentScope
            ?: StandaloneProjectFactory.createLibraryModuleSearchScope(
                binaryRoots,
                binaryVirtualFiles,
                libraryScopeConstructionMode.toInternalLibraryScopeConstructionMode(),
                coreApplicationEnvironment,
                project,
            )

        return KaLibraryModuleImpl(
            directRegularDependencies,
            directDependsOnDependencies,
            directFriendDependencies,
            contentScope,
            platform,
            project,
            binaryRoots,
            binaryVirtualFiles,
            libraryName,
            librarySources,
            isSdk,
        )
    }
}

@OptIn(ExperimentalContracts::class, StandaloneWorkaroundApi::class)
public inline fun KaModuleContainerBuilder.buildKtLibraryModule(init: KtLibraryModuleBuilder.() -> Unit): KaLibraryModule {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    val builder = KtLibraryModuleBuilder(coreApplicationEnvironment, project, isSdk = false)
    builder.libraryScopeConstructionMode = libraryScopeConstructionMode
    return builder.apply(init).build()
}
