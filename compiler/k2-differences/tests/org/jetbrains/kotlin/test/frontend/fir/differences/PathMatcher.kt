/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.differences

import java.io.File

abstract class PathMatcher {
    abstract fun matches(path: String): Boolean
}

fun PathMatcher.matches(file: File) = matches(file.path)

class NameMatcher(private val name: String) : PathMatcher() {
    private val exclusions = mutableListOf<SubstringMatcher>()

    override fun matches(path: String) = path.split("/").any { it == name } && exclusions.none { it.matches(path) }
}

val String.name get() = NameMatcher(this)

class SubstringMatcher(private val substring: String) : PathMatcher() {
    override fun matches(path: String) = substring in path
}

val String.substring get() = SubstringMatcher(this)

fun File.conforms(patterns: Collection<PathMatcher>): Boolean = patterns.any { it.matches(this) }
