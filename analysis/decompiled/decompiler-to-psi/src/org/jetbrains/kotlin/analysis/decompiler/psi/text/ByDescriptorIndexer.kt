/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi.text

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.unwrapNullability
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes


fun getQualifiedName(typeElement: KtTypeElement?, isSuspend: Boolean): String? {
    val referencedName = when (typeElement) {
        is KtUserType -> getQualifiedName(typeElement)
        is KtFunctionType -> {
            var parametersCount = typeElement.parameters.size
            typeElement.receiverTypeReference?.let { parametersCount++ }
            if (isSuspend) {
                StandardNames.getSuspendFunctionClassId(parametersCount).asFqNameString()
            } else {
                StandardNames.getFunctionClassId(parametersCount).asFqNameString()
            }
        }
        is KtNullableType -> getQualifiedName(typeElement.unwrapNullability(), isSuspend)
        else -> null
    }
    return referencedName
}

private fun getQualifiedName(userType: KtUserType): String? {
    val qualifier = userType.qualifier ?: return userType.referencedName
    return getQualifiedName(qualifier) + "." + userType.referencedName
}

fun KtElementImplStub<*>.getAllModifierLists(): Array<out KtDeclarationModifierList> =
    @Suppress("DEPRECATION") // KT-78356
    getStubOrPsiChildren(KtStubElementTypes.MODIFIER_LIST, KtStubElementTypes.MODIFIER_LIST.arrayFactory)
