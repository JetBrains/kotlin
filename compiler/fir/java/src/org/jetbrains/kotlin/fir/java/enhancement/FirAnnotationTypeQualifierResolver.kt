/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.enhancement

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.convertAnnotationsToFir
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.load.java.AbstractAnnotationTypeQualifierResolver
import org.jetbrains.kotlin.load.java.JavaModuleAnnotationsProvider
import org.jetbrains.kotlin.load.java.JavaTypeEnhancementState
import org.jetbrains.kotlin.load.java.JavaTypeQualifiersByElementType
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.DEFAULT_ANNOTATION_MEMBER_NAME
import org.jetbrains.kotlin.name.FqName

class FirAnnotationTypeQualifierResolver(
    private val session: FirSession,
    javaTypeEnhancementState: JavaTypeEnhancementState,
    private val javaModuleAnnotationsProvider: JavaModuleAnnotationsProvider,
) : AbstractAnnotationTypeQualifierResolver<FirAnnotation>(javaTypeEnhancementState), FirSessionComponent {

    override val FirAnnotation.metaAnnotations: Iterable<FirAnnotation>
        get() = coneClassLikeType?.lookupTag?.toSymbol(session)?.fir?.annotations.orEmpty()

    override val FirAnnotation.key: Any
        get() = coneClassLikeType!!.lookupTag

    override val FirAnnotation.fqName: FqName?
        get() = coneClassLikeType?.lookupTag?.classId?.asSingleFqName()

    override fun FirAnnotation.enumArguments(onlyValue: Boolean): Iterable<String> =
        argumentMapping.mapping.values.flatMap { argument ->
            if (!onlyValue || argument !is FirNamedArgumentExpression || argument.name == DEFAULT_ANNOTATION_MEMBER_NAME)
                argument.toEnumNames()
            else
                emptyList()
        }

    private fun FirExpression.toEnumNames(): List<String> =
        when (this) {
            is FirArrayOfCall -> arguments.flatMap { it.toEnumNames() }
            is FirVarargArgumentsExpression -> arguments.flatMap { it.toEnumNames() }
            else -> listOfNotNull(toResolvedCallableSymbol()?.callableId?.callableName?.asString())
        }

    fun extractDefaultQualifiers(firClass: FirRegularClass): JavaTypeQualifiersByElementType? {
        val classId = firClass.symbol.classId
        val outerClassId = classId.outerClassId
        val parentQualifiers = if (outerClassId != null) {
            (session.symbolProvider.getClassLikeSymbolByClassId(outerClassId)?.fir as? FirRegularClass)
                ?.let { extractDefaultQualifiers(it) }
        } else {
            val forModule = javaModuleAnnotationsProvider.getAnnotationsForModuleOwnerOfClass(classId)
                ?.let { extractAndMergeDefaultQualifiers(null, it.convertAnnotationsToFir(session, JavaTypeParameterStack.EMPTY)) }
            val forPackage = (firClass as? FirJavaClass)?.javaPackage
                ?.let { extractAndMergeDefaultQualifiers(forModule, it.convertAnnotationsToFir(session, JavaTypeParameterStack.EMPTY)) }
            forPackage ?: forModule
        }
        return extractAndMergeDefaultQualifiers(parentQualifiers, firClass.annotations)
    }
}

val FirSession.javaAnnotationTypeQualifierResolver: FirAnnotationTypeQualifierResolver by FirSession.sessionComponentAccessor()
