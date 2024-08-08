/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.ir

import org.jetbrains.kotlin.codegen.CustomScriptCodegenTest
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TargetBackend.JVM_IR

open class IrCustomScriptCodegenTest : CustomScriptCodegenTest() {
    override val backend: TargetBackend
        get() = JVM_IR

    override fun testAnnotatedDefinition() {
        // Discussing
    }
}
