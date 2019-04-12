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

fun <T : DeclarationDescriptor> Iterable<T>.applyFilter(filter: ModuleFilter): List<T> = filter { filter(it.module) }
