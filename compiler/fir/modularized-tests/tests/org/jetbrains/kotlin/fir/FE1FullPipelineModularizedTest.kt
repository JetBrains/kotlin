/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.LanguageVersion

/**
 * Language version to be used for K1 FP tests
 */
internal val LANGUAGE_VERSION_K1: String = System.getProperty("fir.bench.language.version.k1", "1.8")

class FE1FullPipelineModularizedTest : AbstractFullPipelineModularizedTest() {
    override fun configureArguments(args: K2JVMCompilerArguments, moduleData: ModuleData) {
        args.useK2 = false
        args.languageVersion = LANGUAGE_VERSION_K1
        // TODO: Remove when support for old modularized tests is removed
        if (moduleData.arguments == null) {
            args.jvmDefault = "compatibility"
            args.apiVersion = API_VERSION
            args.optIn = arrayOf(
                "kotlin.RequiresOptIn",
                "kotlin.contracts.ExperimentalContracts",
                "kotlin.io.path.ExperimentalPathApi",
                "org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI"
            )
            args.multiPlatform = true
            args.noStdlib = true
            args.noReflect = true
        }

        require(LanguageVersion.fromVersionString(args.languageVersion)!! < LanguageVersion.KOTLIN_2_0) {
            "Language version misconfiguration for K1 FP: ${args.languageVersion} >= 2.0"
        }
    }

    fun testTotalKotlin() {
        pinCurrentThreadToIsolatedCpu()
        for (i in 0 until PASSES) {
            println("Pass $i")
            runTestOnce(i)
        }
    }
}
