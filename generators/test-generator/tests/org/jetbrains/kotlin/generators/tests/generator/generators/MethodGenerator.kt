/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.generator.generators

import org.jetbrains.kotlin.generators.tests.generator.MethodModel
import org.jetbrains.kotlin.utils.Printer

abstract class MethodGenerator<in T : MethodModel> {
    companion object {
        fun generateDefaultSignature(method: MethodModel, p: Printer) {
            p.print("public void ${method.name}() throws Exception")
        }
    }

    abstract val kind: MethodModel.Kind

    abstract fun generateSignature(method: @UnsafeVariance T, p: Printer)
    abstract fun generateBody(method: @UnsafeVariance T, p: Printer)
}
