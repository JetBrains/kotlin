/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.inline

import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.builtins.getFunctionalClassKind
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * Implementation of given method.
 *
 * TODO: this method is actually a part of resolve and probably duplicates another one
 */
internal fun <T : CallableMemberDescriptor> T.resolveFakeOverride(): T {
    if (this.kind.isReal) {
        return this
    } else {
        val overridden = OverridingUtil.getOverriddenDeclarations(this)
        val filtered = OverridingUtil.filterOutOverridden(overridden)
        // TODO: is it correct to take first?
        @Suppress("UNCHECKED_CAST")
        return filtered.first { it.modality != Modality.ABSTRACT } as T
    }
}

internal val KotlinType.isKFunctionType: Boolean
    get() {
        val kind = constructor.declarationDescriptor?.getFunctionalClassKind()
        return kind == FunctionClassDescriptor.Kind.KFunction
    }

internal val FunctionDescriptor.isFunctionInvoke: Boolean
    get() {
        val dispatchReceiver = dispatchReceiverParameter ?: return false
        assert(!dispatchReceiver.type.isKFunctionType)

        return dispatchReceiver.type.isFunctionType &&
                this.isOperator && this.name == OperatorNameConventions.INVOKE
    }

internal val FunctionDescriptor.target: FunctionDescriptor
    get() = (if (modality == Modality.ABSTRACT) this else resolveFakeOverride()).original

// It is possible to declare "external inline fun",
// but it doesn't have much sense for native,
// since externals don't have IR bodies.
// Enforce inlining of constructors annotated with @InlineConstructor.
// TODO: should we keep this?
private val inlineConstructor = FqName("konan.internal.InlineConstructor")

internal val FunctionDescriptor.needsInlining: Boolean
    get() {
        val inlineConstructor = annotations.hasAnnotation(inlineConstructor)
        if (inlineConstructor) return true
        return (this.isInline && !this.isExternal)
    }

val ClassDescriptor.enumEntries: List<ClassDescriptor>
    get() {
        assert(this.kind == ClassKind.ENUM_CLASS)
        return this.unsubstitutedMemberScope.getContributedDescriptors()
            .filterIsInstance<ClassDescriptor>()
            .filter { it.kind == ClassKind.ENUM_ENTRY }
    }