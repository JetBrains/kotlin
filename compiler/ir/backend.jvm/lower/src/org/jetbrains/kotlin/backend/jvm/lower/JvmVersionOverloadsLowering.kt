/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.lower.VersionOverloadsLowering
import org.jetbrains.kotlin.config.MavenComparableVersion
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.isAnnotation
import org.jetbrains.kotlin.name.JvmStandardClassIds

open class JvmVersionOverloadsLowering(irFactory: IrFactory, irBuiltIns: IrBuiltIns) : VersionOverloadsLowering(irFactory, irBuiltIns) {
    constructor(context: LoweringContext) : this(context.irFactory, context.irBuiltIns)

    override fun generateWrapperHeader(
        original: IrFunction,
        version: MavenComparableVersion?,
        includedParams: BooleanArray,
    ): IrFunction = super.generateWrapperHeader(original, version, includedParams).apply {
        this.annotations = this.annotations.filter {
            !it.isAnnotation(JvmStandardClassIds.JVM_OVERLOADS_FQ_NAME)
        }
    }
}