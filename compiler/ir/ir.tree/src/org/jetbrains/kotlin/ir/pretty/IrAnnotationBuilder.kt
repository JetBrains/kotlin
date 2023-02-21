/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.ir.expressions.IrConstructorCall

@PrettyIrDsl
class IrAnnotationBuilder internal constructor() {

    internal val annotations = mutableListOf<IrConstructorCall>()

    @IrNodeBuilderDsl
    fun irConstructorCall() {}
}
