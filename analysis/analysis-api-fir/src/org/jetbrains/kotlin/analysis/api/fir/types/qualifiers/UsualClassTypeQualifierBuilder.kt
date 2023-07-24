/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types.qualifiers

import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.types.KtClassTypeQualifier
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.tryCollectDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment
import org.jetbrains.kotlin.fir.containingClassForLocal
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.renderWithType
import org.jetbrains.kotlin.fir.resolve.toFirRegularClass
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.LookupTagInternals
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal object UsualClassTypeQualifierBuilder {
    fun buildQualifiers(
        coneType: ConeClassLikeTypeImpl,
        builder: KtSymbolByFirBuilder
    ): List<KtClassTypeQualifier.KtResolvedClassTypeQualifier> {

        val classSymbolToRender = coneType.lookupTag.toSymbol(builder.rootSession)
            ?: errorWithFirSpecificEntries("ConeClassLikeTypeImpl is not resolved to symbol for on-error type", coneType = coneType) {
                withEntry("useSiteSession", builder.rootSession) { it.toString() }
            }


        if (classSymbolToRender !is FirRegularClassSymbol) {
            return listOf(
                KtClassTypeQualifier.KtResolvedClassTypeQualifier(
                    builder.classifierBuilder.buildClassifierSymbol(classSymbolToRender),
                    coneType.typeArguments.map { builder.typeBuilder.buildTypeProjection(it) },
                    builder.token
                )
            )
        }

        val designation = classSymbolToRender.fir.let {
            val nonLocalDesignation = it.tryCollectDesignation()
            nonLocalDesignation?.toSequence(includeTarget = true)?.toList()
                ?: collectDesignationPathForLocal(it)
        }


        var typeParametersLeft = coneType.typeArguments.size

        fun needToRenderTypeParameters(index: Int): Boolean {
            if (typeParametersLeft <= 0) return false
            return index == designation.lastIndex ||
                    (designation[index] as? FirRegularClass)?.isInner == true ||
                    (designation[index + 1] as? FirRegularClass)?.isInner == true
        }

        val result = mutableListOf<KtClassTypeQualifier.KtResolvedClassTypeQualifier>()
        designation.forEachIndexed { index, currentClass ->
            check(currentClass is FirRegularClass)
            val typeParameters = if (needToRenderTypeParameters(index)) {
                val typeParametersCount = currentClass.typeParameters.count { it is FirTypeParameter }
                val begin = typeParametersLeft - typeParametersCount
                val end = typeParametersLeft
                check(begin >= 0)
                typeParametersLeft -= typeParametersCount
                coneType.typeArguments.slice(begin until end).map { builder.typeBuilder.buildTypeProjection(it) }
            } else emptyList()
            result += KtClassTypeQualifier.KtResolvedClassTypeQualifier(
                builder.classifierBuilder.buildClassifierSymbol(currentClass.symbol),
                typeParameters,
                builder.token
            )
        }
        return result
    }

    private fun FirRegularClass.collectForLocal(): List<FirClassLikeDeclaration> {
        require(isLocal)
        var containingClassLookUp = containingClassForLocal()
        val designation = mutableListOf<FirClassLikeDeclaration>(this)
        @OptIn(LookupTagInternals::class)
        while (containingClassLookUp != null && containingClassLookUp.classId.isLocal) {
            val currentClass = containingClassLookUp.toFirRegularClass(moduleData.session) ?: break
            designation.add(currentClass)
            containingClassLookUp = currentClass.containingClassForLocal()
        }
        return designation.asReversed()
    }

    private fun collectDesignationPathForLocal(declaration: FirClassLikeDeclaration): List<FirDeclaration> {
        checkWithAttachment(
            declaration.isLocal,
            message = { "${declaration::class} is not local" }
        ) {
            withFirEntry("firDeclaration", declaration)
        }
        return when (declaration) {
            is FirAnonymousObject -> listOf(declaration)
            is FirRegularClass -> declaration.collectForLocal()
            is FirTypeAlias -> listOf(declaration) // TODO: handle type aliases
            else -> errorWithAttachment("Invalid declaration ${declaration::class}") {
                withFirEntry("declaration", declaration)
            }
        }
    }
}