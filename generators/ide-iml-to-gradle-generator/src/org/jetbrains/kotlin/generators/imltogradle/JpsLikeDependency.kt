/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.imltogradle

import org.jetbrains.jps.model.java.JpsJavaDependencyScope

interface JpsLikeDependency {
    fun convertToGradleCall(): String
    fun normalizedForComparison(): JpsLikeDependency
}

class JpsLikeDependencyWithComment(private val base: JpsLikeDependency, private val comment: String) : JpsLikeDependency {
    override fun convertToGradleCall(): String {
        return "${base.convertToGradleCall()} // $comment"
    }

    override fun normalizedForComparison() = base
}

data class JpsLikeJarDependency(
    val dependencyNotation: String,
    val scope: JpsJavaDependencyScope,
    val dependencyConfiguration: String?,
    val exported: Boolean
) : JpsLikeDependency {
    init {
        require(!dependencyNotation.contains(DEFAULT_KOTLIN_SNAPSHOT_VERSION)) {
            "JpsLikeJarDependency dependency notation ($dependencyNotation) cannot contain Kotlin snapshot version. " +
                    "Most likely you want to configure JpsLikeModuleDependency"
        }
    }

    override fun convertToGradleCall(): String {
        val scopeArg = "JpsDepScope.$scope"
        val exportedArg = "exported = true".takeIf { exported }
        return "jpsLikeJarDependency(${listOfNotNull(dependencyNotation, scopeArg, dependencyConfiguration, exportedArg).joinToString()})"
    }

    override fun normalizedForComparison() = this
}

data class JpsLikeModuleDependency(
    val moduleName: String,
    val scope: JpsJavaDependencyScope,
    val exported: Boolean
) : JpsLikeDependency {
    override fun convertToGradleCall(): String {
        val scopeArg = "JpsDepScope.$scope"
        val exportedArg = "exported = true".takeIf { exported }
        return "jpsLikeModuleDependency(${listOfNotNull("\"$moduleName\"", scopeArg, exportedArg).joinToString()})"
    }

    override fun normalizedForComparison() = this
}
