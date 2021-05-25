/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.contributors.helpers

import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtScope
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtPackageSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.idea.references.KtReference

internal fun KtAnalysisSession.getStaticScope(reference: KtReference): KtScope? =
    when (val symbol = reference.resolveToSymbol()) {
        is KtSymbolWithMembers -> symbol.getStaticMemberScope()
        is KtPackageSymbol -> symbol.getPackageScope()
        else -> null
    }