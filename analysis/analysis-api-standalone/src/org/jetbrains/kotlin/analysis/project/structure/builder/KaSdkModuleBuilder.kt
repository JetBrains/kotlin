/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.builder

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.impl.base.util.LibraryUtils
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import java.nio.file.Path
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@KtModuleBuilderDsl
public class KtSdkModuleBuilder(
    coreApplicationEnvironment: CoreApplicationEnvironment,
    project: Project,
) : KtLibraryModuleBuilder(coreApplicationEnvironment, project, isSdk = true) {
    @OptIn(KaImplementationDetail::class)
    public fun addBinaryRootsFromJdkHome(jdkHome: Path, isJre: Boolean) {
        val jdkRoots = LibraryUtils.findClassesFromJdkHome(jdkHome, isJre)
        addBinaryRoots(jdkRoots)
    }
}

@OptIn(ExperimentalContracts::class)
public inline fun KtModuleProviderBuilder.buildKtSdkModule(init: KtSdkModuleBuilder.() -> Unit): KaLibraryModule {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return KtSdkModuleBuilder(coreApplicationEnvironment, project).apply(init).build()
}
