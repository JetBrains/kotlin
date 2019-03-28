/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:UseExperimental(DescriptorInIrDeclaration::class)

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.backend.common.descriptors.WrappedDeclarationDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.ir.DescriptorInIrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclaration

@UseExperimental(DescriptorInIrDeclaration::class)
val IrDeclaration.isExpect get() = descriptor.let { it is MemberDescriptor && it.isExpect }

@UseExperimental(DescriptorInIrDeclaration::class)
fun <T : IrDeclaration> T.rebindWrappedDescriptor(to: T = this) {
    @Suppress("UNCHECKED_CAST")
    (descriptor as WrappedDeclarationDescriptor<T>).bind(to)
}

@UseExperimental(DescriptorInIrDeclaration::class)
inline fun <T : IrDeclaration, reified WD : WrappedDeclarationDescriptor<T>> T.tryToRebindWrappedDescriptor(to: T = this) {
    @Suppress("UNCHECKED_CAST")
    (descriptor as? WD)?.bind(to)
}
