/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.error

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.TypeSubstitution
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner

class ErrorClassDescriptor(name: Name) : ClassDescriptorImpl(
    ErrorUtils.errorModule, name, Modality.OPEN, ClassKind.CLASS, emptyList(), SourceElement.NO_SOURCE, false, LockBasedStorageManager.NO_LOCKS
) {
    init {
        val errorConstructor = ClassConstructorDescriptorImpl.create(this, Annotations.EMPTY, true, SourceElement.NO_SOURCE)
            .apply {
                initialize(
                    emptyList(),
                    DescriptorVisibilities.INTERNAL
                )
            }
        val memberScope = ErrorUtils.createErrorScope(ErrorScopeKind.SCOPE_FOR_ERROR_CLASS, errorConstructor.name.toString(), "")
        errorConstructor.returnType = ErrorType(
            ErrorUtils.createErrorTypeConstructor(ErrorTypeKind.ERROR_CLASS),
            memberScope,
            ErrorTypeKind.ERROR_CLASS
        )
        initialize(memberScope, setOf(errorConstructor), errorConstructor)
    }

    override fun substitute(substitutor: TypeSubstitutor): ClassDescriptor = this

    override fun getMemberScope(typeArguments: List<TypeProjection>, kotlinTypeRefiner: KotlinTypeRefiner): MemberScope =
        ErrorUtils.createErrorScope(ErrorScopeKind.SCOPE_FOR_ERROR_CLASS, name.toString(), typeArguments.toString())

    override fun getMemberScope(typeSubstitution: TypeSubstitution, kotlinTypeRefiner: KotlinTypeRefiner): MemberScope =
        ErrorUtils.createErrorScope(ErrorScopeKind.SCOPE_FOR_ERROR_CLASS, name.toString(), typeSubstitution.toString())

    override fun toString(): String = name.asString()
}