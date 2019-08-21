/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.net.URL

open class AbstractBlackBoxCodegenTestWithVariadicGenerics : AbstractBlackBoxCodegenTest() {
    init {
        additionalDependencies = listOf(
            PathUtil.kotlinPathsForDistDirectory.variadicGenericsPath
        )
    }

    override fun updateConfiguration(configuration: CompilerConfiguration) {
        configuration.addJvmClasspathRoot(PathUtil.kotlinPathsForDistDirectory.variadicGenericsPath)
//        configuration.put(JVMConfigurationKeys.IR, true)
    }

    override fun getBackend() = TargetBackend.ANY
//    override fun getBackend() = TargetBackend.JVM_IR

    override fun getClassPathURLs(): Array<out URL> {
        val urls = super.getClassPathURLs()
        return urls
    }
}