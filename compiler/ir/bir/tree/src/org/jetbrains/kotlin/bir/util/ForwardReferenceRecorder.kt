/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.util

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementBackReferenceRecorderScope
import org.jetbrains.kotlin.bir.BirElementBase

internal class ForwardReferenceRecorder : BirElementBackReferenceRecorderScope {
    var recordedRef: BirElementBase? = null

    override fun recordReference(forwardRef: BirElement?) {
        if (forwardRef == null) return

        if (recordedRef == null) {
            recordedRef = forwardRef as BirElementBase
        } else {
            if (recordedRef !== forwardRef)
                TODO("multiple forward refs for element")
        }
    }

    fun reset() {
        recordedRef = null
    }
}