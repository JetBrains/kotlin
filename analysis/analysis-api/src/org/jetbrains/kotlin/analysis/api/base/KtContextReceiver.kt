/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.base

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.types.KaType
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
 * the context receiver is `KaContextReceiver(type=KtClassType(Int), label="a")`
 */
public abstract class KaContextReceiver : KaLifetimeOwner {
    /**
     * Type of the context receiver
     *
     * @see KaContextReceiver
     */
    public abstract val type: KaType

    /**
     * Additional label for the context receivers in the format `label@Type`, if label is not present, return `null`
     *
     * @see KaContextReceiver
     */
    public abstract val label: Name?
}

@Deprecated("Use 'KaContextReceiver' instead", ReplaceWith("KaContextReceiver"))
public typealias KtContextReceiver = KaContextReceiver

/**
 * Something which can have a [KaContextReceiver] declared. This may be a callable symbol, a class symbol, or a functional type.
 */
public interface KaContextReceiversOwner : KaLifetimeOwner {
    /**
     * List of [KaContextReceiver] directly declared in the source code
     */
    public val contextReceivers: List<KaContextReceiver>
}

@Deprecated("Use 'KaContextReceiversOwner' instead", ReplaceWith("KaContextReceiversOwner"))
public typealias KtContextReceiversOwner = KaContextReceiversOwner