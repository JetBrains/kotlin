/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiled.light.classes.fe10

import com.intellij.psi.PsiMember
import org.jetbrains.kotlin.analysis.decompiled.light.classes.origin.KotlinDeclarationInCompiledFileSearcher
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile
import org.jetbrains.kotlin.analysis.decompiler.psi.text.BySignatureIndexer
import org.jetbrains.kotlin.analysis.decompiler.psi.text.ClassNameAndSignature
import org.jetbrains.kotlin.analysis.decompiler.psi.text.relativeClassName
import org.jetbrains.kotlin.load.kotlin.MemberSignature
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDeclarationContainer

class KotlinDeclarationInCompiledFileSearcherFE10Impl: KotlinDeclarationInCompiledFileSearcher() {
    override fun findDeclarationInCompiledFile(file: KtClsFile, member: PsiMember, signature: MemberSignature): KtDeclaration? {
        val relativeClassName = member.relativeClassName()
        val key = ClassNameAndSignature(relativeClassName, signature)

        val memberName = member.name

        if (memberName != null && !file.isContentsLoaded && file.hasDeclarationWithKey(BySignatureIndexer, key)) {
            val container: KtDeclarationContainer? = if (relativeClassName.isEmpty())
                file
            else {
                val topClassOrObject = file.declarations.singleOrNull() as? KtClassOrObject
                relativeClassName.fold(topClassOrObject) { classOrObject, name ->
                    classOrObject?.declarations?.singleOrNull { it.name == name.asString() } as? KtClassOrObject
                }
            }

            val declaration = container?.declarations?.singleOrNull {
                it.name == memberName
            }

            if (declaration != null) {
                return declaration
            }
        }

        return file.getDeclaration(BySignatureIndexer, key)
    }
}