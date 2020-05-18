/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api

import org.jetbrains.kotlin.types.model.KotlinTypeMarker

data class ImplicitReceiverSmartCast(val types: Collection<KotlinTypeMarker>, val kind: ImplicitReceiverSmartcastKind)

enum class ImplicitReceiverSmartcastKind {
    DISPATCH, EXTENSION
}
