/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.ReturnValueCheckerMode
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirEmptyArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationCall
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.name.StandardClassIds

public class MustUseAnnotationMetadataProvider private constructor(val session: FirSession) : FirAdditionalMetadataProvider() {
    override fun findGeneratedAnnotationsFor(declaration: FirDeclaration): List<FirAnnotation> {
        if (!hasGeneratedAnnotationsFor(declaration)) return emptyList()
        return addMustUseValueAnnotation(
            declarationSymbol = declaration.symbol
        )
    }

    override fun hasGeneratedAnnotationsFor(declaration: FirDeclaration): Boolean {
        if (declaration is FirSimpleFunction || declaration is FirRegularClass) {
            return declaration.annotations.none { it.annotationTypeRef.coneTypeOrNull?.classId == mustUseReturnValueClassId }
        }
        return false
    }

    private val mustUseReturnValueClassId = StandardClassIds.Annotations.MustUseReturnValue
    private val mustUseValueClass by lazy { session.symbolProvider.getClassLikeSymbolByClassId(mustUseReturnValueClassId) as? FirRegularClassSymbol }


    private fun addMustUseValueAnnotation(
        declarationSymbol: FirBasedSymbol<*>,
    ): List<FirAnnotation> {
        val muvClassSymbol = mustUseValueClass ?: return emptyList()
        val mustUseValueCtor = muvClassSymbol.primaryConstructorIfAny(session) ?: return emptyList()

        val annCall = buildAnnotationCall {
            argumentList = FirEmptyArgumentList
            annotationTypeRef = buildResolvedTypeRef {
                coneType = muvClassSymbol.defaultType()
            }
            calleeReference = buildResolvedNamedReference {
                name = muvClassSymbol.name
                resolvedSymbol = mustUseValueCtor
            }
            annotationResolvePhase = FirAnnotationResolvePhase.CompilerRequiredAnnotations
//            source = declarationSymbol.source
//            useSiteTarget = useSite
            // origin = TODO: Do we need special kind of origin or .Source is enough here?

            containingDeclarationSymbol = declarationSymbol
        }
        return listOf(annCall)
    }

    override fun findMetadataExtensionsFor(declaration: FirDeclaration): Map<String, ByteArray> {
        return emptyMap()
    }

    companion object {
        public fun createIfFeatureEnabled(session: FirSession): MustUseAnnotationMetadataProvider? =
            if (session.languageVersionSettings.getFlag(AnalysisFlags.returnValueCheckerMode) == ReturnValueCheckerMode.FULL)
                MustUseAnnotationMetadataProvider(session)
            else null
    }
}
