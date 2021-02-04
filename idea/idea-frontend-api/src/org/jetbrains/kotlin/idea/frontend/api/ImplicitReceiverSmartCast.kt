/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api

import org.jetbrains.kotlin.idea.frontend.api.types.KtType

data class ImplicitReceiverSmartCast(val type: KtType, val kind: ImplicitReceiverSmartcastKind)

enum class ImplicitReceiverSmartcastKind {
    DISPATCH, EXTENSION
}
