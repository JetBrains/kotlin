/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.multiplatform

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.module

typealias ModuleFilter = (ModuleDescriptor) -> Boolean

fun onlyFromThisModule(module: ModuleDescriptor): ModuleFilter = { it == module }

val ALL_MODULES: ModuleFilter = { true }

fun allModulesProvidingExpectsFor(platformModule: ModuleDescriptor): ModuleFilter = {
    it == platformModule || it in platformModule.allExpectedByModules
}

/**
 * @param commonModule: The module for which the allowed modules can provide actuals for.
 * Meaning that all allowed modules will have declared a dependsOn edge to said [commonModule]
 *
 * @param platformModule: This parameter is only required to support pre-hmpp IDE
 * consumers. In this mode, leaf/platform modules will get wrapped using PlatformModuleInfo which
 * will be one [ModuleDescriptor] which wrap all dependsOn sources as well.
 * This module will not declare 'proper' [ModuleDescriptor.allExpectedByModules] to the common module
 * and therefore has to be passed into this filter as well to manually check if an actual was provided by this module.
 * This parameter can be dropped and removed once 'non-hmpp' mode shall not be supported anymore.
 */
fun allModulesProvidingActualsFor(
    commonModule: ModuleDescriptor,
    platformModule: ModuleDescriptor
): ModuleFilter = { module ->
    when {
        module == commonModule -> true
        commonModule in module.allExpectedByModules -> true
        /* Support non-hmpp IDE case */
        module == platformModule -> true
        else -> false
    }
}

fun <T : DeclarationDescriptor> Iterable<T>.applyFilter(filter: ModuleFilter): List<T> = filter { filter(it.module) }
