/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.resolve.source.toSourceElement
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.KotlinType

class SyntheticClassDescriptorForLambda(
        containingDeclaration: DeclarationDescriptor,
        name: Name,
        supertypes: Collection<KotlinType>,
        element: KtElement
) : ClassDescriptorImpl(containingDeclaration, name, Modality.FINAL, ClassKind.CLASS, supertypes, element.toSourceElement(),
                        /* isExternal = */ false, LockBasedStorageManager.NO_LOCKS) {
    init {
        initialize(MemberScope.Empty, emptySet(), null)
    }

    fun isCallableReference(): Boolean = source.getPsi() is KtCallableReferenceExpression
}
