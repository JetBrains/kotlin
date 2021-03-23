/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.resolve.jvm.JvmClassName

interface JvmGeneratorExtensions {
    val classNameOverride: MutableMap<IrClass, JvmClassName>

    val rawTypeAnnotationConstructor: IrConstructor?

    fun referenceLocalClass(classDescriptor: ClassDescriptor): IrClassSymbol?
}
