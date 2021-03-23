/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.imltogradle

sealed class GradleDependencyNotation(val dependencyNotation: String, val dependencyConfiguration: String? = null) {
    init {
        require(dependencyNotation.isNotEmpty())
        require(dependencyConfiguration?.isNotEmpty() ?: true)
    }

    companion object {
        private const val artifactNameSubregex = """([a-zA-Z\-\._1-9]*?)"""

        private val libPathToGradleNotationRegex = """^lib\/$artifactNameSubregex\.jar$""".toRegex()
        private val pluginsPathToGradleNotationRegex = """^plugins\/$artifactNameSubregex\/.*?$""".toRegex()
        private val jarToGradleNotationRegex = """^$artifactNameSubregex\.jar$""".toRegex()

        fun fromJarPath(jarPath: String): GradleDependencyNotation? {
            if (jarPath == "lib/cds/classesLogAgent.jar") {
                return null // TODO
            }

            fun Regex.firstGroup() = matchEntire(jarPath)?.groupValues?.get(1)

            return pluginsPathToGradleNotationRegex.firstGroup()?.let { IntellijPluginDepGradleDependencyNotation(it) }
                ?: libPathToGradleNotationRegex.firstGroup()?.let { IntellijDepGradleDependencyNotation(it) }
                ?: jarToGradleNotationRegex.firstGroup()?.let { IntellijDepGradleDependencyNotation(it) }
                ?: error("Path $jarPath matches none of the regexes")
        }
    }

    data class IntellijPluginDepGradleDependencyNotation(val pluginName: String) :
        GradleDependencyNotation("intellijPluginDep(\"$pluginName\", forIde = true)")

    data class IntellijDepGradleDependencyNotation(val jarName: String) :
        GradleDependencyNotation("intellijDep(forIde = true)", "{ includeJars(\"$jarName\") }")
}
