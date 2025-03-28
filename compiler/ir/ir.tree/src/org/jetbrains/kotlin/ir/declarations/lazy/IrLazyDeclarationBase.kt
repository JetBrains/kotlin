/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import kotlin.properties.ReadWriteProperty

interface IrLazyDeclarationBase : IrDeclaration {
    fun createLazyAnnotations(): ReadWriteProperty<Any?, List<IrConstructorCall>>
}
