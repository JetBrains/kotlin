/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.sessions

import com.google.common.collect.Sets
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.testFramework.PsiTestUtil
import junit.framework.Assert
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.project.productionSourceInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.TestProjectModule
import org.jetbrains.kotlin.idea.fir.low.level.api.TestProjectStructure
import org.jetbrains.kotlin.idea.fir.low.level.api.TestProjectStructureReader
import org.jetbrains.kotlin.idea.fir.low.level.api.incModificationTracker
import org.jetbrains.kotlin.idea.jsonUtils.getString
import org.jetbrains.kotlin.idea.stubs.AbstractMultiModuleTest
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.writeText

abstract class AbstractSessionsInvalidationTest : AbstractMultiModuleTest() {
    override fun getTestDataPath(): String =
        "${KotlinTestUtils.getHomeDirectory()}/idea/idea-frontend-fir/idea-fir-low-level-api/testdata/sessionInvalidation/"

    protected fun doTest(path: String) {
        val testStructure = TestProjectStructureReader.readToTestStructure(
            Paths.get(path),
            toTestStructure = MultiModuleTestProjectStructure.Companion::fromTestProjectStructure
        )
        val modulesByNames = testStructure.modules.associate { moduleData ->
            moduleData.name to createEmptyModule(moduleData.name)
        }
        testStructure.modules.forEach { moduleData ->
            val module = modulesByNames.getValue(moduleData.name)
            moduleData.dependsOnModules.forEach { dependencyName ->
                module.addDependency(modulesByNames.getValue(dependencyName))
            }
        }

        val rootModule = modulesByNames[testStructure.rootModule]
            ?: error("${testStructure.rootModule} is not present in the list of modules")
        val modulesToMakeOOBM = testStructure.modulesToMakeOOBM.map {
            modulesByNames[it]
                ?: error("$it is not present in the list of modules")
        }

        val rootModuleSourceInfo = rootModule.productionSourceInfo()!!

        val storage = FirIdeSessionProviderStorage(project)

        val initialSessions = storage.getFirSessions(rootModuleSourceInfo)
        modulesToMakeOOBM.forEach { it.incModificationTracker() }
        val sessionsAfterOOBM = storage.getFirSessions(rootModuleSourceInfo)

        val changedSessions = Sets.symmetricDifference(initialSessions, sessionsAfterOOBM)
        val changedSessionsModulesNamesSorted = changedSessions.map { (it.moduleInfo as ModuleSourceInfo).module.name }.distinct().sorted()

        Assert.assertEquals(testStructure.expectedInvalidatedModules, changedSessionsModulesNamesSorted)
    }

    private fun FirIdeSessionProviderStorage.getFirSessions(rootModuleInfo: ModuleSourceInfo): Set<FirIdeSession> {
        val sessionProvider = getSessionProvider(rootModuleInfo)
        return sessionProvider.sessions.values.toSet()
    }

    private fun createEmptyModule(name: String): Module {
        val tmpDir = createTempDirectory().toPath()
        val module: Module = createModule("$tmpDir/$name", moduleType)
        val root = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tmpDir.toFile())!!
        WriteCommandAction.writeCommandAction(module.project).run<RuntimeException> {
            root.refresh(false, true)
        }

        PsiTestUtil.addSourceContentToRoots(module, root)
        return module
    }
}

private data class MultiModuleTestProjectStructure(
    val modules: List<TestProjectModule>,
    val rootModule: String,
    val modulesToMakeOOBM: List<String>,
    val expectedInvalidatedModules: List<String>,
) {
    companion object {
        fun fromTestProjectStructure(testProjectStructure: TestProjectStructure): MultiModuleTestProjectStructure {
            val json = testProjectStructure.json

            return MultiModuleTestProjectStructure(
                testProjectStructure.modules,
                json.getString(ROOT_MODULE_FIELD),
                json.getAsJsonArray(MODULES_TO_MAKE_OOBM_IN_FIELD).map { it.asString }.sorted(),
                json.getAsJsonArray(EXPECTED_INVALIDATED_MODULES_FIELD).map { it.asString }.sorted(),
            )
        }

        private const val ROOT_MODULE_FIELD = "rootModule"
        private const val MODULES_TO_MAKE_OOBM_IN_FIELD = "modulesToMakeOOBM"
        private const val EXPECTED_INVALIDATED_MODULES_FIELD = "expectedInvalidatedModules"
    }
}