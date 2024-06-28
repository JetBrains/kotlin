/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.declaration.*

object NativeDeclarationCheckers : DeclarationCheckers() {
    override val basicDeclarationCheckers: Set<FirBasicDeclarationChecker>
        get() = setOf(
            FirNativeThrowsChecker.Regular,
            FirNativeThrowsChecker.ForExpectClass,
            FirNativeSharedImmutableChecker,
            FirNativeThreadLocalChecker,
            FirNativeIdentifierChecker,
            FirNativeObjCNameChecker,
        )

    override val functionCheckers: Set<FirFunctionChecker>
        get() = setOf(
            FirNativeObjcOverrideApplicabilityChecker
        )

    override val callableDeclarationCheckers: Set<FirCallableDeclarationChecker>
        get() = setOf(
            FirNativeObjCRefinementChecker,
            FirNativeObjCNameCallableChecker.Regular,
            FirNativeObjCNameCallableChecker.ForExpectClass,
        )

    override val classCheckers: Set<FirClassChecker>
        get() = setOf(
            FirNativeObjCRefinementOverridesChecker.Regular,
            FirNativeObjCRefinementOverridesChecker.ForExpectClass,
            FirNativeObjCNameOverridesChecker.Regular,
            FirNativeObjCNameOverridesChecker.ForExpectClass,
            FirNativeObjCOutletChecker,
            FirNativeObjCActionChecker,
            FirNativeObjCOverrideInitChecker,
        )

    override val regularClassCheckers: Set<FirRegularClassChecker>
        get() = setOf(
            FirNativeObjCRefinementAnnotationChecker,
            FirNativeHiddenFromObjCInheritanceChecker,
        )

    override val fileCheckers: Set<FirFileChecker>
        get() = setOf(
            FirNativePackageDirectiveChecker,
        )
}
