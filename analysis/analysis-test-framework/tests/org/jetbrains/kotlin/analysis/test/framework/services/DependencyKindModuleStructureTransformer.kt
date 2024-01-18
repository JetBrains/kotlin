/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services

import org.jetbrains.kotlin.analysis.test.framework.AnalysisApiTestDirectives
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.moduleKind
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.model.DependencyDescription
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.DefaultsProvider
import org.jetbrains.kotlin.test.services.ModuleStructureTransformer
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.impl.TestModuleStructureImpl

/**
 * This transformer is required for correct work of [getKtModuleFactoryForTestModule][org.jetbrains.kotlin.analysis.test.framework.project.structure.getKtModuleFactoryForTestModule]
 * to provide correct [DependencyKind] for dependencies
 *
 * @see org.jetbrains.kotlin.analysis.test.framework.project.structure.getKtModuleFactoryForTestModule
 */
@OptIn(TestInfrastructureInternals::class)
object DependencyKindModuleStructureTransformer : ModuleStructureTransformer() {
    override fun transformModuleStructure(moduleStructure: TestModuleStructure, defaultsProvider: DefaultsProvider): TestModuleStructure {
        if (AnalysisApiTestDirectives.MODULE_KIND !in moduleStructure.allDirectives) return moduleStructure

        val moduleMapping = moduleStructure.modules.associateBy(TestModule::name)
        return TestModuleStructureImpl(
            moduleStructure.modules.map { module ->
                module.copy(
                    allDependencies = module.allDependencies.map { dependency ->
                        transformDependency(dependency, moduleMapping)
                    }
                )
            },
            moduleStructure.originalTestDataFiles,
        )
    }

    private fun transformDependency(
        dependency: DependencyDescription,
        moduleMapping: Map<String, TestModule>,
    ): DependencyDescription {
        val dependencyModule = moduleMapping.getValue(dependency.moduleName)
        val newKind = when (dependencyModule.moduleKind) {
            TestModuleKind.Source,
            TestModuleKind.LibrarySource,
            TestModuleKind.ScriptSource,
            TestModuleKind.CodeFragment -> {
                DependencyKind.Source
            }

            TestModuleKind.LibraryBinary -> {
                DependencyKind.Binary
            }

            null -> {
                // There is no explicit module kind, so the dependency already has the right kind
                return dependency
            }
        }

        return dependency.copy(kind = newKind)
    }
}
