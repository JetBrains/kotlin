/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiled.light.classes.origin


import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind

interface LightMemberOriginForCompiledElement<T : PsiMember> : LightMemberOrigin {
    val member: T

    override val originKind: JvmDeclarationOriginKind
        get() = JvmDeclarationOriginKind.OTHER

    override fun isEquivalentTo(other: PsiElement?): Boolean {
        return when (other) {
            is KtDeclaration -> originalElement?.isEquivalentTo(other) ?: false
            is PsiMember -> member.isEquivalentTo(other)
            else -> false
        }
    }

    override fun isValid(): Boolean = member.isValid
}

data class LightMemberOriginForCompiledField(val psiField: PsiField, val file: KtClsFile) : LightMemberOriginForCompiledElement<PsiField> {
    override val member: PsiField
        get() = psiField

    override fun copy(): LightMemberOrigin {
        return LightMemberOriginForCompiledField(psiField.copy() as PsiField, file)
    }

    override fun isEquivalentTo(other: LightMemberOrigin?): Boolean {
        if (other !is LightMemberOriginForCompiledField) return false
        return psiField.isEquivalentTo(other.psiField)
    }

    override val originalElement: KtDeclaration? by lazyPub {
        KotlinDeclarationInCompiledFileSearcher.getInstance().findDeclarationInCompiledFile(file, psiField)
    }
}

data class LightMemberOriginForCompiledMethod(val psiMethod: PsiMethod, val file: KtClsFile) :
    LightMemberOriginForCompiledElement<PsiMethod> {

    override val member: PsiMethod
        get() = psiMethod

    override fun isEquivalentTo(other: LightMemberOrigin?): Boolean {
        if (other !is LightMemberOriginForCompiledMethod) return false
        return psiMethod.isEquivalentTo(other.psiMethod)
    }

    override fun copy(): LightMemberOrigin {
        return LightMemberOriginForCompiledMethod(psiMethod.copy() as PsiMethod, file)
    }

    override val originalElement: KtDeclaration? by lazyPub {
        KotlinDeclarationInCompiledFileSearcher.getInstance().findDeclarationInCompiledFile(file, psiMethod)
    }
}