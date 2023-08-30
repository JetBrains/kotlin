/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.builder

import org.jetbrains.kotlin.analysis.api.impl.base.util.LibraryUtils
import org.jetbrains.kotlin.analysis.api.standalone.base.project.structure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.project.structure.KtSdkModule
import org.jetbrains.kotlin.analysis.project.structure.impl.KtSdkModuleImpl
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@KtModuleBuilderDsl
public class KtSdkModuleBuilder(
    private val kotlinCoreProjectEnvironment: KotlinCoreProjectEnvironment
) : KtBinaryModuleBuilder() {
    public lateinit var sdkName: String

    public fun addBinaryRootsFromJdkHome(jdkHome: Path, isJre: Boolean) {
        val jdkRoots = LibraryUtils.findClassesFromJdkHome(jdkHome, isJre)
        addBinaryRoots(jdkRoots)
    }

    override fun build(): KtSdkModule {
        val binaryRoots = getBinaryRoots()
        val contentScope = StandaloneProjectFactory.createSearchScopeByLibraryRoots(binaryRoots, kotlinCoreProjectEnvironment)

        return KtSdkModuleImpl(
            directRegularDependencies,
            directDependsOnDependencies,
            directFriendDependencies,
            contentScope,
            platform,
            kotlinCoreProjectEnvironment.project,
            binaryRoots,
            sdkName
        )
    }
}

@OptIn(ExperimentalContracts::class)
public inline fun KtModuleProviderBuilder.buildKtSdkModule(init: KtSdkModuleBuilder.() -> Unit): KtSdkModule {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return KtSdkModuleBuilder(kotlinCoreProjectEnvironment).apply(init).build()
}