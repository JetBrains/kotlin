/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.CompatibilityMode
import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.IrFileSerializer
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.NativeRuntimeNames

class KonanIrFileSerializer(
    declarationTable: DeclarationTable,
    languageVersionSettings: LanguageVersionSettings,
    bodiesOnlyForInlines: Boolean = false,
    compatibilityMode: CompatibilityMode,
    normalizeAbsolutePaths: Boolean,
    sourceBaseDirs: Collection<String>,
    publicAbiOnly: Boolean = false,
) : IrFileSerializer(
    declarationTable,
    compatibilityMode,
    languageVersionSettings,
    publicAbiOnly = publicAbiOnly,
    bodiesOnlyForInlines = bodiesOnlyForInlines,
    normalizeAbsolutePaths = normalizeAbsolutePaths,
    sourceBaseDirs = sourceBaseDirs
) {

    override fun backendSpecificExplicitRoot(node: IrAnnotationContainer): Boolean {
        val classId = when (node) {
            is IrFunction -> NativeRuntimeNames.Annotations.exportForCppRuntimeClassId
            is IrClass -> NativeRuntimeNames.Annotations.exportTypeInfoClassId
            else -> return false
        }

        return node.hasAnnotation(classId)
    }

    override fun backendSpecificSerializeAllMembers(irClass: IrClass) = !KonanFakeOverrideClassFilter.needToConstructFakeOverrides(irClass)
}