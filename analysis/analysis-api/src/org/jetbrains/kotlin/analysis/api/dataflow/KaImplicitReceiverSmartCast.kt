/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.dataflow

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.types.KaType

/**
 * Represents type information about an implicit receiver which has been smart-cast to a more specific type. An implicit smart cast is
 * applied to an implicit receiver, such as `substring()` called on an implicit `this` given an earlier smart cast `this is String`.
 */
@KaNonPublicApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaImplicitReceiverSmartCast : KaLifetimeOwner {
    /**
     * The receiver type with the smart cast applied.
     */
    public val type: KaType

    /**
     * The kind of implicit receiver, i.e. a dispatch or extension receiver.
     */
    public val kind: KaImplicitReceiverSmartCastKind
}
