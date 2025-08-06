/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtEnumEntrySuperclassReferenceExpression
import org.jetbrains.kotlin.psi.stubs.KotlinEnumEntrySuperclassReferenceExpressionStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KotlinEnumEntrySuperclassReferenceExpressionStubImpl(parent: StubElement<*>, private val _referencedName: StringRef) :
    KotlinStubBaseImpl<KtEnumEntrySuperclassReferenceExpression>(parent, KtStubElementTypes.ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION),
    KotlinEnumEntrySuperclassReferenceExpressionStub {
    override val referencedName: String get() = _referencedName.string
}
