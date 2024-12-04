/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField

abstract class EnumEntriesIntrinsicMappingsCache {
    abstract fun getEnumEntriesIntrinsicMappings(containingClass: IrClass, enumClass: IrClass): IrField

    abstract fun generateMappingsClasses()
}
