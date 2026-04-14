/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.projectStructure

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.psi.KtExperimentalApi
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.isKtsFile
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.targetPlatform
import java.nio.file.Path

/**
 * A [KtTestModuleFactory] for script and REPL snippet test modules.
 *
 * For multi-snippet REPL tests (files with `// SNIPPET` directives):
 * - Non-last snippets are compiled via [ReplSnippetCompiler] and returned as [TestModuleKind.LibraryBinary].
 * - The last snippet is returned as a [TestModuleKind.ScriptSource].
 *
 * @see TestModuleKind.ScriptSource
 */
object KtScriptTestModuleFactory : KtTestModuleFactory {
    @OptIn(KtExperimentalApi::class)
    override fun createModule(
        testModule: TestModule,
        contextModule: KtTestModule?,
        dependencyBinaryRoots: Collection<Path>,
        testServices: TestServices,
        project: Project,
    ): KtTestModule {
        val ktFile = TestModuleStructureFactory.createSourcePsiFiles(testModule, testServices, project).single { it is KtFile } as KtFile

        if (ktFile.script?.isReplSnippet != true) {
            return createScriptModule(testModule, ktFile, testServices, project)
        }

        val allSnippetModules = testServices.moduleStructure.modules.filter { module ->
            module.files.any { it.isKtsFile && it.name.endsWith(".repl.kts") }
        }

        if (testModule == allSnippetModules.last()) {
            return createScriptModule(testModule, ktFile, testServices, project)
        }

        return compileSnippetToLibrary(testModule, testServices, project)
    }

    private fun createScriptModule(
        testModule: TestModule,
        ktFile: KtFile,
        testServices: TestServices,
        project: Project,
    ): KtTestModule {
        val module = KaScriptModuleImpl(
            ktFile,
            testModule.targetPlatform(testServices),
            testModule.languageVersionSettings,
            project,
        )

        return KtTestModule(TestModuleKind.ScriptSource, testModule, module, listOf(ktFile))
    }

    private fun compileSnippetToLibrary(
        testModule: TestModule,
        testServices: TestServices,
        project: Project,
    ): KtTestModule {
        val binaryRoots = testServices.replSnippetCompiler.compileSnippetToJar(testModule, testServices)
        val libraryModule = KaLibraryModuleImpl(
            testModule.name,
            testModule.targetPlatform(testServices),
            TestModuleStructureFactory.getScopeForLibraryByRoots(project, binaryRoots, testServices),
            project,
            binaryRoots = binaryRoots,
            librarySources = null,
            isSdk = false,
        )

        return KtTestModule(TestModuleKind.LibraryBinary, testModule, libraryModule, emptyList())
    }
}
