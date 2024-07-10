/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.services

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.services.LLFirElementByPsiElementChooser
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.createEmptySession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.analysis.utils.printer.parentsOfType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.BodyBuildingMode
import org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirImplicitNullableAnyTypeRef
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addIfNotNull

/**
 * In Standalone mode, deserialized elements don't have sources, so we need to implement [LLFirElementByPsiElementChooser] based on
 * component comparison (see [LLFirElementByPsiElementChooser]).
 *
 * TODO: We might be able to remove this service if KT-65836 is viable (using stub-based deserialized symbol providers in Standalone mode).
 */
class LLStandaloneFirElementByPsiElementChooser : LLFirElementByPsiElementChooser() {
    override fun isMatchingValueParameter(psi: KtParameter, fir: FirValueParameter): Boolean {
        if (fir.realPsi != null) return fir.realPsi === psi

        return fir.name == psi.nameAsSafeName
    }

    override fun isMatchingTypeParameter(psi: KtTypeParameter, fir: FirTypeParameter): Boolean {
        if (fir.realPsi != null) return fir.realPsi === psi

        return fir.name == psi.nameAsSafeName
    }

    override fun isMatchingEnumEntry(psi: KtEnumEntry, fir: FirEnumEntry): Boolean {
        if (fir.realPsi != null) return fir.realPsi === psi

        return fir.name == psi.nameAsName
    }

    // TODO: Use structural type comparison? We can potentially ignore components which don't factor into overload resolution, such as type
    //       annotations, because we only need to pick one FIR callable without a reasonable doubt and ambiguities cannot originate from
    //       libraries.
    override fun isMatchingCallableDeclaration(psi: KtCallableDeclaration, fir: FirCallableDeclaration): Boolean {
        if (fir.realPsi != null) return fir.realPsi === psi

        if (fir is FirConstructor && psi is KtConstructor<*>) {
            if (psi is KtPrimaryConstructor && fir.isPrimary) return true // There can only be one primary constructor.
            if (psi is KtPrimaryConstructor || fir.isPrimary) return false
        }

        if (!modifiersMatch(psi, fir)) return false
        if (!receiverTypeMatches(psi, fir)) return false
        if (!returnTypesMatch(psi, fir)) return false
        if (!typeParametersMatch(psi, fir)) return false
        if (fir is FirFunction && !valueParametersMatch(psi, fir)) return false

        return true
    }

    private fun modifiersMatch(psi: KtCallableDeclaration, fir: FirCallableDeclaration): Boolean {
        // According to asymmetric logic in `PsiRawFirBuilder`.
        if (psi.parentsOfType<KtDeclaration>().any { it.hasExpectModifier() } != fir.symbol.rawStatus.isExpect) return false
        if (psi.hasActualModifier() != fir.symbol.rawStatus.isActual) return false
        return true
    }

    private fun receiverTypeMatches(psi: KtCallableDeclaration, fir: FirCallableDeclaration): Boolean {
        if ((fir.receiverParameter != null) != (psi.receiverTypeReference != null)) return false
        if (fir.receiverParameter != null && !isTheSameTypes(
                psi.receiverTypeReference!!,
                fir.receiverParameter!!.typeRef,
                isVararg = false,
            )
        ) {
            return false
        }
        return true
    }

    private fun returnTypesMatch(psi: KtCallableDeclaration, fir: FirCallableDeclaration): Boolean {
        if (psi is KtConstructor<*>) return true
        return isTheSameTypes(psi.typeReference!!, fir.returnTypeRef, isVararg = false)
    }

