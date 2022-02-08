/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiled.light.classes.origin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiMember
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile
import org.jetbrains.kotlin.load.kotlin.MemberSignature
import org.jetbrains.kotlin.psi.KtDeclaration

abstract class KotlinDeclarationInCompiledFileSearcher {
    abstract fun findDeclarationInCompiledFile(file: KtClsFile, member: PsiMember, signature: MemberSignature): KtDeclaration?

    companion object {
        fun getInstance(): KotlinDeclarationInCompiledFileSearcher =
            ApplicationManager.getApplication().getService(KotlinDeclarationInCompiledFileSearcher::class.java)
    }
}