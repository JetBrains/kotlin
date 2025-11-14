/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kdoc.psi.api

import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtNonPublicApi

/**
 * A view of a KDoc comment.
 *
 * This is part of a non-public API intended for use only by IDE and Dokka. Current implementation
 * is based on the original `findKDoc` logic from Kotlin IDE Plugin.
 *
 * - If the symbol owns the KDoc, [primaryTag] is the KDoc's default section and
 *   [additionalSections] contains all sections of that KDoc (including the default one).
 * - For a primary constructor, if the enclosing class KDoc has an `@constructor` section,
 *   that section is used as [primaryTag], and [additionalSections] contains the sections
 *   that include `@param` tags from the class's KDoc.
 * - In some cases KDoc for a symbol may be extracted from the KDoc of a parent element. Examples:
 *   - a property-parameter of the primary constructor;
 *   - a type parameter;
 *   - a property of a class/object referenced with a @property tag in the class/object KDoc.
 *
 * @property primaryTag tag/section if the KDoc that is relevant for the symbol;
 * @property additionalSections other sections of the same KDoc that may provide additional context.
 */
@KtNonPublicApi
@SubclassOptInRequired(KtImplementationDetail::class)
interface KDocCommentDescriptor {
    /**
     * Primary tag/section tag/section if the KDoc that is relevant for the symbol
     */
    val primaryTag: KDocTag

    /**
     * Other sections from the same KDoc as [primaryTag] that may provide additional context
     */
    val additionalSections: List<KDocSection>
}