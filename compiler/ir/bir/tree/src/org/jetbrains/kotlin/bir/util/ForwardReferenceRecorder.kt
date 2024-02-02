/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.util

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.BirElementBase
import org.jetbrains.kotlin.bir.symbols.BirSymbol

internal class ForwardReferenceRecorder {
    var recordedRef: BirElementBase? = null
        private set

    fun recordReference(forwardRef: BirSymbol?) {
        recordReference(forwardRef?.owner as BirElementBase?)
    }

    fun recordReference(forwardRef: BirElementBase?) {
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