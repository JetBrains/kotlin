/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiled.light.classes.fe10

import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.analysis.decompiled.light.classes.origin.KotlinDeclarationInCompiledFileSearcher
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile
import org.jetbrains.kotlin.analysis.decompiler.psi.text.BySignatureIndexer
import org.jetbrains.kotlin.analysis.decompiler.psi.text.ClassNameAndSignature
import org.jetbrains.kotlin.analysis.decompiler.psi.text.relativeClassName
import org.jetbrains.kotlin.asJava.syntheticAccessors
import org.jetbrains.kotlin.load.kotlin.MemberSignature
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.allConstructors

class KotlinDeclarationInCompiledFileSearcherFE10Impl : KotlinDeclarationInCompiledFileSearcher() {
    override fun findDeclarationInCompiledFile(file: KtClsFile, member: PsiMember, signature: MemberSignature): KtDeclaration? {
        val relativeClassName = member.relativeClassName()
        val key = ClassNameAndSignature(relativeClassName, signature)

        val memberName = member.name
        if (memberName != null && !file.isContentsLoaded && file.hasDeclarationWithKey(BySignatureIndexer, key)) {
            findByStub(file, relativeClassName, member, memberName)?.let { return it }
        }

        val declaration = file.getDeclaration(BySignatureIndexer, key) ?: return null
        return if (member is PsiMethod && member.isConstructor && declaration is KtClassOrObject) {
            declaration.primaryConstructor ?: declaration
        } else {
            declaration
        }
    }
}

private fun findByStub(
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
