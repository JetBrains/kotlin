/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.projectStructure

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.targetPlatform
import java.nio.file.Path

/**
 * A [KtTestModuleFactory] for script and REPL snippet test modules.
 *
 * Determines the [TestModuleKind] based on the file extension:
 * - `.repl.kts` files are treated as REPL snippets and marked via [KtScript.markAsReplSnippet].
 * - All other `.kts` files are treated as regular scripts.
 *
 * Note: currently only single-file snippets are supported. Multi-snippets are not yet implemented.
 *
 * @see TestModuleKind.ScriptSource
 */
object KtScriptTestModuleFactory : KtTestModuleFactory {
    override fun createModule(
        testModule: TestModule,
        contextModule: KtTestModule?,
        dependencyBinaryRoots: Collection<Path>,
        testServices: TestServices,
        project: Project,
    ): KtTestModule {
        val ktFile = TestModuleStructureFactory.createSourcePsiFiles(testModule, testServices, project).single { it is KtFile } as KtFile
        val module = KaScriptModuleImpl(
            ktFile,
            testModule.targetPlatform(testServices),
            testModule.languageVersionSettings,
            project,
        )

        return KtTestModule(TestModuleKind.ScriptSource, testModule, module, listOf(ktFile))
    }
}
