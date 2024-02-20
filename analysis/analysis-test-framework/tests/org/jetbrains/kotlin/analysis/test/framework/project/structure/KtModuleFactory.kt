/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.project.structure

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.explicitTestModuleKind
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices
import java.nio.file.Path

fun interface KtModuleFactory : TestService {
    /**
     * Creates a [KtModule](org.jetbrains.kotlin.analysis.project.structure.KtModule) for the given [testModule].
     *
     * @param contextModule a module to use as a context module. Some kinds of modules (such as dangling file modules) require a
     * context module. Modules representing code fragments also require a context element. That is why the [KtTestModule] is passed
     * instead of a plain [KtModule][org.jetbrains.kotlin.analysis.project.structure.KtModule].
     *
     * @param dependencyPaths Paths of directories containing dependency libraries. The paths will be set as class paths for compilation.
     */
    fun createModule(
        testModule: TestModule,
        contextModule: KtTestModule?,
        dependencyPaths: Collection<Path>,
        testServices: TestServices,
        project: Project,
    ): KtTestModule
}

private val TestServices.ktModuleFactory: KtModuleFactory by TestServices.testServiceAccessor()

/**
 * Returns the appropriate [KtModuleFactory] to build a [KtModule][org.jetbrains.kotlin.analysis.project.structure.KtModule] for the given
 * [testModule].
 *
 * By default, the [KtModuleFactory] registered with these [TestServices] is returned. It may be overruled by the
 * [MODULE_KIND][org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestDirectives.MODULE_KIND] directive for a specific test module.
 *
 * [DependencyKindModuleStructureTransformer][org.jetbrains.kotlin.analysis.test.framework.services.DependencyKindModuleStructureTransformer]
 * should be used to properly set up the [DependencyKind][org.jetbrains.kotlin.test.model.DependencyKind] for module dependencies.
 *
 * @see org.jetbrains.kotlin.analysis.test.framework.services.DependencyKindModuleStructureTransformer
 */
fun TestServices.getKtModuleFactoryForTestModule(testModule: TestModule): KtModuleFactory = when (testModule.explicitTestModuleKind) {
    TestModuleKind.Source -> KtSourceModuleFactory
    TestModuleKind.LibraryBinary -> KtLibraryBinaryModuleFactory
    TestModuleKind.LibrarySource -> KtLibrarySourceModuleFactory
    TestModuleKind.ScriptSource -> KtScriptModuleFactory
    TestModuleKind.CodeFragment -> KtCodeFragmentModuleFactory
    TestModuleKind.NotUnderContentRoot -> error("Unsupported test module kind: ${TestModuleKind.NotUnderContentRoot}")
    else -> ktModuleFactory
}
