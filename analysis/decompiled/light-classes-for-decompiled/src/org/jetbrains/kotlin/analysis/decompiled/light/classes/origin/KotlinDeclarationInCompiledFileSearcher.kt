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
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.type.MapPsiToAsmDesc
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

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
        val classOrFile: KtDeclarationContainer = file.declarations.singleOrNull() as? KtClassOrObject ?: file
        val container: KtDeclarationContainer = if (relativeClassName.isEmpty())
            classOrFile
        else {
            relativeClassName.fold(classOrFile) { declaration: KtDeclarationContainer?, name: Name ->
                declaration?.declarations?.singleOrNull() { it is KtClassOrObject && it.name == name.asString() } as? KtClassOrObject
            }
        } ?: return null

        if (member is PsiMethod && member.isConstructor) {
            return container.safeAs<KtClassOrObject>()?.takeIf { it.name == memberName }?.allConstructors?.singleOrNull()
        }

        val declarations = container.declarations
        return when (member) {
            is PsiMethod -> {
                val names = member.syntheticAccessors(withoutOverrideCheck = true).map(Name::asString) + memberName
                declarations.singleOrNull { it.name in names }
            }

            is PsiField -> declarations.singleOrNull { it !is KtNamedFunction && it.name == memberName }
            else -> declarations.singleOrNull { it.name == memberName }
        }
    }

    companion object {
        fun getInstance(): KotlinDeclarationInCompiledFileSearcher =
            ApplicationManager.getApplication().getService(KotlinDeclarationInCompiledFileSearcher::class.java)
    }
}