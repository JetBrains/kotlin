/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.environment

import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver

/**
 * The JVM views of the classpath of one compilation: a [KotlinClassFinder] and a [PackagePartProvider] over a
 * given [JvmClasspath], and the module graph of the whole classpath. The metadata and JKlib pipelines use it as
 * such, since they too read JVM-shaped roots.
 *
 * It hands out no Java view and knows nothing about FIR: which Java implementation serves a classpath, and
 * whether the Kotlin declarations of a session are exposed to Java resolution, is a decision of the
 * compilation, made once by whoever builds its sessions (`FirJavaInterop`).
 */
interface JvmCompilationEnvironment {
    fun getKotlinClassFinder(classpath: JvmClasspath): KotlinClassFinder

    fun getPackagePartProvider(classpath: JvmClasspath): PackagePartProvider

    fun getJavaModuleResolver(): JavaModuleResolver
}
