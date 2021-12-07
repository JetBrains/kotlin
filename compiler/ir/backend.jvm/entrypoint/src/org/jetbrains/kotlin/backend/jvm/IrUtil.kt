/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.jvm.lower.SingletonObjectJvmStaticTransformer
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrClass

fun IrClass.handleJvmStaticInSingletonObjects(irBuiltIns: IrBuiltIns, cachedFields: CachedFieldsForObjectInstances) {
    transform(SingletonObjectJvmStaticTransformer(irBuiltIns, cachedFields), null)
}