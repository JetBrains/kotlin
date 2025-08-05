/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.psi.KtBlockStringTemplateEntry
import org.jetbrains.kotlin.psi.stubs.KotlinBlockStringTemplateEntryStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KotlinBlockStringTemplateEntryStubImpl(
    parent: StubElement<*>?,
    override val hasMultipleExpressions: Boolean,
    override val text: String,
) : KotlinStubBaseImpl<KtBlockStringTemplateEntry>(parent, KtStubElementTypes.LONG_STRING_TEMPLATE_ENTRY),
    KotlinBlockStringTemplateEntryStub
