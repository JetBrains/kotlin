/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kdoc.psi.impl

import org.jetbrains.kotlin.kdoc.psi.api.KtDocCommentDescriptor
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtNonPublicApi

@KtNonPublicApi
@KtImplementationDetail
class KtDocCommentDescriptorImpl(
    override val primaryTag: KDocTag,
    override val additionalSections: List<KDocSection>,
) : KtDocCommentDescriptor
