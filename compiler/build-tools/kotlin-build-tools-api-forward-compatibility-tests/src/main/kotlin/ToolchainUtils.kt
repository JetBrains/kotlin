/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.SharedApiClassesClassLoader
import java.io.File
import java.net.URLClassLoader

private const val COMPILER_CLASSPATH_PROPERTY = "kotlin.build-tools-api.test.compilerClasspath"

private fun initializeBtaClassloader(): URLClassLoader {
    val classpath = System.getProperty(COMPILER_CLASSPATH_PROPERTY)
        ?: error("$COMPILER_CLASSPATH_PROPERTY is not set")

    val urls =
        classpath.split(File.pathSeparator)
            .map { File(it).toURI().toURL() }

    println("Loading classes from classpath: $urls")
    return URLClassLoader(urls.toTypedArray(), SharedApiClassesClassLoader())
}

val btaClassloader = initializeBtaClassloader()

val toolchain by lazy(LazyThreadSafetyMode.NONE) { KotlinToolchains.loadImplementation(btaClassloader) }