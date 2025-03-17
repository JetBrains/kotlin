/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassLikeSymbol
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.isAny
import org.jetbrains.kotlin.fir.types.isNullableAny
import org.jetbrains.kotlin.fir.resolve.toSymbol

/**
 * Check that the given class does not inherit from class or implements interface that is
 * marked as HiddenFromObjC (aka "marked with annotation that is marked as HidesFromObjC").
 */
object FirNativeHiddenFromObjCInheritanceChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        // Enum entries inherit from their enum class.
        if (declaration.classKind == ClassKind.ENUM_ENTRY) {
            return
        }
        // Non-public types do not leak to Objective-C API surface, so it is OK for them
        // to inherit from hidden types.
        if (!declaration.visibility.isPublicAPI) return
        val session = context.session
        // No need to report anything on class that is hidden itself.
        if (checkIsHiddenFromObjC(declaration.symbol, session)) {
            return
        }

        val superTypes = declaration.superConeTypes
            .filterNot { it.isAny || it.isNullableAny }
            .mapNotNull { it.toSymbol(session) }

        superTypes.firstOrNull { st -> checkIsHiddenFromObjC(st, session) }?.let {
            reporter.reportOn(declaration.source, FirNativeErrors.SUBTYPE_OF_HIDDEN_FROM_OBJC, context)
        }
    }
}

private fun checkContainingClassIsHidden(classSymbol: FirClassLikeSymbol<*>, session: FirSession): Boolean {
    return classSymbol.getContainingClassSymbol()?.let {
        if (checkIsHiddenFromObjC(it, session)) {
            true
        } else {
            checkContainingClassIsHidden(it, session)
        }
    } ?: false
}

private fun checkIsHiddenFromObjC(classSymbol: FirClassLikeSymbol<*>, session: FirSession): Boolean {
    classSymbol.resolvedAnnotationsWithClassIds.forEach { annotation ->
        val annotationClass = annotation.toAnnotationClassLikeSymbol(session) ?: return@forEach

        // `classSymbol` might be a checked class supertype, so its annotation arguments might stay unresolved.
        // This means meta annotations also don't have to be fully resolved.
        val objCExportMetaAnnotations = annotationClass.resolvedAnnotationsWithClassIds.findMetaAnnotations(session)
        if (objCExportMetaAnnotations.hidesFromObjCAnnotation != null) {
            return true
        }
    }
    return checkContainingClassIsHidden(classSymbol, session)
}
