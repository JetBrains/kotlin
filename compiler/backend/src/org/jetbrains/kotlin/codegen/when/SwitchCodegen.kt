/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.`when`

abstract class SwitchCodegen {
    companion object {
        // In modern JVM implementations it shouldn't matter very much for runtime performance
        // whether to choose lookupswitch or tableswitch.
        // The only metric that really matters is bytecode size and here we can estimate:
        // - lookupswitch: ~ 2 * labelsNumber
        // - tableswitch: ~ rangeLength
        fun preferLookupOverSwitch(labelsNumber: Int, rangeLength: Long) = rangeLength > 2L * labelsNumber || rangeLength > Int.MAX_VALUE
    }
}
