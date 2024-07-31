/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.caches.LLSharedCacheLocks
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.LLFirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.java.FirJavaFacadeForSource
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.load.java.createJavaClassFinder
import java.util.concurrent.locks.ReentrantLock

internal class LLFirJavaSymbolProvider(
    firSession: FirSession,
    moduleData: LLFirModuleData,
    project: Project,
    val searchScope: GlobalSearchScope
) : JavaSymbolProvider(
    firSession,
    FirJavaFacadeForSource(firSession, moduleData, project.createJavaClassFinder(searchScope))
) {
    override val sharedClassComputationLock: ReentrantLock?
        get() = LLSharedCacheLocks.sharedJavaClassComputationLock
}
