/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.directOverriddenFunctions
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors
import org.jetbrains.kotlin.fir.backend.native.interop.getObjCInitMethod
import org.jetbrains.kotlin.fir.backend.native.interop.isKotlinObjCClass
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.resolve.getSuperClassSymbolOrAny
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.NativeStandardInteropNames.objCOverrideInitClassId

object FirNativeObjCOverrideInitChecker : FirClassChecker(MppCheckerKind.Platform) {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val session = context.session

        fun FirClassSymbol<*>.constructors(session: FirSession): List<FirConstructorSymbol> {
            val result = mutableListOf<FirConstructorSymbol>()
            session.declaredMemberScope(this, memberRequiredPhase = null).processDeclaredConstructors { result += it }
            return result
        }

        fun FirConstructorSymbol.overridesConstructor(other: FirConstructorSymbol): Boolean {
            val aParams = this.valueParameterSymbols
            val bParams = other.valueParameterSymbols
            if (aParams.size != bParams.size)
                return false
            return aParams.zip(bParams).all { (thisParameter, otherParameter) ->
                thisParameter.name == otherParameter.name && thisParameter.resolvedReturnType == otherParameter.resolvedReturnType
            }
        }

        fun checkCanGenerateOverrideInit(firClass: FirClass, constructor: FirConstructorSymbol) {
            val superClass = (firClass as FirRegularClass).symbol.getSuperClassSymbolOrAny(session)
            val superConstructors = superClass.constructors(session).filter {
                constructor.overridesConstructor(it)
            }.toList()

            val superConstructor: FirConstructorSymbol = superConstructors.singleOrNull() ?: run {
                if (superConstructors.isEmpty())
                    reporter.reportOn(
                        constructor.source,
                        FirNativeErrors.CONSTRUCTOR_DOES_NOT_OVERRIDE_ANY_SUPER_CONSTRUCTOR,
                        objCOverrideInitClassId.asSingleFqName(),
                        context
                    )
                else
                    reporter.reportOn(
                        constructor.source,
                        FirNativeErrors.CONSTRUCTOR_MATCHES_SEVERAL_SUPER_CONSTRUCTORS,
                        objCOverrideInitClassId.asSingleFqName(),
                        context
                    )
                return
            }

            val initMethod = superConstructor.getObjCInitMethod(session)!!

            // Remove fake overrides of this init method, also check for explicit overriding:
            firClass.symbol.processAllDeclaredCallables(context.session) {
                if (it is FirNamedFunctionSymbol &&
                    initMethod in it.directOverriddenFunctions(context) &&
                    !it.isSubstitutionOrIntersectionOverride
                ) {
                    reporter.reportOn(
                        constructor.source,
                        FirNativeErrors.CONSTRUCTOR_OVERRIDES_ALREADY_OVERRIDDEN_OBJC_INITIALIZER,
                        objCOverrideInitClassId.asSingleFqName(),
                        context
                    )
                }
            }
        }

        fun checkKotlinObjCClass(firClass: FirClass) {
            firClass.constructors(context.session).forEach { symbol ->
                if (symbol.hasAnnotation(objCOverrideInitClassId, session)) {
                    checkCanGenerateOverrideInit(firClass, symbol)
                }
            }
        }

        if (!declaration.isExpect && declaration.symbol.isKotlinObjCClass(context.session)) {
            checkKotlinObjCClass(declaration)
        }
    }
}
