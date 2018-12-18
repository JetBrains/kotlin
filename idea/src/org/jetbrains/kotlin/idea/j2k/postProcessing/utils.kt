/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.j2k.postProcessing

import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.psi.*


fun KtExpression.asProperty(): KtProperty? =
    (this as? KtNameReferenceExpression)
        ?.references
        ?.firstOrNull { it is KtSimpleNameReference }
        ?.resolve() as? KtProperty

fun KtExpression.unpackedReferenceToProperty(): KtProperty? =
    when (this) {
        is KtDotQualifiedExpression ->
            if (receiverExpression is KtThisExpression) selectorExpression as? KtNameReferenceExpression
            else null
        is KtNameReferenceExpression -> this
        else -> null
    }?.references
        ?.firstOrNull { it is KtSimpleNameReference }
        ?.resolve() as? KtProperty


fun KtDeclaration.type() =
    (resolveToDescriptorIfAny() as? CallableDescriptor)?.returnType