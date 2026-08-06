/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure

import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

/**
 * The SDK (e.g. JDK) which this module is compiled against, if any.
 */
internal fun KaModule.sdkDependency(): KaLibraryModule? {
    if (this is KaLibraryModule && isSdk) return null
    return directRegularDependencies.firstOrNull { it is KaLibraryModule && it.isSdk } as? KaLibraryModule
}

/**
 * The SDK used to select the builtins session for this use-site module
 * (see [LLFirBuiltinsSessionFactory.getBuiltinsSession][org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.factory.LLFirBuiltinsSessionFactory.getBuiltinsSession]).
 *
 * For dangling file modules, the context module's SDK is used as a fallback so that a dangling file sees the same
 * deserialized builtin classes as its context.
 */
internal fun KaModule.builtinsSessionSdkDependency(): KaLibraryModule? = when (this) {
    is KaDanglingFileModule -> sdkDependency() ?: contextModule.builtinsSessionSdkDependency()
    else -> sdkDependency()
}
