/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.DescriptorInIrDeclaration
import org.jetbrains.kotlin.ir.symbols.IrBindableSymbol

@DescriptorInIrDeclaration
val <D : DeclarationDescriptor> IrSymbolDeclaration<IrBindableSymbol<D, *>>.descriptor: D
    get() = symbol.descriptor

@DescriptorInIrDeclaration
val IrDeclaration.descriptorOrNull: DeclarationDescriptor?
    get() = when (this) {
        is IrSymbolOwner -> symbol.descriptor
        else -> null
    }

@DescriptorInIrDeclaration
val IrDeclaration.descriptor: DeclarationDescriptor
    get() = descriptorOrNull!!
