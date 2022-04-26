/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments

private val LANGUAGE_VERSION: String = System.getProperty("fir.bench.language.version", "1.4")

class FullPipelineModularizedTest : AbstractFullPipelineModularizedTest() {

    override fun configureArguments(args: K2JVMCompilerArguments, moduleData: ModuleData) {
        args.useK2 = true
        args.useIR = true
        args.apiVersion = LANGUAGE_VERSION
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
        for (i in 0 until PASSES) {
            println("Pass $i")
            runTestOnce(i)
        }
    }
}
