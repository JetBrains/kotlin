/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.environment

import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver

interface JvmCompilationEnvironment {
    fun getKotlinClassFinder(classpath: JvmClasspath): KotlinClassFinder

    fun getPackagePartProvider(classpath: JvmClasspath): PackagePartProvider

    fun getJavaModuleResolver(): JavaModuleResolver
}
