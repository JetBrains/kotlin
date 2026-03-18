/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.serialization.IrSignatureClashTest
import org.jetbrains.kotlin.ir.util.KotlinMangler

@Suppress("JUnitTestCaseWithNoTests")
class JsIrSignatureClashTest : IrSignatureClashTest() {
    override val irMangler: KotlinMangler.IrMangler = JsManglerIr
}
