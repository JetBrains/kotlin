/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation.util

import org.jetbrains.kotlin.buildtools.api.SharedApiClassesClassLoader
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Paths
import kotlin.io.path.toPath

private const val COMPILER_CLASSPATH_PROPERTY = "kotlin.build-tools-api.test.compilerClasspath"

fun initializeBtaClassloader(customParent: ClassLoader? = null): URLClassLoader {
    val classpath = System.getProperty(COMPILER_CLASSPATH_PROPERTY)
        ?: error("$COMPILER_CLASSPATH_PROPERTY is not set")

    val urls =
        classpath.split(File.pathSeparator)
            .map { File(it).toURI().toURL() }

    return URLClassLoader(urls.toTypedArray(), customParent ?: SharedApiClassesClassLoader())
}

val btaClassloader = initializeBtaClassloader()

val currentKotlinStdlibLocation
    get() = btaClassloader.loadClass(KotlinVersion::class.qualifiedName).protectionDomain.codeSource.location.toURI().toPath()

val currentKotlinJsStdlibKlibLocation
    get() = Paths.get(System.getProperty("kotlin.build-tools-api.test.jsStdlibClasspath"))
