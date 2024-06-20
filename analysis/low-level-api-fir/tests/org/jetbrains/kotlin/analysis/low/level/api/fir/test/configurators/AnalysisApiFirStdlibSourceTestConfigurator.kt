/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModuleFactory
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.createKtLibrarySourceModule
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import java.nio.file.Path

object AnalysisApiFirStdlibSourceTestConfigurator : AnalysisApiFirSourceLikeTestConfigurator(false) {
    override fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable) {
        super.configureTest(builder, disposable)
        builder.apply {
            useAdditionalService<KtTestModuleFactory> { KtStdlibSourceTestModuleFactory }
        }
    }
}

private object KtStdlibSourceTestModuleFactory : KtTestModuleFactory {
    override fun createModule(
        testModule: TestModule,
        contextModule: KtTestModule?,
        dependencyBinaryRoots: Collection<Path>,
        testServices: TestServices,
        project: Project,
    ): KtTestModule {
        val libraryJar = ForTestCompileRuntime.runtimeJarForTests().toPath()
        val librarySourcesJar = ForTestCompileRuntime.runtimeSourcesJarForTests().toPath()
        return createKtLibrarySourceModule(
            libraryJar = libraryJar,
            librarySourcesJar = librarySourcesJar,
            testModule = testModule,
            project = project,
            testServices = testServices,
        )
    }
}
