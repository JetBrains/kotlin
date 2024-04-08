/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.types

import org.jetbrains.kotlin.analysis.api.symbols.KtClassLikeSymbol

/**
 * Returns a [KtClassLikeSymbol] for this [KtType] if the type represents a uniquely resolvable class/object/type alias.
 */
public val KtType.classSymbol: KtClassLikeSymbol?
    get() = (this as? KtNonErrorClassType)?.classSymbol

/**
 * Returns the [KtType]'s [abbreviated type][KtType.abbreviatedType], or the type itself if it doesn't have an abbreviated type.
 *
 * A common pattern is to prefer the abbreviated type if it exists, and otherwise take the original type, for example to find the best
 * target for navigation.
 */
public val KtType.abbreviatedTypeOrSelf: KtType
    get() = abbreviatedType ?: this
