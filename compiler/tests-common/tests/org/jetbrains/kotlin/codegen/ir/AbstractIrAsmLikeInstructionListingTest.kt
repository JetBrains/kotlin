/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.ir

import org.jetbrains.kotlin.codegen.AbstractAsmLikeInstructionListingTest
import org.jetbrains.kotlin.test.TargetBackend

abstract class AbstractIrAsmLikeInstructionListingTest : AbstractAsmLikeInstructionListingTest() {
    override val backend = TargetBackend.JVM_IR
}