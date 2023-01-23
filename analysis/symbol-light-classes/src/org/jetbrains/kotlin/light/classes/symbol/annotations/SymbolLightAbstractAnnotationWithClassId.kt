/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.name.ClassId

internal abstract class SymbolLightAbstractAnnotationWithClassId(
    val classId: ClassId,
    owner: PsiElement,
) : SymbolLightAbstractAnnotation(owner) {
    override fun getQualifiedName(): String = classId.asFqNameString()
}
