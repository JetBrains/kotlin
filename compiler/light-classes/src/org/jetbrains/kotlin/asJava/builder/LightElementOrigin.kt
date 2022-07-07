/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.builder

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin

fun JvmDeclarationOrigin.toLightMemberOrigin(): LightElementOrigin = when (val originalElement = element) {
    is KtAnnotationEntry -> DefaultLightElementOrigin(originalElement)
    is KtDeclaration -> LightMemberOriginForDeclaration(originalElement, originKind, parametersForJvmOverload)
    else -> LightElementOrigin.None
}

fun PsiElement?.toLightClassOrigin(): LightElementOrigin {
    return if (this != null) DefaultLightElementOrigin(this) else LightElementOrigin.None
}
