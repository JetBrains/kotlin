/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiled.light.classes.fe10

import com.intellij.psi.PsiMember
import org.jetbrains.kotlin.analysis.decompiled.light.classes.origin.KotlinDeclarationInCompiledFileSearcher
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile
import org.jetbrains.kotlin.load.kotlin.MemberSignature
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclaration

class KotlinDeclarationInCompiledFileSearcherFE10Impl : KotlinDeclarationInCompiledFileSearcher() {
    override fun findDeclarationInCompiledFile(file: KtClsFile, member: PsiMember, signature: MemberSignature): KtDeclaration? {
        val relativeClassName = generateSequence(member.containingClass) { it.containingClass }.toList().dropLast(1).reversed()
            .map { Name.identifier(it.name!!) }

        val memberName = member.name ?: return null
        return findByStubs(file, relativeClassName, member, memberName)
    }
}
