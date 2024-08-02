/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.references

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.idea.references.KtReference

@KaPlatformInterface
abstract class KotlinReferenceService {
    abstract fun canBeReferenceTo(reference: KtReference, target: PsiElement): Boolean

    companion object {
        fun getInstance(): KotlinReferenceService =
            ApplicationManager.getApplication().getService(KotlinReferenceService::class.java)
    }
}