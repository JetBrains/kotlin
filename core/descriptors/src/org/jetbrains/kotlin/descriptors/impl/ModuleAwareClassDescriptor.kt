/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.impl

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleAwareClassDescriptor.Companion.getRefinedMemberScopeIfPossible
import org.jetbrains.kotlin.descriptors.impl.ModuleAwareClassDescriptor.Companion.getRefinedUnsubstitutedMemberScopeIfPossible
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.TypeSubstitution
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner

abstract class ModuleAwareClassDescriptor : ClassDescriptor {
    protected abstract fun getUnsubstitutedMemberScope(kotlinTypeRefiner: KotlinTypeRefiner): MemberScope
    protected abstract fun getMemberScope(typeSubstitution: TypeSubstitution, kotlinTypeRefiner: KotlinTypeRefiner): MemberScope
    protected abstract fun getMemberScope(typeArguments: List<TypeProjection>, kotlinTypeRefiner: KotlinTypeRefiner): MemberScope

    companion object {
        internal fun ClassDescriptor.getRefinedUnsubstitutedMemberScopeIfPossible(
            kotlinTypeRefiner: KotlinTypeRefiner
        ): MemberScope =
            (this as? ModuleAwareClassDescriptor)?.getUnsubstitutedMemberScope(kotlinTypeRefiner) ?: this.unsubstitutedMemberScope

        internal fun ClassDescriptor.getRefinedMemberScopeIfPossible(
            typeSubstitution: TypeSubstitution,
            kotlinTypeRefiner: KotlinTypeRefiner
        ): MemberScope =
            (this as? ModuleAwareClassDescriptor)?.getMemberScope(typeSubstitution, kotlinTypeRefiner) ?: this.getMemberScope(
                typeSubstitution
            )
    }
}

fun ClassDescriptor.getRefinedUnsubstitutedMemberScopeIfPossible(
    kotlinTypeRefiner: KotlinTypeRefiner
): MemberScope = getRefinedUnsubstitutedMemberScopeIfPossible(kotlinTypeRefiner)

fun ClassDescriptor.getRefinedMemberScopeIfPossible(
    typeSubstitution: TypeSubstitution,
    kotlinTypeRefiner: KotlinTypeRefiner
): MemberScope = getRefinedMemberScopeIfPossible(typeSubstitution, kotlinTypeRefiner)
