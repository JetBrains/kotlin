/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols.markers

import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol

/**
 * A marker interface for symbols which could potentially be members of some class.
 *
 * @see org.jetbrains.kotlin.analysis.api.components.KaTypeProviderMixIn.getDispatchReceiverType
 */
public interface KaPossibleMemberSymbol : KaSymbol

@Deprecated("Use 'KaPossibleMemberSymbol' instead", ReplaceWith("KaPossibleMemberSymbol"))
public typealias KtPossibleMemberSymbol = KaPossibleMemberSymbol