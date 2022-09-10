/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiled.light.classes.origin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile
import org.jetbrains.kotlin.asJava.syntheticAccessors
import org.jetbrains.kotlin.load.kotlin.MemberSignature
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.allConstructors
import org.jetbrains.kotlin.type.MapPsiToAsmDesc

abstract class KotlinDeclarationInCompiledFileSearcher {
    abstract fun findDeclarationInCompiledFile(file: KtClsFile, member: PsiMember, signature: MemberSignature): KtDeclaration?
    fun findDeclarationInCompiledFile(file: KtClsFile, member: PsiMember): KtDeclaration? {
        val signature = when (member) {
            is PsiField -> {
                val desc = MapPsiToAsmDesc.typeDesc(member.type)
                MemberSignature.fromFieldNameAndDesc(member.name, desc)
            }

            is PsiMethod -> {
                val desc = MapPsiToAsmDesc.methodDesc(member)
                val name = if (member.isConstructor) "<init>" else member.name
                MemberSignature.fromMethodNameAndDesc(name, desc)
            }

            else -> null
        } ?: return null

        return findDeclarationInCompiledFile(file, member, signature)
    }

    protected fun findByStubs(
        file: KtClsFile,
        relativeClassName: List<Name>,
        member: PsiMember,
        memberName: String,
    ): KtDeclaration? {
        val topClassOrObject = file.declarations.singleOrNull() as? KtClassOrObject
        val container: KtClassOrObject = if (relativeClassName.isEmpty())
            topClassOrObject
        else {
            relativeClassName.fold(topClassOrObject) { classOrObject, name ->
                classOrObject?.declarations?.singleOrNull { it.name == name.asString() } as? KtClassOrObject
            }
        } ?: return null

        return if (member is PsiMethod && member.isConstructor) {
            container.takeIf { it.name == memberName }?.allConstructors?.singleOrNull()
        } else {
            val declarations = container.declarations
            val names: Collection<String> = if (member is PsiMethod)
                member.syntheticAccessors.map(Name::asString) + memberName
            else
                listOf(memberName)

            declarations.singleOrNull { declaration -> declaration.name in names }
        }
    }

    companion object {
        fun getInstance(): KotlinDeclarationInCompiledFileSearcher =
            ApplicationManager.getApplication().getService(KotlinDeclarationInCompiledFileSearcher::class.java)
    }
}