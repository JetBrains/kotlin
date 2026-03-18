/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.IrSignatureClashTest
import org.jetbrains.kotlin.ir.util.KotlinMangler

@Suppress("JUnitTestCaseWithNoTests")
class NativeIrSignatureClashTest : IrSignatureClashTest() {
    override val irMangler: KotlinMangler.IrMangler = KonanManglerIr
}
