/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.createEmptySession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.analysis.utils.printer.parentsOfType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.BodyBuildingMode
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirImplicitNullableAnyTypeRef
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addIfNotNull

// TODO replace with structural type comparison?
object KtDeclarationAndFirDeclarationEqualityChecker {
    fun representsTheSameDeclaration(psi: KtCallableDeclaration, fir: FirCallableDeclaration): Boolean {
        if (!modifiersMatch(psi, fir)) return false
        if (!receiverTypeMatch(psi, fir)) return false
        if (!returnTypesMatch(psi, fir)) return false
        if (!typeParametersMatch(psi, fir)) return false
        if (fir is FirFunction && !valueParametersMatch(psi, fir)) return false
        return true
    }

    private fun modifiersMatch(psi: KtCallableDeclaration, fir: FirCallableDeclaration): Boolean {
        // According to asymmetric logic in 'RawFirBuilder'
        if (psi.parentsOfType<KtDeclaration>().any { it.hasExpectModifier() } != fir.symbol.rawStatus.isExpect) return false
        if (psi.hasActualModifier() != fir.symbol.rawStatus.isActual) return false
        return true
    }

    private fun receiverTypeMatch(psi: KtCallableDeclaration, fir: FirCallableDeclaration): Boolean {
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
        classIdToName[this]?.let { return it }
        return "kotlin.Array<out $this>"
    }

    private val classIdToName: Map<String, String> = buildList<Pair<String, String>> {
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
        else -> errorWithFirSpecificEntries("Invalid type reference", fir = this)
    }

    private fun isTheSameTypes(
        psiTypeReference: KtTypeReference,
        coneTypeReference: FirTypeRef,
        isVararg: Boolean
    ): Boolean =
        psiTypeReference.toKotlinTypReference().renderTypeAsKotlinType(isVararg) == coneTypeReference.renderTypeAsKotlinType()

    @Suppress("DEPRECATION_ERROR")
    private fun KtTypeReference.toKotlinTypReference(): FirTypeRef {
        // Maybe resolve all types here to not to work with FirTypeRef directly
        return RawFirBuilder(
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
            is ConeTypeVariableType -> lookupTag.name.asString()
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

    @TestOnly
    fun renderPsi(ktFunction: KtFunction): String = buildString {
        appendLine("receiver: ${ktFunction.receiverTypeReference?.toKotlinTypReference()?.renderTypeAsKotlinType()}")
        ktFunction.valueParameters.forEach { parameter ->
            appendLine("${parameter.name}: ${parameter.typeReference?.toKotlinTypReference()?.renderTypeAsKotlinType()}")
        }
        appendLine("return: ${ktFunction.typeReference?.toKotlinTypReference()?.renderTypeAsKotlinType()}")
    }

    @TestOnly
    fun renderFir(firFunction: FirFunction): String = buildString {
        appendLine("receiver: ${firFunction.receiverParameter?.typeRef?.renderTypeAsKotlinType()}")
        firFunction.valueParameters.forEach { parameter ->
            appendLine("${parameter.name}: ${parameter.returnTypeRef.renderTypeAsKotlinType()}")
        }
        appendLine("return: ${firFunction.returnTypeRef.renderTypeAsKotlinType()}")
    }

    private object DummyScopeProvider : FirScopeProvider() {
        override fun getUseSiteMemberScope(
            klass: FirClass,
            useSiteSession: FirSession,
            scopeSession: ScopeSession,
            memberRequiredPhase: FirResolvePhase?,
        ): FirTypeScope = shouldNotBeCalled()

        override fun getStaticMemberScopeForCallables(
            klass: FirClass,
            useSiteSession: FirSession,
            scopeSession: ScopeSession
        ): FirContainingNamesAwareScope? = shouldNotBeCalled()

        override fun getNestedClassifierScope(
            klass: FirClass,
            useSiteSession: FirSession,
            scopeSession: ScopeSession
        ): FirContainingNamesAwareScope? = shouldNotBeCalled()

        private fun shouldNotBeCalled(): Nothing = error("Should not be called in RawFirBuilder while converting KtTypeReference")
    }
}
