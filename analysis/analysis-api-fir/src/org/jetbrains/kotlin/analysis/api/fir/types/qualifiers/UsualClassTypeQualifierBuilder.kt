/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types.qualifiers

import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.impl.base.types.KaBaseResolvedClassTypeQualifier
import org.jetbrains.kotlin.analysis.api.types.KaResolvedClassTypeQualifier
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.toSequence
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.tryCollectDesignationWithOptionalFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.fir.containingClassForLocal
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.resolve.toFirRegularClass
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.LookupTagInternals
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal object UsualClassTypeQualifierBuilder {
    fun buildQualifiers(
        coneType: ConeClassLikeTypeImpl,
        builder: KaSymbolByFirBuilder
    ): List<KaResolvedClassTypeQualifier> {

        val classSymbolToRender = coneType.lookupTag.toSymbol(builder.rootSession)
            ?: errorWithFirSpecificEntries("ConeClassLikeTypeImpl is not resolved to symbol for on-error type", coneType = coneType) {
                withEntry("useSiteSession", builder.rootSession) { it.toString() }
            }


        if (classSymbolToRender !is FirRegularClassSymbol) {
            return listOf(
                KaBaseResolvedClassTypeQualifier(
                    builder.classifierBuilder.buildClassifierSymbol(classSymbolToRender),
                    coneType.typeArguments.map { builder.typeBuilder.buildTypeProjection(it) },
                )
            )
        }

        val designation = classSymbolToRender.fir.let {
            val nonLocalDesignation = it.tryCollectDesignationWithOptionalFile()
            nonLocalDesignation?.toSequence(includeTarget = true)?.toList() ?: collectDesignationPathForLocal(it)
        }.filterIsInstance<FirRegularClass>()

        var typeParametersLeft = coneType.typeArguments.size

        fun needToRenderTypeParameters(index: Int): Boolean {
            if (typeParametersLeft <= 0) return false
            return index == designation.lastIndex || designation[index].isInner || designation[index + 1].isInner
        }

        val result = mutableListOf<KaResolvedClassTypeQualifier>()
        designation.forEachIndexed { index, currentClass ->
            val typeParameters = if (needToRenderTypeParameters(index)) {
                val typeParametersCount = currentClass.typeParameters.count { it is FirTypeParameter }
                val begin = typeParametersLeft - typeParametersCount
                val end = typeParametersLeft
                check(begin >= 0)
                typeParametersLeft -= typeParametersCount
                coneType.typeArguments.slice(begin until end).map { builder.typeBuilder.buildTypeProjection(it) }
            } else emptyList()
            result += KaBaseResolvedClassTypeQualifier(
                builder.classifierBuilder.buildClassifierSymbol(currentClass.symbol),
                typeParameters,
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

    private fun collectDesignationPathForLocal(declaration: FirClassLikeDeclaration): List<FirClassLikeDeclaration> {
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