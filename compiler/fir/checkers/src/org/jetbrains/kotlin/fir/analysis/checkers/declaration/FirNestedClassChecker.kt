/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.NESTED_CLASS_NOT_ALLOWED
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isLocal

// No need to visit anonymous object since an anonymous object is always inner. This aligns with
// compiler/frontend/src/org/jetbrains/kotlin/resolve/ModifiersChecker.java:198
object FirNestedClassChecker : FirRegularClassChecker() {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        // Local enums / objects / companion objects are handled with different diagnostic codes.
        if ((declaration.classKind.isSingleton || declaration.classKind == ClassKind.ENUM_CLASS) && declaration.isLocal) return
        val containingDeclaration = context.containingDeclarations.lastOrNull() as? FirClass ?: return

        // Since 1.3, enum entries can contain inner classes only.
        // Companion objects are reported with code WRONG_MODIFIER_CONTAINING_DECLARATION instead
        if (containingDeclaration.classKind == ClassKind.ENUM_ENTRY && !declaration.isInner && !declaration.isCompanion) {
            reporter.reportOn(declaration.source, NESTED_CLASS_NOT_ALLOWED, declaration.description, context)
            return
        }

        val containerIsLocal = containingDeclaration.effectiveVisibility == EffectiveVisibility.Local

        if (!declaration.isInner && (containingDeclaration.isInner || containerIsLocal || context.isInsideAnonymousObject)) {
            reporter.reportOn(declaration.source, NESTED_CLASS_NOT_ALLOWED, declaration.description, context)
        }
    }

    private val CheckerContext.isInsideAnonymousObject get() = containingDeclarations.any { it is FirAnonymousObject }

    // Note: here we don't differentiate anonymous object like in FE1.0
    // (org.jetbrains.kotlin.resolve.ModifiersChecker.DetailedClassKind) because this case has been ruled out in the first place.
    private val FirRegularClass.description: String
        get() = when (classKind) {
            ClassKind.CLASS -> "Class"
            ClassKind.INTERFACE -> "Interface"
            ClassKind.ENUM_CLASS -> "Enum class"
            ClassKind.ENUM_ENTRY -> "Enum entry"
            ClassKind.ANNOTATION_CLASS -> "Annotation class"
            ClassKind.OBJECT -> if (this.isCompanion) "Companion object" else "Object"
        }
}
