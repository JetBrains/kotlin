/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.IrFileSerializer
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.FqName

class JsIrFileSerializer(
    messageLogger: IrMessageLogger,
    declarationTable: DeclarationTable,
    expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>,
    skipExpects: Boolean,
    bodiesOnlyForInlines: Boolean = false,
    icMode: Boolean = false,
    allowNullTypes: Boolean = false,
    allowErrorStatementOrigins: Boolean = false,
) : IrFileSerializer(
    messageLogger,
    declarationTable,
    expectDescriptorToSymbol,
    bodiesOnlyForInlines = bodiesOnlyForInlines,
    skipExpects = skipExpects,
    skipMutableState = icMode,
    allowNullTypes = allowNullTypes,
    allowErrorStatementOrigins = allowErrorStatementOrigins,
) {
    companion object {
        private val JS_EXPORT_FQN = FqName("kotlin.js.JsExport")
    }

    override fun backendSpecificExplicitRoot(node: IrAnnotationContainer): Boolean {
        return node.annotations.hasAnnotation(JS_EXPORT_FQN)
    }
}
