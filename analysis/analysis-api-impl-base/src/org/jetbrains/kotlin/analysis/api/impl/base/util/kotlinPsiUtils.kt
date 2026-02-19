/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.util

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

@KaImplementationDetail
val KtCallableDeclaration.callableId: CallableId?
    get() = when (this) {
        is KtNamedFunction -> callableId
        is KtProperty -> callableId
        else -> null
    }

/**
 * Not null [CallableId] for functions which are not local and are not a member of a local class.
 */
@KaImplementationDetail
val KtNamedFunction.callableId: CallableId?
    get() = if (isLocal) null else callableIdForName(nameAsSafeName)

@KaImplementationDetail
val KtEnumEntry.callableId: CallableId?
    get() = callableIdForName(nameAsSafeName)

@KaImplementationDetail
val KtProperty.callableId: CallableId?
    get() = if (isLocal) null else callableIdForName(nameAsSafeName)

@KaImplementationDetail
fun KtDeclaration.callableIdForName(callableName: Name): CallableId? {
    val containingClassOrObject = containingClassOrObject
    if (containingClassOrObject != null) {
        return containingClassOrObject.getClassId()?.let { classId ->
            CallableId(classId = classId, callableName = callableName)
        }
    }

    return CallableId(packageName = containingKtFile.packageFqName, callableName = callableName)
}