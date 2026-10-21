/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.declaration.*
import org.jetbrains.kotlin.fir.analysis.wasm.checkers.declaration.*
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.declaration.FirJsExportAnnotationChecker
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.declaration.FirWebCommonExternalPropertyAccessorChecker
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.declaration.FirWebCommonNativeInvokeChecker

object WasmBaseDeclarationCheckers : DeclarationCheckers() {
    override val classCheckers: Set<FirClassChecker> = setOf(
        FirWasmExternalInheritanceChecker.Regular,
        FirWasmExternalInheritanceChecker.ForExpectClass,
    )

    override val basicDeclarationCheckers: Set<FirBasicDeclarationChecker> = setOf(
        FirWasmImportAnnotationChecker,
        FirWasmExportAnnotationChecker,
        FirWasmExternalChecker,
    )

    override val propertyAccessorCheckers: Set<FirPropertyAccessorChecker> = setOf(
        FirWebCommonExternalPropertyAccessorChecker,
    )

    override val propertyCheckers: Set<FirPropertyChecker> = setOf(
        FirWasmEagerInitializationChecker,
    )
}

object WasmJsDeclarationCheckers : DeclarationCheckers() {
    override val basicDeclarationCheckers: Set<FirBasicDeclarationChecker> = setOf(
        FirWasmJsInteropTypesChecker,
        FirWasmJsFunAnnotationChecker,
        FirJsExportAnnotationChecker,
        FirWasmJsModuleChecker,
        FirWasmExternalFileChecker,
        FirWasmJsAssociatedObjectChecker,
    )

    override val namedFunctionCheckers: Set<FirNamedFunctionChecker> = setOf(
        FirWebCommonNativeInvokeChecker,
    )

    override val propertyCheckers: Set<FirPropertyChecker> = setOf(
        FirWasmJsVarInJsModuleFileChecker,
    )
}

object WasmWasiDeclarationCheckers : DeclarationCheckers() {
    override val basicDeclarationCheckers: Set<FirBasicDeclarationChecker> = setOf(
        FirWasmWasiExternalDeclarationChecker,
    )
}
