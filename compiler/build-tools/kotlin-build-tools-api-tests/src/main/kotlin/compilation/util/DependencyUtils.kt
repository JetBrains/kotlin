/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation.util

import org.jetbrains.kotlin.buildtools.api.SharedApiClassesClassLoader
import java.io.File
import java.net.URLClassLoader
import kotlin.io.path.toPath

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

val currentKotlinStdlibLocation
    get() = btaClassloader.loadClass(KotlinVersion::class.qualifiedName).protectionDomain.codeSource.location.toURI().toPath()