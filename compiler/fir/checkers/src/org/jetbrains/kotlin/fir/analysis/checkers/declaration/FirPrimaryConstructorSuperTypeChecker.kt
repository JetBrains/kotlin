/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClass
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.isInterface
import org.jetbrains.kotlin.fir.declarations.primaryConstructor
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirImplicitAnyTypeRef
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull

/** Checker on super type declarations in the primary constructor of a class declaration. */
object FirPrimaryConstructorSuperTypeChecker : FirRegularClassChecker() {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.isInterface) {
            for (superTypeRef in declaration.superTypeRefs) {
                val source = superTypeRef.source ?: continue
                if (source.treeStructure.getParent(source.lighterASTNode)?.tokenType == KtNodeTypes.CONSTRUCTOR_CALLEE) {
                    reporter.reportOn(source, FirErrors.SUPERTYPE_INITIALIZED_IN_INTERFACE, context)
                }
            }
            return
        }

        val primaryConstructor = declaration.primaryConstructor

        if (primaryConstructor == null) {
            checkSupertypeInitializedWithoutPrimaryConstructor(declaration, reporter, context)
        } else {
            checkSuperTypeNotInitialized(primaryConstructor, declaration, context, reporter)
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
        primaryConstructor: FirConstructor,
        regularClass: FirRegularClass,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val containingClass = context.containingDeclarations.lastIsInstanceOrNull<FirRegularClass>()
        val delegatedConstructorCall = primaryConstructor.delegatedConstructor ?: return
        // No need to check implicit call to the constructor of `kotlin.Any`.
        val constructedTypeRef = delegatedConstructorCall.constructedTypeRef
        if (constructedTypeRef is FirImplicitAnyTypeRef) return
        val superClass = constructedTypeRef.coneType.toRegularClass(context.session) ?: return
        // Subclassing a singleton should be reported as SINGLETON_IN_SUPERTYPE
        if (superClass.classKind.isSingleton) return
        if (regularClass.isEffectivelyExpect(containingClass, context) ||
            regularClass.isEffectivelyExternal(containingClass, context)
        ) {
            return
        }
        val delegatedCallSource = delegatedConstructorCall.source ?: return
        if (delegatedCallSource.kind !is FirFakeSourceElementKind) return
        if (superClass.symbol.classId == StandardClassIds.Enum) return
        if (delegatedCallSource.elementType != KtNodeTypes.SUPER_TYPE_CALL_ENTRY) {
            reporter.reportOn(constructedTypeRef.source, FirErrors.SUPERTYPE_NOT_INITIALIZED, context)
        }
    }

    /**
     * SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR is reported on code like the following, where `B` does not have a primary
     * constructor, in which case, one can not call the delegated constructor of `A` in the super type list. `B` doesn't have a primary
     * constructor because it doesn't declare it, nor is it implicitly created in presence of a explicitly declared constructor inside the
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
        regularClass: FirRegularClass,
        reporter: DiagnosticReporter,
        context: CheckerContext
    ) {
        for (superTypeRef in regularClass.superTypeRefs) {
            val source = superTypeRef.source ?: continue
            if (source.treeStructure.getParent(source.lighterASTNode)?.tokenType == KtNodeTypes.CONSTRUCTOR_CALLEE) {
                reporter.reportOn(regularClass.source, FirErrors.SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR, context)
            }
        }
    }
}
