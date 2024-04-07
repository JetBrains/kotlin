/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.defaultConstructor.ir

import org.jetbrains.kotlin.codegen.defaultConstructor.AbstractDefaultArgumentsReflectionTest
import org.jetbrains.kotlin.test.TargetBackend

abstract class AbstractIrDefaultArgumentsReflectionTest : AbstractDefaultArgumentsReflectionTest() {
    override val backend: TargetBackend
        get() = TargetBackend.JVM_IR
}
