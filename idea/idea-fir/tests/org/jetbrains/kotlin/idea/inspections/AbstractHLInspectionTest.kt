/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import org.jetbrains.kotlin.idea.codeInsight.AbstractInspectionTest

abstract class AbstractHLInspectionTest : AbstractInspectionTest() {
    override fun isFirPlugin() = true
    override fun inspectionClassDirective() = "// FIR_INSPECTION_CLASS:"
    override fun registerGradlPlugin() {}
}