/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.declaration.*
import org.jetbrains.kotlin.fir.analysis.js.checkers.declaration.*
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.declaration.FirJsExportAnnotationChecker
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.declaration.FirJsExportedActualMatchExpectChecker
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.declaration.FirMultipleJsExportDefaultAnnotationChecker
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.declaration.FirWebCommonExternalPropertyAccessorChecker
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.declaration.FirWebCommonNativeInvokeChecker

object JsDeclarationCheckers : DeclarationCheckers() {
    override val functionCheckers: Set<FirFunctionChecker> = setOf(
        FirJsInheritanceFunctionChecker.Regular,
        FirJsInheritanceFunctionChecker.ForExpectClass,
    )

    override val basicDeclarationCheckers: Set<FirBasicDeclarationChecker> = setOf(
        FirJsModuleChecker,
        FirJsRuntimeAnnotationChecker,
        FirJsExternalChecker,
        FirJsExternalFileChecker,
        FirJsNameChecker,
        FirJsSymbolChecker,
        FirJsExportAnnotationChecker,
        FirJsExportDeclarationChecker,
        FirJsExportedActualMatchExpectChecker,
        FirJsActualExternalInterfaceSuggestJsNoRuntimeChecker,
        FirJsBuiltinNameClashChecker,
        FirJsNameCharsChecker,
        FirJsStaticChecker,
        FirJsNoRuntimeDeclarationChecker,
    )

    override val regularClassCheckers: Set<FirRegularClassChecker> = setOf(
        FirDataClassNonExportableConstructorChecker
    )

    override val classCheckers: Set<FirClassChecker> = setOf(
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

    override val namedFunctionCheckers: Set<FirNamedFunctionChecker> = setOf(
        FirWebCommonNativeInvokeChecker,
        FirJsNativeGetterChecker,
        FirJsNativeSetterChecker,
    )

    override val propertyCheckers: Set<FirPropertyChecker> = setOf(
        FirJsPropertyDelegationByDynamicChecker,
        FirJsEagerInitializationChecker,
        FirJsVarInJsModuleFileChecker,
    )

    override val fileCheckers: Set<FirFileChecker> = setOf(
        FirJsPackageDirectiveChecker,
        FirMultipleJsExportDefaultAnnotationChecker,
        FirJsNameClashFileTopLevelDeclarationsChecker
    )

    override val propertyAccessorCheckers: Set<FirPropertyAccessorChecker> = setOf(
        FirWebCommonExternalPropertyAccessorChecker,
    )
}
