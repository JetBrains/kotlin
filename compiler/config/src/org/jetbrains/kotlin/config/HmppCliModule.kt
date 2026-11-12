/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import java.io.File

class HmppCliModule(val name: String, val sources: Set<File>) {

    override fun toString(): String {
        return "Module $name"
    }
}

/**
 * All [modules] are sorted in reversed topological order
 *   (module without dependencies will be the first)
 */
class HmppCliModuleStructure(
    val modules: List<HmppCliModule>,
    val sourceDependencies: Map<HmppCliModule, List<HmppCliModule>>,
    val moduleDependencies: Map<HmppCliModule, List<String>>,
    val friendDependencies: Map<HmppCliModule, List<String>>
)

fun HmppCliModuleStructure.getModuleNameForSource(source: File): String? {
    return modules.firstOrNull { source in it.sources }?.name
}

fun HmppCliModuleStructure.isFromCommonModule(source: File): Boolean {
    return modules.indexOfFirst { source in it.sources }.let { it >= 0 && it < modules.size - 1 }
}
