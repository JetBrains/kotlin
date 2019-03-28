/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.backend.common.descriptors.WrappedDeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.ir.DescriptorInIrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.resolve.DescriptorUtils

val IrDeclaration.isExpect get() = descriptorWithoutAccessCheck.let { it is MemberDescriptor && it.isExpect }

val IrDeclaration.isLocal get() = DescriptorUtils.isLocal(this.descriptorWithoutAccessCheck)

fun <T : IrDeclaration> T.rebindWrappedDescriptor(to: T = this) {
    @Suppress("UNCHECKED_CAST")
    (descriptorWithoutAccessCheck as WrappedDeclarationDescriptor<T>).bind(to)
}

@UseExperimental(DescriptorInIrDeclaration::class)
inline fun <T : IrDeclaration, reified WD : WrappedDeclarationDescriptor<T>> T.tryToRebindWrappedDescriptor(to: T = this) {
    @Suppress("UNCHECKED_CAST")
    (descriptor as? WD)?.bind(to)
}

@UseExperimental(DescriptorInIrDeclaration::class)
private val IrDeclaration.descriptorWithoutAccessCheck: DeclarationDescriptor
    get() = descriptor

