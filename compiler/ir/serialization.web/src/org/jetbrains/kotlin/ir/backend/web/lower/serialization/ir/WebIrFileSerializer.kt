/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.web.lower.serialization.ir

import org.jetbrains.kotlin.backend.common.serialization.CompatibilityMode
import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.IrFileSerializer
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.FqName

class WebIrFileSerializer(
    messageLogger: IrMessageLogger,
    declarationTable: DeclarationTable,
    expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>,
    compatibilityMode: CompatibilityMode,
    skipExpects: Boolean,
    languageVersionSettings: LanguageVersionSettings,
    bodiesOnlyForInlines: Boolean = false,
    normalizeAbsolutePaths: Boolean,
    sourceBaseDirs: Collection<String>
) : IrFileSerializer(
    messageLogger,
    declarationTable,
    expectDescriptorToSymbol,
    compatibilityMode,
    languageVersionSettings,
    bodiesOnlyForInlines = bodiesOnlyForInlines,
    skipExpects = skipExpects,
    normalizeAbsolutePaths = normalizeAbsolutePaths,
    sourceBaseDirs = sourceBaseDirs
) {
    companion object {
        private val JS_EXPORT_FQN = FqName("kotlin.js.JsExport")
    }

    override fun backendSpecificExplicitRoot(node: IrAnnotationContainer): Boolean {
        return node.annotations.hasAnnotation(JS_EXPORT_FQN)
    }
}
