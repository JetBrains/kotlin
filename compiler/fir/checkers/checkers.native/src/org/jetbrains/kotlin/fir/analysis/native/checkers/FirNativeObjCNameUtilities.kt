/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenSafe
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.unwrapSubstitutionOverrides
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
                add(receiverParameterSymbol?.getObjCName(session))
                valueParameterSymbols.forEach { add(it.getObjCName(session)) }
                contextParameterSymbols.forEach { add(it.getObjCName(session)) }
            }
            is FirPropertySymbol -> buildList {
                add((this@getObjCNames as FirBasedSymbol<*>).getObjCName(session))
                add(receiverParameterSymbol?.getObjCName(session))
                contextParameterSymbols.forEach { add(it.getObjCName(session)) }
            }
            else -> listOf(getObjCName(session))
        }
    }

    private fun FirAnnotationContainer.getObjCName(session: FirSession): ObjCName? =
        getAnnotationByClassId(objCNameClassId, session)?.let { ObjCName(it, session) }

    private fun FirBasedSymbol<*>.getObjCName(session: FirSession): ObjCName? =
        getAnnotationByClassId(objCNameClassId, session)?.let { ObjCName(it, session) }

    class ObjCName(val annotation: FirAnnotation, session: FirSession) {
        val name: String? = annotation.getStringArgument(StandardNames.NAME, session)
        val swiftName: String? = annotation.getStringArgument(swiftNameName, session)
        val exact: Boolean = annotation.getBooleanArgument(exactName, session) ?: false

        override fun equals(other: Any?): Boolean =
            other is ObjCName && name == other.name && swiftName == other.swiftName && exact == other.exact

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + swiftName.hashCode()
            result = 31 * result + exact.hashCode()
            return result
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun checkCallableMember(
        firTypeScope: FirTypeScope,
        memberSymbol: FirCallableSymbol<*>,
        declarationToReport: FirDeclaration,
    ) {
        val overriddenSymbols =
            firTypeScope.getDirectOverriddenSafe(memberSymbol).map { it.unwrapSubstitutionOverrides() }
        if (overriddenSymbols.isEmpty()) return
        val objCNames = overriddenSymbols.map { it.getFirstBaseSymbol().getObjCNames(context.session) }
        if (!objCNames.allNamesEquals()) {
            val containingDeclarations = overriddenSymbols.mapNotNull {
                it.containingClassLookupTag()?.toRegularClassSymbol()
            }
            reporter.reportOn(
                declarationToReport.source,
                FirNativeErrors.INCOMPATIBLE_OBJC_NAME_OVERRIDE,
                memberSymbol,
                containingDeclarations
            )
        }
    }

    context(context: CheckerContext)
    private fun FirCallableSymbol<*>.getFirstBaseSymbol(): FirCallableSymbol<*> {
        val session = context.session
        val ownScope = containingClassLookupTag()?.toSymbol()?.fullyExpandedClass()?.unsubstitutedScope()
            ?: return this
        val overriddenMemberSymbols = ownScope.getDirectOverriddenSafe(this).map { it.unwrapSubstitutionOverrides() }
        return if (overriddenMemberSymbols.isEmpty()) this else overriddenMemberSymbols.first().getFirstBaseSymbol()
    }

    private fun List<List<ObjCName?>>.allNamesEquals(): Boolean {
        val first = this[0]
        for (i in 1 until size) {
            if (first != this[i]) return false
        }
        return true
    }
}
