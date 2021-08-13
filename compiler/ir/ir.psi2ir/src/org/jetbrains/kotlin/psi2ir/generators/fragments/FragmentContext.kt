/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators.fragments

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol

class FragmentContext(
    val capturedDescriptorToFragmentParameterMap: MutableMap<DeclarationDescriptor, IrValueParameterSymbol> = mutableMapOf()
)