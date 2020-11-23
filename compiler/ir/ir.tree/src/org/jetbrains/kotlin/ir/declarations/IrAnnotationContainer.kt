/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.ir.expressions.IrConstructorCall

interface IrAnnotationContainer {
    val annotations: List<IrConstructorCall>
}

interface IrMutableAnnotationContainer : IrAnnotationContainer {
    override var annotations: List<IrConstructorCall>
}
