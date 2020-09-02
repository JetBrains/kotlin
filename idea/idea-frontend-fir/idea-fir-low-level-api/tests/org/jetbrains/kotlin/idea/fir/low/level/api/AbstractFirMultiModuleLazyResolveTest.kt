/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.idea.stubs.AbstractMultiModuleTest
import org.jetbrains.kotlin.idea.util.sourceRoots
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.nio.file.Paths

abstract class AbstractFirMultiModuleLazyResolveTest : AbstractMultiModuleTest() {
    override fun getTestDataPath(): String =
        "${KotlinTestUtils.getHomeDirectory()}/idea/idea-frontend-fir/idea-fir-low-level-api/testdata/multiModuleLazyResolve/"

    fun doTest(path: String) {
        val testStructure = TestProjectStructureReader.read(Paths.get(path))
        val modulesByNames = testStructure.modules.associate { moduleData ->
            moduleData.name to module(moduleData.name)
        }
        testStructure.modules.forEach { moduleData ->
            val module = modulesByNames.getValue(moduleData.name)
            moduleData.dependsOnModules.forEach { dependencyName ->
                module.addDependency(modulesByNames.getValue(dependencyName))
            }
        }
        val moduleToResolve = modulesByNames.getValue(testStructure.fileToResolve.moduleName)
        val fileToAnalysePath = moduleToResolve.sourceRoots.first().url + "/" + testStructure.fileToResolve.relativeFilePath

        val virtualFileToAnalyse = VirtualFileManager.getInstance().findFileByUrl(fileToAnalysePath)
            ?: error("File ${testStructure.fileToResolve.filePath} not found")
        val ktFileToAnalyse = PsiManager.getInstance(project).findFile(virtualFileToAnalyse) as KtFile
        val resolveState = LowLevelFirApiFacade.getResolveStateFor(ktFileToAnalyse)

        val fails = testStructure.fails

        try {
            val fir = LowLevelFirApiFacade.getOrBuildFirFor(ktFileToAnalyse, resolveState, FirResolvePhase.BODY_RESOLVE)
            KotlinTestUtils.assertEqualsToFile(File("$path/expected.txt"), fir.render())
        } catch (e: Throwable) {
            if (!fails) throw e
            return
        }
        if (fails) {
            throw AssertionError("Looks like test is passing, please remove `\"fails\": true` from structure.json")
        }
    }
}
