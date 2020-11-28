/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analyzer

import org.jetbrains.kotlin.descriptors.ModuleDescriptor

val ModuleDescriptor.moduleInfo: ModuleInfo?
    get() = getCapability(ModuleInfo.Capability)

internal fun collectAllExpectedByModules(entryModule: ModuleInfo): Set<ModuleInfo> {
    val unprocessedModules = ArrayDeque<ModuleInfo>().apply { addAll(entryModule.expectedBy) }
    val expectedByModules = HashSet<ModuleInfo>()

    while (unprocessedModules.isNotEmpty()) {
        val nextImplemented = unprocessedModules.removeFirst()
        if (expectedByModules.add(nextImplemented)) {
            unprocessedModules.addAll(nextImplemented.expectedBy)
        }
    }

    return expectedByModules
}
