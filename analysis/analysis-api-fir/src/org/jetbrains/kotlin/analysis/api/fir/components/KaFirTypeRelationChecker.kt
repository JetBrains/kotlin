/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaSubtypingErrorTypePolicy
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirType
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.isSubclassOf
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseTypeRelationChecker
import org.jetbrains.kotlin.analysis.api.lifetime.assertIsValidAndAccessible
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.AbstractTypeChecker

internal class KaFirTypeRelationChecker(
    override val analysisSessionProvider: () -> KaFirSession
) : KaBaseTypeRelationChecker<KaFirSession>(), KaFirSessionComponent {
    override fun KaType.semanticallyEquals(other: KaType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean = withValidityAssertion {
        other.assertIsValidAndAccessible()
        check(this is KaFirType)
        check(other is KaFirType)
        return AbstractTypeChecker.equalTypes(
            createTypeCheckerContext(errorTypePolicy),
            this.coneType,
            other.coneType,
        )
    }

    override fun KaType.isSubtypeOf(supertype: KaType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean = withValidityAssertion {
        supertype.assertIsValidAndAccessible()
        check(this is KaFirType)
        check(supertype is KaFirType)
        return AbstractTypeChecker.isSubtypeOf(
            createTypeCheckerContext(errorTypePolicy),
            this.coneType,
            supertype.coneType,
        )
    }

    override fun KaClassType.isClassSubtypeOf(classId: ClassId, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean {
        val superclassSymbol = analysisSession.firSession.symbolProvider.getClassLikeSymbolByClassId(classId)
            ?: return errorTypePolicy == KaSubtypingErrorTypePolicy.LENIENT

        return isClassSubtypeOf(superclassSymbol, errorTypePolicy)
    }

    override fun KaClassType.isClassSubtypeOf(symbol: KaClassLikeSymbol, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean =
        isClassSubtypeOf(symbol.firSymbol, errorTypePolicy)

    private fun KaClassType.isClassSubtypeOf(
        superclassSymbol: FirClassLikeSymbol<*>,
        errorTypePolicy: KaSubtypingErrorTypePolicy,
    ): Boolean {
        require(this is KaFirType)

        val useSiteSession = analysisSession.firSession

        // We have to prepare the type to e.g. expand type aliases to be in line with `equalTypes` and `isSubtypeOf`.
        val preparedType = AbstractTypeChecker.prepareType(useSiteSession.typeContext, coneType)
        if (preparedType !is ConeClassLikeType) return false

        val classSymbol = preparedType.lookupTag.toRegularClassSymbol(useSiteSession)
            ?: return errorTypePolicy == KaSubtypingErrorTypePolicy.LENIENT

        val expandedSuperclassSymbol = superclassSymbol.fullyExpandedClass(useSiteSession)
            ?: return errorTypePolicy == KaSubtypingErrorTypePolicy.LENIENT

        return classSymbol == expandedSuperclassSymbol || isSubclassOf(
            classSymbol.fir,
            expandedSuperclassSymbol.fir,
            useSiteSession,
            allowIndirectSubtyping = true,
        )
    }
}
