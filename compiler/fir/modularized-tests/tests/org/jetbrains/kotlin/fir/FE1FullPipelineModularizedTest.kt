/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments

class FE1FullPipelineModularizedTest : AbstractFullPipelineModularizedTest() {
    override fun configureArguments(args: K2JVMCompilerArguments, moduleData: ModuleData) {
        args.useK2 = false
        args.jvmDefault = "compatibility"
        args.apiVersion = API_VERSION
        args.optIn = arrayOf(
            "kotlin.RequiresOptIn",
            "kotlin.contracts.ExperimentalContracts",
            "kotlin.io.path.ExperimentalPathApi",
            "org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI"
        )
        args.multiPlatform = true
    }

    fun testTotalKotlin() {
        isolate()
        for (i in 0 until PASSES) {
            println("Pass $i")
            runTestOnce(i)
        }
    }
}
