/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.model.DependencyDescription
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.services.impl.TestModuleStructureImpl

/**
 * This provider adds additional empty module platform module if there is only one module in the module structure with common platform
 *
 * ```
 * // MODULE: some
 * ```
 * converted to
 * ```
 * // MODULE: some
 * ...
 * // MODULE: platform()()(some)
 * ```
 *
 * This is needed for MPP diagnostic tests, which sometimes omit specifying platform module, which causes Fir2Ir facade skip
 *   IR actualizer, since there is only one module at all
 */
@OptIn(TestInfrastructureInternals::class)
object PlatformModuleProvider : ModuleStructureTransformer() {
    override fun transformModuleStructure(moduleStructure: TestModuleStructure, defaultsProvider: DefaultsProvider): TestModuleStructure {
        val module = moduleStructure.modules.singleOrNull() ?: return moduleStructure
        val dependency = DependencyDescription(module.name, defaultsProvider.defaultDependencyKind, DependencyRelation.DependsOnDependency)
        val platformModule = module.copy(
            name = "${module.name}-platform",
            targetPlatform = defaultsProvider.defaultPlatform,
            allDependencies = listOf(dependency),
            files = emptyList()
        )
        return TestModuleStructureImpl(listOf(module, platformModule), moduleStructure.originalTestDataFiles)
    }
}