    private fun typeParametersMatch(psiFunction: KtCallableDeclaration, firFunction: FirCallableDeclaration): Boolean {
        if (firFunction.typeParameters.size != psiFunction.typeParameters.size) return false
        val boundsByName = psiFunction.typeConstraints.groupBy { it.subjectTypeParameterName?.getReferencedName() }
        firFunction.typeParameters.zip(psiFunction.typeParameters) { expectedTypeParameter, candidateTypeParameter ->
            if (expectedTypeParameter.symbol.name.toString() != candidateTypeParameter.name) return false
            val candidateBounds = mutableListOf<KtTypeReference>()
            candidateBounds.addIfNotNull(candidateTypeParameter.extendsBound)
            boundsByName[candidateTypeParameter.name]?.forEach {
                candidateBounds.addIfNotNull(it.boundTypeReference)
            }
            val expectedBounds = expectedTypeParameter.symbol.resolvedBounds.filter { it !is FirImplicitNullableAnyTypeRef }
            if (candidateBounds.size != expectedBounds.size) return false
            expectedBounds.zip(candidateBounds) { expectedBound, candidateBound ->
                if (!isTheSameTypes(
                        candidateBound,
                        expectedBound,
                        isVararg = false
                    )
                ) {
                    return false
                }
            }
        }
        return true
    }

    private fun valueParametersMatch(psiFunction: KtCallableDeclaration, firFunction: FirFunction): Boolean {
        if (firFunction.valueParameters.size != psiFunction.valueParameters.size) return false
        firFunction.valueParameters.zip(psiFunction.valueParameters) { expectedParameter, candidateParameter ->
            if (expectedParameter.name.toString() != candidateParameter.name) return false
            if (expectedParameter.isVararg != candidateParameter.isVarArg) return false
            val candidateParameterType = candidateParameter.typeReference ?: return false
            if (!isTheSameTypes(
                    candidateParameterType,
                    expectedParameter.returnTypeRef,
                    isVararg = expectedParameter.isVararg
                )
            ) {
                return false
            }
        }
        return true
    }

    private fun FirTypeRef.renderTypeAsKotlinType(isVararg: Boolean = false): String {
        val rendered = when (this) {
            is FirResolvedTypeRef -> type.renderTypeAsKotlinType()
            is FirUserTypeRef -> {
                val renderedQualifier = qualifier.joinToString(separator = ".") { part ->
                    buildString {
                        append(part.name)
                        if (part.typeArgumentList.typeArguments.isNotEmpty()) {
                            part.typeArgumentList.typeArguments.joinTo(this, prefix = "<", postfix = ">") { it.renderTypeAsKotlinType() }
                        }
                    }
                }
                if (isMarkedNullable) "$renderedQualifier?" else renderedQualifier
            }
            is FirFunctionTypeRef -> {
                val classId = if (isSuspend) {
                    StandardNames.getSuspendFunctionClassId(parametersCount)
                } else {
                    StandardNames.getFunctionClassId(parametersCount)
                }
                buildString {
                    append(classId.asSingleFqName().toString())
                    val parameters = buildList {
                        receiverTypeRef?.let(::add)
                        parameters.mapTo(this) { it.returnTypeRef }
                        returnTypeRef.let(::add)
                    }
                    if (parameters.isNotEmpty()) {
                        append(parameters.joinToString(prefix = "<", postfix = ">") { it.renderTypeAsKotlinType() })
                    }
                    if (isMarkedNullable) {
                        append("?")
                    }
                }
            }
            else -> errorWithFirSpecificEntries("Invalid type reference", fir = this)
        }
        return if (isVararg) {
            rendered.asArrayType()
        } else {
            rendered
        }
    }

    private fun String.asArrayType(): String {
        arrayClassIdByElementType[this]?.let { return it }
        return "kotlin.Array<out $this>"
    }

    private val arrayClassIdByElementType: Map<String, String> = buildList<Pair<String, String>> {
        StandardClassIds.primitiveArrayTypeByElementType.mapTo(this) { (classId, arrayClassId) ->
            classId.asString().replace('/', '.') to arrayClassId.asString().replace('/', '.')
        }
        StandardClassIds.unsignedArrayTypeByElementType.mapTo(this) { (classId, arrayClassId) ->
            classId.asString().replace('/', '.') to arrayClassId.asString().replace('/', '.')
        }
    }.toMap()

