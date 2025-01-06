/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.moduleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.fir.java.FirJavaFacadeForSource
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.load.java.createJavaClassFinder

internal class LLFirJavaSymbolProvider(firSession: LLFirSession, val searchScope: GlobalSearchScope) : JavaSymbolProvider(
    firSession,
    FirJavaFacadeForSource(firSession, firSession.moduleData, firSession.project.createJavaClassFinder(searchScope))
)
