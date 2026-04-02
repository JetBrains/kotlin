/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.projectStructure

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.isKtsFile
import java.nio.file.Path

/**
 * Universal factory for source-like test modules. It automatically detects whether the test module is a script or a source module.
 *
 * @see TestModuleKind.Source
 * @see TestModuleKind.ScriptSource
 */
object KtSourceLikeTestModuleFactory : KtTestModuleFactory {
    override fun createModule(
        testModule: TestModule,
        contextModule: KtTestModule?,
        dependencyBinaryRoots: Collection<Path>,
        testServices: TestServices,
        project: Project,
    ): KtTestModule = when {
        testModule.files.any(TestFile::isKtsFile) -> {
            KtScriptTestModuleFactory.createModule(testModule, contextModule, dependencyBinaryRoots, testServices, project)
        }

        else -> KtSourceTestModuleFactory.createModule(testModule, contextModule, dependencyBinaryRoots, testServices, project)
    }
}
