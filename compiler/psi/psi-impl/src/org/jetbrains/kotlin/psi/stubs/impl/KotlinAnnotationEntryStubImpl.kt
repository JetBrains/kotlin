/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.constant.ConstantValue
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.stubs.KotlinAnnotationEntryStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KotlinAnnotationEntryStubImpl(
    parent: StubElement<out PsiElement>?,
    private val _shortName: StringRef?,
    override val hasValueArguments: Boolean,
    val valueArguments: Map<Name, ConstantValue<*>>?,
) : KotlinStubBaseImpl<KtAnnotationEntry>(parent, KtStubElementTypes.ANNOTATION_ENTRY), KotlinAnnotationEntryStub {

    override val shortName: String?
        get() = _shortName?.string
}
