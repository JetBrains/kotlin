/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.base

import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.name.Name

/**
 * A context receiver of function/property type which are directly specified in the code
 *
 * E.g, for the following code
 * ```
 * context(a@Int)
 * fun foo(){}
 * ```
 *
 * the context receiver is `KtContextReceiver(type=KtClassType(Int), label="a")`
 */
public abstract class KtContextReceiver : KtLifetimeOwner {
    /**
     * Type of the context receiver
     *
     * @see KtContextReceiver
     */
    public abstract val type: KtType

    /**
     * Additional label for the context receivers in the format `label@Type`, if label is not present, return `null`
     *
     * @see KtContextReceiver
     */
    public abstract val label: Name?
}

/**
 * Something which can have a [KtContextReceiver] declared. This may be a callable symbol or a class symbol
 */
public interface KtContextReceiversOwner : KtLifetimeOwner {
    /**
     * List of [KtContextReceiver] directly declared in the source code
     */
    public val contextReceivers: List<KtContextReceiver>
}