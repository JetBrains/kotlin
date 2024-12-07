/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.model

import org.jetbrains.kotlin.test.TargetBackend

fun interface TargetBackendComputer {
    fun compute(definedTargetBackend: TargetBackend?, testKClass: Class<*>): TargetBackend
}

object DefaultTargetBackendComputer : TargetBackendComputer {
    override fun compute(definedTargetBackend: TargetBackend?, testKClass: Class<*>): TargetBackend {
        return definedTargetBackend ?: TargetBackend.ANY
    }
}
