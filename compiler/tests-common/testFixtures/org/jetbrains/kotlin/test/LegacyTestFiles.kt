/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.ObsoleteTestInfrastructure

@ObsoleteTestInfrastructure
class LegacyTestFile @JvmOverloads constructor(
    @JvmField val name: String,
    @JvmField val content: String,
    @JvmField val directives: Directives = Directives()
) : Comparable<LegacyTestFile> {
    override operator fun compareTo(other: LegacyTestFile): Int {
        return name.compareTo(other.name)
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is LegacyTestFile && other.name == name
    }

    override fun toString(): String {
        return name
    }

}

@ObsoleteTestInfrastructure
open class LegacyTestModule(
    @JvmField val name: String,
    @JvmField val dependenciesSymbols: List<String>,
    @JvmField val friendsSymbols: List<String>,
    // mimics the name from ModuleStructureExtractorImpl, thought later converted to `-Xfragment-refines` parameter
    @JvmField val dependsOnSymbols: List<String> = listOf(),
) : Comparable<LegacyTestModule> {

    val dependencies: MutableList<LegacyTestModule> = arrayListOf()
    val friends: MutableList<LegacyTestModule> = arrayListOf()
    val dependsOn: MutableList<LegacyTestModule> = arrayListOf()

    override fun compareTo(other: LegacyTestModule): Int = name.compareTo(other.name)

    override fun toString(): String = name
}
