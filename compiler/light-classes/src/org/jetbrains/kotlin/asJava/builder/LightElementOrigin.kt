/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.asJava.builder

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
        is KtAnnotationEntry -> DefaultLightElementOrigin(originalElement)
        is KtDeclaration -> LightMemberOriginForDeclaration(originalElement, originKind)
        else -> LightElementOrigin.None
    }
}

interface LightMemberOrigin : LightElementOrigin {
    override val originalElement: KtDeclaration?
    override val originKind: JvmDeclarationOriginKind

    fun isEquivalentTo(other: LightMemberOrigin?): Boolean
    fun copy(): LightMemberOrigin
}

data class LightMemberOriginForDeclaration(override val originalElement: KtDeclaration, override val originKind: JvmDeclarationOriginKind) : LightMemberOrigin {
    override fun isEquivalentTo(other: LightMemberOrigin?): Boolean {
        if (other !is LightMemberOriginForDeclaration) return false
        return originalElement.isEquivalentTo(other.originalElement)
    }

    override fun copy(): LightMemberOrigin {
        return LightMemberOriginForDeclaration(originalElement.copy() as KtDeclaration, originKind)
    }
}

data class DefaultLightElementOrigin(override val originalElement: PsiElement?) : LightElementOrigin {
    override val originKind: JvmDeclarationOriginKind? get() = null
}

fun PsiElement?.toLightClassOrigin(): LightElementOrigin {
    return if (this != null) DefaultLightElementOrigin(this) else LightElementOrigin.None
}