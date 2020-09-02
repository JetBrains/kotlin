/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.enhancement

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.load.java.*
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.DEFAULT_ANNOTATION_MEMBER_NAME
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.Jsr305State
import org.jetbrains.kotlin.utils.ReportLevel
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

class FirAnnotationTypeQualifierResolver(private val session: FirSession, private val jsr305State: Jsr305State) {

    class TypeQualifierWithApplicability(
        private val typeQualifier: FirAnnotationCall,
        private val applicability: Int
    ) {
        operator fun component1() = typeQualifier
        operator fun component2() = AnnotationQualifierApplicabilityType.values().filter(this::isApplicableTo)

        private fun isApplicableTo(elementType: AnnotationQualifierApplicabilityType) =
            isApplicableConsideringMask(AnnotationQualifierApplicabilityType.TYPE_USE) || isApplicableConsideringMask(
                elementType
            )

        private fun isApplicableConsideringMask(elementType: AnnotationQualifierApplicabilityType) =
            (applicability and (1 shl elementType.ordinal)) != 0
    }

    // TODO: memoize this function
    private fun computeTypeQualifierNickname(klass: FirRegularClass): FirAnnotationCall? {
        if (klass.annotations.none { it.classId == TYPE_QUALIFIER_NICKNAME_ID }) return null

        return klass.annotations.firstNotNullResult(this::resolveTypeQualifierAnnotation)
    }

    private fun resolveTypeQualifierNickname(klass: FirRegularClass): FirAnnotationCall? {
        if (klass.classKind != ClassKind.ANNOTATION_CLASS) return null

        return computeTypeQualifierNickname(klass)
    }

    private val FirAnnotationCall.resolvedClass: FirRegularClass?
        get() = (coneClassLikeType?.lookupTag?.toSymbol(this@FirAnnotationTypeQualifierResolver.session) as? FirRegularClassSymbol)?.fir

    fun resolveTypeQualifierAnnotation(annotationCall: FirAnnotationCall): FirAnnotationCall? {
        if (jsr305State.disabled) {
            return null
        }

        val annotationClass = annotationCall.resolvedClass ?: return null
        if (annotationClass.isAnnotatedWithTypeQualifier) return annotationCall

        return resolveTypeQualifierNickname(annotationClass)
    }

    fun resolveQualifierBuiltInDefaultAnnotation(annotationCall: FirAnnotationCall): NullabilityQualifierWithApplicability? {
        if (jsr305State.disabled) {
            return null
        }

        val annotationClassId = annotationCall.classId
        return BUILT_IN_TYPE_QUALIFIER_DEFAULT_ANNOTATION_IDS[annotationClassId]?.let { (qualifier, applicability) ->
            val state = resolveJsr305ReportLevel(annotationCall).takeIf { it != ReportLevel.IGNORE } ?: return null
            return NullabilityQualifierWithApplicability(qualifier.copy(isForWarningOnly = state.isWarning), applicability)
        }
    }

    fun resolveTypeQualifierDefaultAnnotation(annotationCall: FirAnnotationCall): TypeQualifierWithApplicability? {
        if (jsr305State.disabled) {
            return null
        }

        val typeQualifierDefaultAnnotatedClass =
            annotationCall.resolvedClass?.takeIf { klass ->
                klass.annotations.any { it.classId == TYPE_QUALIFIER_DEFAULT_ID }
            } ?: return null

        val elementTypesMask =
            annotationCall.resolvedClass!!
                .annotations.find { it.classId == TYPE_QUALIFIER_DEFAULT_ID }!!
                .arguments
                .flatMap { argument ->
                    if (argument !is FirNamedArgumentExpression || argument.name == DEFAULT_ANNOTATION_MEMBER_NAME)
                        argument.mapConstantToQualifierApplicabilityTypes()
                    else
                        emptyList()
                }
                .fold(0) { acc: Int, applicabilityType -> acc or (1 shl applicabilityType.ordinal) }

        val typeQualifier = typeQualifierDefaultAnnotatedClass.annotations.firstOrNull { resolveTypeQualifierAnnotation(it) != null }
            ?: return null

        return TypeQualifierWithApplicability(
            typeQualifier,
            elementTypesMask
        )
    }

    fun resolveJsr305ReportLevel(annotationCall: FirAnnotationCall): ReportLevel {
        resolveJsr305CustomLevel(annotationCall)?.let { return it }
        return jsr305State.global
    }

    fun resolveJsr305CustomLevel(annotationCall: FirAnnotationCall): ReportLevel? {
        jsr305State.user[annotationCall.classId?.run { packageFqName.asString() + "." + relativeClassName.asString() }]?.let { return it }
        return annotationCall.resolvedClass?.migrationAnnotationStatus()
    }

    private fun FirRegularClass.migrationAnnotationStatus(): ReportLevel? {
        val enumEntryName = annotations.find {
            it.classId == MIGRATION_ANNOTATION_ID
        }?.arguments?.firstOrNull()?.toResolvedCallableSymbol()?.callableId?.callableName ?: return null

        jsr305State.migration?.let { return it }

        return when (enumEntryName.asString()) {
            "STRICT" -> ReportLevel.STRICT
            "WARN" -> ReportLevel.WARN
            "IGNORE" -> ReportLevel.IGNORE
            else -> null
        }
    }

    private fun FirExpression.mapConstantToQualifierApplicabilityTypes(): List<AnnotationQualifierApplicabilityType> =
        when (this) {
            is FirArrayOfCall -> arguments.flatMap { it.mapConstantToQualifierApplicabilityTypes() }
            else -> listOfNotNull(
                when (toResolvedCallableSymbol()?.callableId?.callableName?.asString()) {
                    "METHOD" -> AnnotationQualifierApplicabilityType.METHOD_RETURN_TYPE
                    "FIELD" -> AnnotationQualifierApplicabilityType.FIELD
                    "PARAMETER" -> AnnotationQualifierApplicabilityType.VALUE_PARAMETER
                    "TYPE_USE" -> AnnotationQualifierApplicabilityType.TYPE_USE
                    else -> null
                }
            )
        }

    val disabled: Boolean = jsr305State.disabled

}

private val FirRegularClass.isAnnotatedWithTypeQualifier: Boolean
    get() = this.symbol.classId in BUILT_IN_TYPE_QUALIFIER_IDS ||
            annotations.any { it.classId == TYPE_QUALIFIER_ID }

private val TYPE_QUALIFIER_ID = ClassId.topLevel(TYPE_QUALIFIER_FQNAME)
private val BUILT_IN_TYPE_QUALIFIER_IDS = BUILT_IN_TYPE_QUALIFIER_FQ_NAMES.map { ClassId.topLevel(it) }
private val TYPE_QUALIFIER_DEFAULT_ID = ClassId.topLevel(TYPE_QUALIFIER_DEFAULT_FQNAME)
private val MIGRATION_ANNOTATION_ID = ClassId.topLevel(MIGRATION_ANNOTATION_FQNAME)
private val TYPE_QUALIFIER_NICKNAME_ID = ClassId.topLevel(TYPE_QUALIFIER_NICKNAME_FQNAME)

private val BUILT_IN_TYPE_QUALIFIER_DEFAULT_ANNOTATION_IDS =
    BUILT_IN_TYPE_QUALIFIER_DEFAULT_ANNOTATIONS.mapKeys { ClassId.topLevel(it.key) }
