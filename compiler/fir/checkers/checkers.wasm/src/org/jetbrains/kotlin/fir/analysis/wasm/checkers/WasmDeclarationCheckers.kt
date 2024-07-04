/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyAccessorChecker
import org.jetbrains.kotlin.fir.analysis.wasm.checkers.declaration.*
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.declaration.FirJsExportAnnotationChecker
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.declaration.FirWebCommonExternalPropertyAccessorChecker

object WasmBaseDeclarationCheckers : DeclarationCheckers() {
    override val classCheckers: Set<FirClassChecker>
        get() = setOf(
            FirWasmExternalInheritanceChecker.Regular,
            FirWasmExternalInheritanceChecker.ForExpectClass,
        )

    override val basicDeclarationCheckers: Set<FirBasicDeclarationChecker>
        get() = setOf(
            FirWasmImportAnnotationChecker,
            FirWasmExportAnnotationChecker,
            FirWasmExternalChecker,
        )

    override val propertyAccessorCheckers: Set<FirPropertyAccessorChecker>
        get() = setOf(
            FirWebCommonExternalPropertyAccessorChecker,
        )
}

object WasmJsDeclarationCheckers : DeclarationCheckers() {
    override val basicDeclarationCheckers: Set<FirBasicDeclarationChecker>
        get() = setOf(
            FirWasmJsInteropTypesChecker,
            FirWasmJsFunAnnotationChecker,
            FirJsExportAnnotationChecker,
            FirWasmJsModuleChecker,
            FirWasmExternalFileChecker,
            FirWasmJsAssociatedObjectChecker,
        )
}

object WasmWasiDeclarationCheckers : DeclarationCheckers() {
    override val basicDeclarationCheckers: Set<FirBasicDeclarationChecker>
        get() = setOf(
            FirWasmWasiExternalDeclarationChecker,
        )
}
