/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.analysis.api.impl.base.projectStructure.KaBuiltinsModuleImpl
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProvider
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import java.nio.file.Path

object AnalysisApiBuiltinsBinaryTestConfigurator : AnalysisApiFirBinaryTestConfigurator() {
    override val testModuleFactory: KtTestModuleFactory get() = KtBuiltinsBinaryTestModuleFactory
}

private object KtBuiltinsBinaryTestModuleFactory : KtTestModuleFactory {
    override fun createModule(
        testModule: TestModule,
        contextModule: KtTestModule?,
        dependencyBinaryRoots: Collection<Path>,
        testServices: TestServices,
        project: Project,
    ): KtTestModule {
        val binaryRoots = BuiltinsVirtualFileProvider.getInstance().getBuiltinVirtualFiles()
        val decompiledFiles = binaryRoots.map {
            PsiManager.getInstance(project).findFile(it)
                ?: error("Builtin virtual file file $it was not found")
        }

        return KtTestModule(
            TestModuleKind.LibraryBinary,
            testModule,
            KaBuiltinsModuleImpl(testModule.targetPlatform, project),
            decompiledFiles,
        )
    }
}
