/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.checkers

import org.jetbrains.kotlin.analysis.low.level.api.fir.LLResolutionFacadeService
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.test.services.TestServices

internal inline fun checkAllFirFiles(testServices: TestServices, check: (List<FirFile>) -> Unit) {
    val project = testServices.ktTestModuleStructure.project
    for (testModule in testServices.ktTestModuleStructure.mainModules) {
        val resolutionFacade = LLResolutionFacadeService.getInstance(project).getResolutionFacade(testModule.ktModule)

        val firFiles = testModule.ktFiles
            .filter { ktFile ->
                // In some cases, the test file might be explicitly excluded from the test module's content scope. For example, resolve
                // extension tests define test files that are excluded from the module's content scope.
                //
                // When this happens, `getOrBuildFirFile` fails because we cannot find the `KaModule` for the excluded `KtFile`. Hence,
                // we have to filter out such files, as they are not meant to be analyzed.
                ktFile.virtualFile in testModule.ktModule.contentScope
            }
            .map { it.getOrBuildFirFile(resolutionFacade) }


        check(firFiles)
    }
}
