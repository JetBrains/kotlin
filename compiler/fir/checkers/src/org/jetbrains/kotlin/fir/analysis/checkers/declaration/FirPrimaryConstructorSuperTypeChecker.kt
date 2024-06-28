/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.descriptors.isEnumEntry
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.SourceNavigator
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.primaryConstructorSuperTypePlatformSupport
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.declarations.utils.isErrorPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.impl.FirImplicitAnyTypeRef
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull

/** Checker on super type declarations in the primary constructor of a class declaration. */
object FirPrimaryConstructorSuperTypeChecker : FirClassChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.isInterface) {
            with(SourceNavigator.forElement(declaration)) {
                for (superTypeRef in declaration.superTypeRefs) {
                    if (superTypeRef.isInConstructorCallee()) {
                        reporter.reportOn(superTypeRef.source, FirErrors.SUPERTYPE_INITIALIZED_IN_INTERFACE, context)
                    }
                }
            }
            return
        }

        if (declaration.classKind.isEnumEntry) return

        val primaryConstructorSymbol = declaration.primaryConstructorIfAny(context.session)

        if (primaryConstructorSymbol == null || primaryConstructorSymbol.isErrorPrimaryConstructor) {
            checkSupertypeInitializedWithoutPrimaryConstructor(declaration, reporter, context)
        } else {
            checkSuperTypeNotInitialized(primaryConstructorSymbol, declaration, context, reporter)
        }
    }

    /**
     *  SUPERTYPE_NOT_INITIALIZED is reported on code like the following. It's skipped if `A` has `()` after it, in which case any
     *  diagnostics for that constructor call will be reported, if applicable.
     *
     *  ```
     *  open class A
     *  class B : <!SUPERTYPE_NOT_INITIALIZED>A<!>
     *  ```
     */
    private fun checkSuperTypeNotInitialized(
        primaryConstructorSymbol: FirConstructorSymbol,
        regularClass: FirClass,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val containingClass = context.containingDeclarations.lastIsInstanceOrNull<FirRegularClass>()
        val delegatedConstructorCall = primaryConstructorSymbol.resolvedDelegatedConstructorCall ?: return
        // No need to check implicit call to the constructor of `kotlin.Any`.
        val constructedTypeRef = delegatedConstructorCall.constructedTypeRef
        if (constructedTypeRef is FirImplicitAnyTypeRef) return
        val superClassSymbol = constructedTypeRef.coneType.toRegularClassSymbol(context.session) ?: return
        // Subclassing a singleton should be reported as SINGLETON_IN_SUPERTYPE
        if (superClassSymbol.classKind.isSingleton) return
        if (regularClass.isEffectivelyExpect(containingClass, context) ||
            regularClass.isEffectivelyExternal(containingClass, context)
        ) {
            return
        }
        val delegatedCallSource = delegatedConstructorCall.source ?: return
        if (delegatedCallSource.kind !is KtFakeSourceElementKind) return
        val supertypesToSkip = context.session.primaryConstructorSuperTypePlatformSupport
            .supertypesThatDontNeedInitializationInSubtypesConstructors
        if (superClassSymbol.classId in supertypesToSkip) return
        if (delegatedCallSource.elementType != KtNodeTypes.SUPER_TYPE_CALL_ENTRY) {
            reporter.reportOn(constructedTypeRef.source, FirErrors.SUPERTYPE_NOT_INITIALIZED, context)
        }
    }

    /**
     * SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR is reported on code like the following, where `B` does not have a primary
     * constructor, in which case, one can not call the delegated constructor of `A` in the super type list. `B` doesn't have a primary
     * constructor because it doesn't declare it, nor is it implicitly created in presence of an explicitly declared constructor inside the
     * class body.
     *
     * ```
     * open class A
     * class B : <!SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR>A()<!> {
     *   constructor()
     * }
     * ```
     */
    private fun checkSupertypeInitializedWithoutPrimaryConstructor(
        regularClass: FirClass,
        reporter: DiagnosticReporter,
        context: CheckerContext
    ) {
        with(SourceNavigator.forElement(regularClass)) {
            for (superTypeRef in regularClass.superTypeRefs) {
                if (superTypeRef.isInConstructorCallee()) {
                    reporter.reportOn(regularClass.source, FirErrors.SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR, context)
                }
            }
        }
    }
}
