/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.resolve.OverridingUtil

// TODO: move me somewhere

/**
 * Implementation of given method.
 *
 * TODO: this method is actually a part of resolve and probably duplicates another one
 */
internal fun IrSimpleFunction.resolveFakeOverrideMaybeAbstract() = this.resolveFakeOverride(allowAbstract = true)

internal fun IrProperty.resolveFakeOverrideMaybeAbstract() = this.getter!!.resolveFakeOverrideMaybeAbstract().correspondingProperty!!

internal fun IrField.resolveFakeOverrideMaybeAbstract() = this.correspondingProperty!!.getter!!.resolveFakeOverrideMaybeAbstract().correspondingProperty!!.backingField

/**
 * Implementation of given method.
 *
 * TODO: this method is actually a part of resolve and probably duplicates another one
 */
internal fun <T : CallableMemberDescriptor> T.resolveFakeOverrideMaybeAbstract(): Set<T> {
    if (this.kind.isReal) {
        return setOf(this)
    } else {
        val overridden = OverridingUtil.getOverriddenDeclarations(this)
        val filtered = OverridingUtil.filterOutOverridden(overridden)
        // TODO: is it correct to take first?
        @Suppress("UNCHECKED_CAST")
        return filtered as Set<T>
    }
}
