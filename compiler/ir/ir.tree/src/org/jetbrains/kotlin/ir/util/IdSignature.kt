/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.name.FqName

fun IdSignature.FileSignature.Companion.fromIrFile(fileSymbol: IrFileSymbol) =
    IdSignature.FileSignature(fileSymbol, fileSymbol.owner.packageFqName, fileSymbol.owner.fileEntry.name)

interface IdSignatureComposer {
    fun composeSignature(descriptor: DeclarationDescriptor): IdSignature?
    fun composeEnumEntrySignature(descriptor: ClassDescriptor): IdSignature?
    fun composeFieldSignature(descriptor: PropertyDescriptor): IdSignature?
    fun composeAnonInitSignature(descriptor: ClassDescriptor): IdSignature?

    fun withFileSignature(fileSignature: IdSignature.FileSignature, body: () -> Unit)

    val mangler: KotlinMangler.DescriptorMangler
}
