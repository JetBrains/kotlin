/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.checkers

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLResolutionFacadeService
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.declarations.roots.collectCachedCheckableRootDeclarations
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.cache.LLFirSessionCache
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModuleStructure
import org.jetbrains.kotlin.fir.declarations.FirDeclaration

/**
 * Collects all FIR *root declarations* across the test's main modules that are subject to **consistency checks**.
 *
 * FIR files are checked in the state they were resolved to, or built fresh and checked as raw FIR if they haven't been resolved yet. In
 * addition, the root declarations currently cached by each module's session are gathered via [collectCachedCheckableRootDeclarations].
 */
internal fun KtTestModuleStructure.collectCheckableRootDeclarations(): List<FirDeclaration> =
    buildSet {
        for (testModule in mainModules) {
            // We always want to collect declarations from the session that was used by the test's resolution, including binary sessions for
            // binary libraries. Going via the resolution facade would force a resolvable session, so we have to access the session cache
            // directly.
            val session = LLFirSessionCache.getInstance(project).getSession(testModule.ktModule, preferBinary = true)

            addAll(session.collectCachedCheckableRootDeclarations())

            // In addition to cached files, we also want to retrieve freshly built FIR files that weren't built during the test. This leads
            // to a slightly higher coverage for the checks which rely on this function.
            if (session is LLFirResolvableModuleSession) {
                val resolutionFacade = LLResolutionFacadeService.getInstance(project).getResolutionFacade(testModule.ktModule)

                testModule.ktFiles
                    .filter { ktFile ->
                        // In some cases, the test file might be explicitly excluded from the test module's content scope. For example, resolve
                        // extension tests define test files that are excluded from the module's content scope.
                        //
                        // When this happens, `getOrBuildFirFile` fails because we cannot find the `KaModule` for the excluded `KtFile`. Hence,
                        // we have to filter out such files, as they are not meant to be analyzed.
                        ktFile.virtualFile in testModule.ktModule.contentScope
                    }
                    .forEach { it.getOrBuildFirFile(resolutionFacade) }
            }
        }
    }.toList()
