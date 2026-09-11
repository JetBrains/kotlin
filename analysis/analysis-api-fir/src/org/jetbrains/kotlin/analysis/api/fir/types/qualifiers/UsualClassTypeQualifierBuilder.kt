/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types.qualifiers

import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.impl.base.types.KaBaseResolvedClassTypeQualifier
import org.jetbrains.kotlin.analysis.api.types.KaResolvedClassTypeQualifier
import org.jetbrains.kotlin.analysis.api.types.KaTypeProjection
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.toSequence
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.tryCollectDesignationWithOptionalFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.fir.containingClassForLocal
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment

internal object UsualClassTypeQualifierBuilder {
    fun buildQualifiers(
        coneType: ConeClassLikeTypeImpl,
        builder: KaSymbolByFirBuilder
    ): List<KaResolvedClassTypeQualifier> {
        val coneTypeClassSymbol = coneType.lookupTag.toSymbol(builder.rootSession)
            ?: errorWithFirSpecificEntries("ConeClassLikeTypeImpl is not resolved to symbol for on-error type", coneType = coneType) {
                withEntry("useSiteSession", builder.rootSession) { it.toString() }
            }

        val designation = coneTypeClassSymbol.fir.let {
            val nonLocalDesignation = it.tryCollectDesignationWithOptionalFile()
            nonLocalDesignation?.toSequence(includeTarget = true)?.toList() ?: collectDesignationPathForLocal(it)
        }.filterIsInstance<FirClassLikeDeclaration>()

        /**
         * Returns a number of own type parameters for [this].
         * In general, it should only count [FirTypeParameter], i.e., type parameters that are declared right on this class.
         * [FirOuterClassTypeParameterRef] and [FirConstructedClassTypeParameterRef] should be ignored.
         */
        fun FirClassLikeDeclaration.numberOfOwnParameters() = typeParameters.count { it is FirTypeParameter }

        /**
         * Type arguments are only rendered for the type's own class and for the chain of its `inner` containers,
         * as only those may have type arguments in a qualified type reference.
         */
        fun shouldRegisterTypeParametersForDesignationPart(index: Int): Boolean {
            return index == designation.lastIndex || designation[index].isInner || designation[index + 1].isInner
        }

        val ownTypeParametersCountsByDesignation = designation.mapIndexed { index, designationClass ->
            if (shouldRegisterTypeParametersForDesignationPart(index)) designationClass.numberOfOwnParameters() else 0
        }
        var restTypeArguments = coneType.typeArguments.asList()

        // The designation is ordered outermost-first and the arguments innermost-first, so every part takes its own
        // ones from the back of what is left.
        fun takeTypeArguments(count: Int): List<KaTypeProjection> {
            if (count == 0 || restTypeArguments.isEmpty()) return emptyList()
            val taken = restTypeArguments.takeLast(count)
            restTypeArguments = restTypeArguments.dropLast(count)
            return taken.map { builder.typeBuilder.buildTypeProjection(it) }
        }

        return designation.mapIndexed { index, currentClass ->
            KaBaseResolvedClassTypeQualifier(
                builder.classifierBuilder.buildClassifierSymbol(currentClass.symbol),
                takeTypeArguments(ownTypeParametersCountsByDesignation[index]),
            )
        }
    }

    private fun FirClassLikeDeclaration.collectForLocal(): List<FirClassLikeDeclaration> {
        require(isLocal)
        var containingClassLookUp = containingClassForLocal()
        val designation = mutableListOf(this)
        var currentClass = containingClassLookUp?.toRegularClassSymbol(moduleData.session)?.fir

        while (containingClassLookUp != null && currentClass?.isLocal == true) {
            designation.add(currentClass)
            containingClassLookUp = currentClass.containingClassForLocal()
            currentClass = containingClassLookUp?.toRegularClassSymbol(moduleData.session)?.fir
        }
        return designation.asReversed()
    }

    private fun collectDesignationPathForLocal(declaration: FirClassLikeDeclaration): List<FirClassLikeDeclaration> {
        checkWithAttachment(
            declaration.isLocal,
            message = { "${declaration::class} is not local" }
        ) {
            withFirEntry("firDeclaration", declaration)
        }
        return when (declaration) {
            is FirAnonymousObject -> listOf(declaration)
            is FirRegularClass,
            is FirTypeAlias
                -> declaration.collectForLocal()
        }
    }
}
