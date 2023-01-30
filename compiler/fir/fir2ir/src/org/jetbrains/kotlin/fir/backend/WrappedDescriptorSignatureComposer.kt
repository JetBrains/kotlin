/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.IdSignatureComposer

class WrappedDescriptorSignatureComposer(
    private val delegate: IdSignatureComposer,
    private val firComposer: Fir2IrSignatureComposer
) : IdSignatureComposer by delegate {
    override fun withFileSignature(fileSignature: IdSignature.FileSignature, body: () -> Unit) {
        firComposer.withFileSignature(fileSignature) {
            delegate.withFileSignature(fileSignature, body)
        }
    }
}