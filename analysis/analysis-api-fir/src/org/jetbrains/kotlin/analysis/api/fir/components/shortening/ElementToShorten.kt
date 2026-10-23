/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.shortening

import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.KtUserType

internal data class AdditionalImports(val simpleImports: Set<FqName>, val starImports: Set<FqName>)

internal sealed class ElementToShorten {
    abstract val element: KtElement
    abstract val nameToImport: FqName?
    abstract val importAllInParent: Boolean
}

internal class ShortenType(
    override val element: KtUserType,
    val shortenedRef: String? = null,
    override val nameToImport: FqName? = null,
    override val importAllInParent: Boolean = false,
) : ElementToShorten()

internal class ShortenQualifier(
    override val element: KtDotQualifiedExpression,
    val shortenedRef: String? = null,
    override val nameToImport: FqName? = null,
    override val importAllInParent: Boolean = false
) : ElementToShorten()

internal class ShortenThisLabel(
    override val element: KtThisExpression,
) : ElementToShorten() {
    override val nameToImport: FqName? = null
    override val importAllInParent: Boolean = false
}

/**
 * N.B. Does not subclass [ElementToShorten] because currently
 * there's no reason to do that.
 */
internal class ShortenKDocQualifier(
    val element: KDocName,
)
