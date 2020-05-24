/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir

import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.types.Variance

// TODO replace with structural type comparision?
object KtDeclarationAndFirDeclarationEqualityChecker {
    fun representsTheSameDeclaration(psi: KtFunction, fir: FirFunction<*>): Boolean {
        if ((fir.receiverTypeRef != null) != (psi.receiverTypeReference != null)) return false
        if (fir.receiverTypeRef != null
            && !isTheSameTypes(
                psi.receiverTypeReference!!,
                fir.receiverTypeRef!!,
                fir.session,
                isVararg = false
            )
        ) {
            return false
        }
        if (fir.valueParameters.size != psi.valueParameters.size) return false
        fir.valueParameters.zip(psi.valueParameters) { expectedParameter, candidateParameter ->
            if (expectedParameter.name.toString() != candidateParameter.name) return false
            if (expectedParameter.isVararg != candidateParameter.isVarArg) return false
            val candidateParameterType = candidateParameter.typeReference ?: return false
            if (!isTheSameTypes(
                    candidateParameterType,
                    expectedParameter.returnTypeRef,
                    fir.session,
                    isVararg = expectedParameter.isVararg
                )
            ) {
                return false
            }
        }
        return true
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun FirTypeRef.renderTypeAsKotlinType(isVararg: Boolean = false): String {
        val rendered = when (this) {
            is FirResolvedTypeRef -> type.renderTypeAsKotlinType()
            is FirUserTypeRef -> {
                val renderedQualifier = qualifier.joinToString(separator = ".") { part ->
                    buildString {
                        append(part.name)
                        if (part.typeArguments.isNotEmpty()) {
                            part.typeArguments.joinTo(this, prefix = "<", postfix = ">") { it.renderTypeAsKotlinType() }
                        }
                    }
                }
                if (isMarkedNullable) "$renderedQualifier?" else renderedQualifier
            }
            is FirFunctionTypeRef -> {
                val classId = if (isSuspend) {
                    KotlinBuiltIns.getSuspendFunctionClassId(parametersCount)
                } else {
                    KotlinBuiltIns.getFunctionClassId(parametersCount)
                }
                buildString {
                    append(classId.asSingleFqName().toString())
                    val parameters = buildList {
                        receiverTypeRef?.let(::add)
                        valueParameters.mapTo(this) { it.returnTypeRef }
                        returnTypeRef.let(::add)
                    }
                    if (parameters.isNotEmpty()) {
                        append(parameters.joinToString(prefix = "<", postfix = ">") { it.renderTypeAsKotlinType() })
                    }
                }
            }
            else -> error("Invalid type reference $this")
        }
        return if (isVararg) {
            "kotlin.Array<out $rendered>"
        } else {
            rendered
        }
    }

    private fun FirTypeProjection.renderTypeAsKotlinType() = when (this) {
        is FirStarProjection -> "*"
        is FirTypeProjectionWithVariance -> buildString {
            append(variance.label)
            if (variance != Variance.INVARIANT) {
                append(" ")
            }
            append(typeRef.renderTypeAsKotlinType())
        }
        else -> error("Invalid type projection $this")
    }

    private fun isTheSameTypes(
        psiTypeReference: KtTypeReference,
        coneTypeReference: FirTypeRef,
        session: FirSession,
        isVararg: Boolean
    ): Boolean =
        psiTypeReference.toKotlinTypReference(session).renderTypeAsKotlinType(isVararg) == coneTypeReference.renderTypeAsKotlinType()

    private fun KtTypeReference.toKotlinTypReference(session: FirSession): FirTypeRef {
        // Maybe resolve all types here to not to work with FirTypeRef directly
        return RawFirBuilder(session, DummyScopeProvider, stubMode = true).buildTypeReference(this)
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
            is ConeFlexibleType -> {
                // Can be present as return type
                "${lowerBound.renderTypeAsKotlinType()}..${upperBound.renderTypeAsKotlinType()}"
            }
            else -> error("Type $this should not be present in Kotlin declaration")
        }.replace('/', '.')
        return rendered + nullability.suffix
    }

    private fun ConeTypeProjection.renderTypeAsKotlinType(): String = when (this) {
        ConeStarProjection -> "*"
        is ConeKotlinTypeProjectionIn -> "in ${type.renderTypeAsKotlinType()}"
        is ConeKotlinTypeProjectionOut -> "out ${type.renderTypeAsKotlinType()}"
        is ConeKotlinType -> renderTypeAsKotlinType()
    }

    @TestOnly
    internal fun renderPsi(ktFunction: KtFunction, session: FirSession): String = buildString {
        appendLine("receiver: ${ktFunction.receiverTypeReference?.toKotlinTypReference(session)?.renderTypeAsKotlinType()}")
        ktFunction.valueParameters.forEach { parameter ->
            appendLine("${parameter.name}: ${parameter.typeReference?.toKotlinTypReference(session)?.renderTypeAsKotlinType()}")
        }
        appendLine("return: ${ktFunction.typeReference?.toKotlinTypReference(session)?.renderTypeAsKotlinType()}")
    }

    @TestOnly
    internal fun renderFir(firFunction: FirFunction<*>): String = buildString {
        appendLine("receiver: ${firFunction.receiverTypeRef?.renderTypeAsKotlinType()}")
        firFunction.valueParameters.forEach { parameter ->
            appendLine("${parameter.name}: ${parameter.returnTypeRef.renderTypeAsKotlinType()}")
        }
        appendLine("return: ${firFunction.returnTypeRef.renderTypeAsKotlinType()}")
    }

    private object DummyScopeProvider : FirScopeProvider() {
        override fun getUseSiteMemberScope(klass: FirClass<*>, useSiteSession: FirSession, scopeSession: ScopeSession): FirScope {
            shouldNotBeCalled()
        }

        override fun getStaticMemberScopeForCallables(
            klass: FirClass<*>,
            useSiteSession: FirSession,
            scopeSession: ScopeSession
        ): FirScope? {
            shouldNotBeCalled()
        }

        override fun getNestedClassifierScope(klass: FirClass<*>, useSiteSession: FirSession, scopeSession: ScopeSession): FirScope? {
            shouldNotBeCalled()
        }

        private fun shouldNotBeCalled(): Nothing =
            error("Should not be called in RawFirBuilder while converting KtTypeReference")
    }
}