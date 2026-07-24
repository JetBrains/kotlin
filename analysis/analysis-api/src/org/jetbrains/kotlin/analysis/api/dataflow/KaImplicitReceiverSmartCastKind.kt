/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.dataflow

import org.jetbrains.kotlin.analysis.api.KaNonPublicApi

/**
 * Represents the kind of implicit receiver affected by the smart cast.
 */
@KaNonPublicApi
public enum class KaImplicitReceiverSmartCastKind {
    /**
     * The cast is applied to the receiver of a member call.
     */
    DISPATCH,

    /**
     * The cast is applied to the receiver of an extension function or property call.
     */
    EXTENSION,
}
