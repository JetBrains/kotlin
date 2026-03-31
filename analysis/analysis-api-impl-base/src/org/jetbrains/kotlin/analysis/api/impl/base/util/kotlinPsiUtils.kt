/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.util

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrScript

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

@OptIn(KtExperimentalApi::class)
@KaImplementationDetail
fun KtDeclaration.callableIdForName(callableName: Name): CallableId? {
    when (val containingDeclaration = containingClassOrScript) {
        null -> {}

        // Class not null -> the declaration must inherit its ClassId. Explicitly skip local classes
        is KtClassOrObject -> return containingDeclaration.getClassId()?.let { classId ->
            CallableId(classId = classId, callableName = callableName)
        }

        // Script not null -> the declaration inherits its ClassId only in case of REPL, top-level otherwise
        is KtScript -> containingDeclaration.replSnippetClassId?.let { classId ->
            return CallableId(classId = classId, callableName = callableName)
        }
    }

    return CallableId(packageName = containingKtFile.packageFqName, callableName = callableName)
}