    private fun FirTypeProjection.renderTypeAsKotlinType() = when (this) {
        is FirStarProjection -> "*"
        is FirTypeProjectionWithVariance -> buildString {
            append(variance.label)
            if (variance != Variance.INVARIANT) {
                append(" ")
            }
            append(typeRef.renderTypeAsKotlinType())
        }
        is FirPlaceholderProjection -> "_"
    }

    private fun isTheSameTypes(
        psiTypeReference: KtTypeReference,
        coneTypeReference: FirTypeRef,
        isVararg: Boolean
    ): Boolean =
        psiTypeReference.toKotlinTypeReference().renderTypeAsKotlinType(isVararg) == coneTypeReference.renderTypeAsKotlinType()

    @Suppress("DEPRECATION_ERROR")
    private fun KtTypeReference.toKotlinTypeReference(): FirTypeRef {
        // Maybe resolve all types here to not to work with FirTypeRef directly
        return PsiRawFirBuilder(
            createEmptySession(),
            DummyScopeProvider,
            bodyBuildingMode = BodyBuildingMode.NORMAL
        ).buildTypeReference(this)
    }

    private fun ConeKotlinType.renderTypeAsKotlinType(): String {
        val rendered = when (this) {
            is ConeClassLikeType -> buildString {
                append(lookupTag.classId.asString())
                if (typeArguments.isNotEmpty()) {
                    append(typeArguments.joinToString(prefix = "<", postfix = ">", separator = ", ") { it.renderTypeAsKotlinType() })
                }
            }
            is ConeTypeVariableType -> typeConstructor.name.asString()
            is ConeLookupTagBasedType -> lookupTag.name.asString()

            // NOTE: Flexible types can occur not only as implicit return types,
            // but also as implicit parameter types, for example in setters with implicit types
            is ConeFlexibleType -> {
                // since Kotlin decompiler always "renders" flexible types as their lower bound, we can do the same here
                lowerBound.renderTypeAsKotlinType()
            }

            else -> errorWithFirSpecificEntries("Type should not be present in Kotlin declaration", coneType = this)
        }.replace('/', '.')

        // UNKNOWN nullability occurs only on flexible types
        val nullabilitySuffix = nullability.takeUnless { it == ConeNullability.UNKNOWN }?.suffix.orEmpty()

        return rendered + nullabilitySuffix
    }

    private fun ConeTypeProjection.renderTypeAsKotlinType(): String = when (this) {
        ConeStarProjection -> "*"
        is ConeKotlinTypeProjectionIn -> "in ${type.renderTypeAsKotlinType()}"
        is ConeKotlinTypeProjectionOut -> "out ${type.renderTypeAsKotlinType()}"
        is ConeKotlinTypeConflictingProjection -> "CONFLICTING-PROJECTION ${type.renderTypeAsKotlinType()}"
        is ConeKotlinType -> renderTypeAsKotlinType()
    }

    private object DummyScopeProvider : FirScopeProvider() {
        override fun getUseSiteMemberScope(
            klass: FirClass,
            useSiteSession: FirSession,
            scopeSession: ScopeSession,
            memberRequiredPhase: FirResolvePhase?,
        ): FirTypeScope = shouldNotBeCalled()

        override fun getStaticCallableMemberScope(
            klass: FirClass,
            useSiteSession: FirSession,
            scopeSession: ScopeSession
        ): FirContainingNamesAwareScope = shouldNotBeCalled()

        override fun getStaticCallableMemberScopeForBackend(
            klass: FirClass,
            useSiteSession: FirSession,
            scopeSession: ScopeSession,
        ): FirContainingNamesAwareScope = shouldNotBeCalled()

        override fun getNestedClassifierScope(
            klass: FirClass,
            useSiteSession: FirSession,
            scopeSession: ScopeSession
        ): FirContainingNamesAwareScope = shouldNotBeCalled()

        private fun shouldNotBeCalled(): Nothing = error("Should not be called in RawFirBuilder while converting KtTypeReference")
    }
}
