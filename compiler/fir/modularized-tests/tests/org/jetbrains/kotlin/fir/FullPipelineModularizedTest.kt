/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments

// This is used for API version configuration for both frontends
// TODO: rename system property
internal val API_VERSION: String = System.getProperty("fir.bench.language.version", "1.4")

// This is used for language version configuration for K2 only. K1 always uses default version
private val LANGUAGE_VERSION_K2: String = System.getProperty("fir.bench.language.version.k2", "2.0")

class FullPipelineModularizedTest : AbstractFullPipelineModularizedTest() {

    override fun configureArguments(args: K2JVMCompilerArguments, moduleData: ModuleData) {
        args.useK2 = true
        args.useIR = true
        args.languageVersion = LANGUAGE_VERSION_K2
        args.apiVersion = API_VERSION
        args.jvmDefault = "compatibility"
        args.optIn = moduleData.optInAnnotations.toTypedArray() + arrayOf(
            "kotlin.RequiresOptIn",
            "kotlin.contracts.ExperimentalContracts",
            "kotlin.io.path.ExperimentalPathApi",
            "org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI"
        )
        args.renderInternalDiagnosticNames = true
    }

    fun testTotalKotlin() {
        isolate()
        for (i in 0 until PASSES) {
            println("Pass $i")
            runTestOnce(i)
        }
    }
}
