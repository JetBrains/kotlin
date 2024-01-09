/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.declaration.*
import org.jetbrains.kotlin.fir.analysis.js.checkers.declaration.*
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.declaration.FirJsExportAnnotationChecker

object JsDeclarationCheckers : DeclarationCheckers() {
    override val functionCheckers: Set<FirFunctionChecker>
        get() = setOf(
            FirJsInheritanceFunctionChecker.Regular,
            FirJsInheritanceFunctionChecker.ForExpectClass,
        )

    override val basicDeclarationCheckers: Set<FirBasicDeclarationChecker>
        get() = setOf(
            FirJsModuleChecker,
            FirJsRuntimeAnnotationChecker,
            FirJsExternalChecker,
            FirJsExternalFileChecker,
            FirJsNameChecker,
            FirJsExportAnnotationChecker,
            FirJsExportDeclarationChecker,
            FirJsBuiltinNameClashChecker,
            FirJsNameCharsChecker
        )

    override val classCheckers: Set<FirClassChecker>
        get() = setOf(
            FirJsMultipleInheritanceChecker.Regular,
            FirJsMultipleInheritanceChecker.ForExpectClass,
            FirJsDynamicDeclarationChecker,
            FirJsInheritanceClassChecker.Regular,
            FirJsInheritanceClassChecker.ForExpectClass,
            FirJsExternalInheritorOnlyChecker.Regular,
            FirJsExternalInheritorOnlyChecker.ForExpectClass,
            FirJsNameClashClassMembersChecker.Regular,
            FirJsNameClashClassMembersChecker.ForExpectClass,
        )

    override val simpleFunctionCheckers: Set<FirSimpleFunctionChecker>
        get() = setOf(
            FirJsNativeInvokeChecker,
            FirJsNativeGetterChecker,
            FirJsNativeSetterChecker,
        )

    override val propertyCheckers: Set<FirPropertyChecker>
        get() = setOf(
            FirJsPropertyDelegationByDynamicChecker
        )

    override val fileCheckers: Set<FirFileChecker>
        get() = setOf(
            FirJsPackageDirectiveChecker,
            FirJsNameClashFileTopLevelDeclarationsChecker
        )
}
