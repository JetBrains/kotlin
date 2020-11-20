/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.serialization.IrFileSerializer
import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.FqName

class JsIrFileSerializer(
    logger: LoggingContext,
    declarationTable: DeclarationTable,
    expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>,
    skipExpects: Boolean,
    bodiesOnlyForInlines: Boolean = false
) : IrFileSerializer(
    logger,
    declarationTable,
    expectDescriptorToSymbol,
    bodiesOnlyForInlines = bodiesOnlyForInlines,
    skipExpects = skipExpects
) {
    companion object {
        private val JS_EXPORT_FQN = FqName("kotlin.js.JsExport")
    }

    override fun backendSpecificExplicitRoot(node: IrAnnotationContainer): Boolean {
        return node.annotations.hasAnnotation(JS_EXPORT_FQN)
    }
}
