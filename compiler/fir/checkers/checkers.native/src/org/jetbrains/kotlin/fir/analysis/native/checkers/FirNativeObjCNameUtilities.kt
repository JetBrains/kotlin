/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.retrieveDirectOverriddenOf
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object FirNativeObjCNameUtilities {
    private val objCNameClassId = ClassId.topLevel(FqName("kotlin.native.ObjCName"))
    private val swiftNameName = Name.identifier("swiftName")
    private val exactName = Name.identifier("exact")

    fun FirBasedSymbol<*>.getObjCNames(session: FirSession): List<ObjCName?> {
        lazyResolveToPhase(FirResolvePhase.ANNOTATION_ARGUMENTS)
        return when (this) {
            is FirFunctionSymbol<*> -> buildList {
                add((this@getObjCNames as FirBasedSymbol<*>).getObjCName(session))
                add(resolvedReceiverTypeRef?.getObjCName(session))
                add(receiverParameter?.getObjCName(session))
                valueParameterSymbols.forEach { add(it.getObjCName(session)) }
            }
            else -> listOf(getObjCName(session))
        }
    }

    private fun FirAnnotationContainer.getObjCName(session: FirSession): ObjCName? =
        getAnnotationByClassId(objCNameClassId, session)?.let(::ObjCName)

    private fun FirBasedSymbol<*>.getObjCName(session: FirSession): ObjCName? =
        getAnnotationByClassId(objCNameClassId, session)?.let(::ObjCName)

    class ObjCName(
        val annotation: FirAnnotation
    ) {
        val name: String? = annotation.getStringArgument(StandardNames.NAME)
        val swiftName: String? = annotation.getStringArgument(swiftNameName)
        val exact: Boolean = annotation.getBooleanArgument(exactName) ?: false

        override fun equals(other: Any?): Boolean =
            other is ObjCName && name == other.name && swiftName == other.swiftName && exact == other.exact

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + swiftName.hashCode()
            result = 31 * result + exact.hashCode()
            return result
        }
    }

    fun checkCallableMember(
        firTypeScope: FirTypeScope,
        memberSymbol: FirCallableSymbol<*>,
        declarationToReport: FirDeclaration,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val overriddenSymbols =
            firTypeScope.retrieveDirectOverriddenOf(memberSymbol).map { it.unwrapSubstitutionOverrides() }
        if (overriddenSymbols.isEmpty()) return
        val objCNames = overriddenSymbols.map { it.getFirstBaseSymbol(context).getObjCNames(context.session) }
        if (!objCNames.allNamesEquals()) {
            val containingDeclarations = overriddenSymbols.mapNotNull {
                it.containingClassLookupTag()?.toFirRegularClassSymbol(context.session)
            }
            reporter.reportOn(
                declarationToReport.source,
                FirNativeErrors.INCOMPATIBLE_OBJC_NAME_OVERRIDE,
                memberSymbol,
                containingDeclarations,
                context
            )
        }
    }

    private fun FirCallableSymbol<*>.getFirstBaseSymbol(context: CheckerContext): FirCallableSymbol<*> {
        val session = context.session
        val ownScope = containingClassLookupTag()?.toSymbol(session)?.fullyExpandedClass(session)?.unsubstitutedScope(context)
            ?: return this
        val overriddenMemberSymbols = ownScope.retrieveDirectOverriddenOf(this).map { it.unwrapSubstitutionOverrides() }
        return if (overriddenMemberSymbols.isEmpty()) this else overriddenMemberSymbols.first().getFirstBaseSymbol(context)
    }

    private fun List<List<ObjCName?>>.allNamesEquals(): Boolean {
        val first = this[0]
        for (i in 1 until size) {
            if (first != this[i]) return false
        }
        return true
    }
}
