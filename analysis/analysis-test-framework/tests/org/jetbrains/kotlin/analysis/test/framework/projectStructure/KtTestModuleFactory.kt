/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.projectStructure

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.explicitTestModuleKind
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices
import java.nio.file.Path

fun interface KtTestModuleFactory : TestService {
    /**
     * Creates a [KtTestModule] for the given [testModule].
     *
     * @param contextModule a module to use as a context module. Some kinds of modules (such as dangling file modules) require a
     * context module. Modules representing code fragments also require a context element. That is why the [KtTestModule] is passed
     * instead of a plain [KaModule][org.jetbrains.kotlin.analysis.api.projectStructure.KaModule].
     * @param dependencyBinaryRoots The binary roots of [testModule]'s binary library dependencies. This allows avoiding unresolved symbol
     * issues when compiling test binary libraries that depend on other test binary libraries.
     */
    fun createModule(
        testModule: TestModule,
        contextModule: KtTestModule?,
        dependencyBinaryRoots: Collection<Path>,
        testServices: TestServices,
        project: Project,
    ): KtTestModule
}

private val TestServices.ktTestModuleFactory: KtTestModuleFactory by TestServices.testServiceAccessor()

/**
 * Returns the appropriate [KtTestModuleFactory] to build a [KaModule][org.jetbrains.kotlin.analysis.api.projectStructure.KaModule] for the given
 * [testModule].
 *
 * By default, the [KtTestModuleFactory] registered with these [TestServices] is returned. It may be overruled by the
 * [MODULE_KIND][org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestDirectives.MODULE_KIND] directive for a specific test module.
 *
 * [DependencyKindModuleStructureTransformer][org.jetbrains.kotlin.analysis.test.framework.services.DependencyKindModuleStructureTransformer]
 * should be used to properly set up the [DependencyKind][org.jetbrains.kotlin.test.model.DependencyKind] for module dependencies.
 *
 * @see org.jetbrains.kotlin.analysis.test.framework.services.DependencyKindModuleStructureTransformer
 */
fun TestServices.getKtModuleFactoryForTestModule(testModule: TestModule): KtTestModuleFactory = when (testModule.explicitTestModuleKind) {
    TestModuleKind.Source -> KtSourceTestModuleFactory
    TestModuleKind.LibraryBinary -> KtLibraryBinaryTestModuleFactory
    TestModuleKind.LibraryBinaryDecompiled -> KtLibraryBinaryDecompiledTestModuleFactory
    TestModuleKind.LibrarySource -> KtLibrarySourceTestModuleFactory
    TestModuleKind.ScriptSource -> KtScriptTestModuleFactory
    TestModuleKind.CodeFragment -> KtCodeFragmentTestModuleFactory
    TestModuleKind.NotUnderContentRoot -> error("Unsupported test module kind: ${TestModuleKind.NotUnderContentRoot}")
    else -> ktTestModuleFactory
}
