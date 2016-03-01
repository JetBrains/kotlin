/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.asJava

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind


interface LightElementOrigin {
    val originalElement: PsiElement?
    val originKind: JvmDeclarationOriginKind?

    object None : LightElementOrigin {
        override val originalElement: PsiElement?
            get() = null
        override val originKind: JvmDeclarationOriginKind?
            get() = null

        override fun toString() = "NONE"
    }
}

fun JvmDeclarationOrigin.toLightMemberOrigin(): LightElementOrigin {
    val originalElement = element
    return when (originalElement) {
        is KtDeclaration -> LightMemberOrigin(originalElement, originKind)
        is KtAnnotationEntry -> DefaultLightElementOrigin(originalElement)
        else -> LightElementOrigin.None
    }
}

data class LightMemberOrigin(override val originalElement: KtDeclaration, override val originKind: JvmDeclarationOriginKind) : LightElementOrigin

data class DefaultLightElementOrigin(override val originalElement: PsiElement?) : LightElementOrigin {
    override val originKind: JvmDeclarationOriginKind? get() = null
}

fun PsiElement?.toLightClassOrigin(): LightElementOrigin {
    return if (this != null) DefaultLightElementOrigin(this) else LightElementOrigin.None
}

fun LightMemberOrigin.copy() = LightMemberOrigin(originalElement.copy() as KtDeclaration, originKind)