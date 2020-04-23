/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClass
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticFactory3
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.declaredMemberScopeProvider
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.stubs.elements.KtPlaceHolderStubElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes.SUPER_TYPE_ENTRY
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes.SUPER_TYPE_LIST

object FirExposedVisibilityChecker : FirDeclarationChecker<FirMemberDeclaration>() {
    override fun check(declaration: FirMemberDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        when (declaration) {
            is FirTypeAlias -> checkTypeAlias(declaration, context, reporter)
            is FirProperty -> checkProperty(declaration, context, reporter)
            is FirFunction<*> -> checkFunction(declaration, context, reporter)
            is FirRegularClass -> checkSupertypes(declaration, context, reporter)
        }
    }

    private fun checkSupertypes(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val classVisibility = declaration.firEffectiveVisibility(declaration.session)
        val supertypes = declaration.superConeTypes
        val supertypesSources = declaration.getSupertypeSources()
        val isInterface = declaration.classKind == ClassKind.INTERFACE
        for (i in supertypes.indices) {
            val supertype = supertypes[i]
            val clazz = supertype.toRegularClass(declaration.session) ?: continue
            val superIsInterface = clazz.classKind == ClassKind.INTERFACE
            if (superIsInterface != isInterface) {
                continue
            }
            val restricting = supertype.leastPermissiveDescriptor(declaration.session, classVisibility)
            if (restricting != null) {
                reporter.reportExposure(
                    if (isInterface) FirErrors.EXPOSED_SUPER_INTERFACE else FirErrors.EXPOSED_SUPER_CLASS,
                    restricting,
                    classVisibility,
                    restricting.firEffectiveVisibility(declaration.session),
                    supertypesSources?.get(i) ?: declaration.source
                )
            }
        }
    }

    private fun checkTypeAlias(declaration: FirTypeAlias, context: CheckerContext, reporter: DiagnosticReporter) {
        val expandedType = declaration.expandedConeType
        val typeAliasVisibility = declaration.firEffectiveVisibility(declaration.session)
        val restricting = expandedType?.leastPermissiveDescriptor(declaration.session, typeAliasVisibility)
        if (restricting != null) {
            reporter.reportExposure(
                FirErrors.EXPOSED_TYPEALIAS_EXPANDED_TYPE,
                restricting,
                typeAliasVisibility,
                restricting.firEffectiveVisibility(declaration.session),
                declaration.source?.getIdentifierSource() ?: declaration.source
            )
        }
    }

    private fun checkFunction(declaration: FirFunction<*>, context: CheckerContext, reporter: DiagnosticReporter) {
        val functionVisibility = (declaration as FirMemberDeclaration).firEffectiveVisibility(declaration.session)
        if (declaration !is FirConstructor) {
            val restricting = declaration.returnTypeRef.coneTypeSafe<ConeKotlinType>()
                ?.leastPermissiveDescriptor(declaration.session, functionVisibility)
            if (restricting != null) {
                reporter.reportExposure(
                    FirErrors.EXPOSED_FUNCTION_RETURN_TYPE,
                    restricting,
                    functionVisibility,
                    restricting.firEffectiveVisibility(declaration.session),
                    declaration.source?.getIdentifierSource() ?: declaration.source
                )
            }
        }
        declaration.valueParameters.forEachIndexed { i, valueParameter ->
            if (i < declaration.valueParameters.size) {
                val restricting =
                    valueParameter.returnTypeRef.coneTypeSafe<ConeKotlinType>()
                        ?.leastPermissiveDescriptor(declaration.session, functionVisibility)
                if (restricting != null) {
                    reporter.reportExposure(
                        FirErrors.EXPOSED_PARAMETER_TYPE,
                        restricting,
                        functionVisibility,
                        restricting.firEffectiveVisibility(declaration.session),
                        valueParameter.source
                    )
                }
            }
        }
        checkMemberReceiver(declaration.receiverTypeRef, declaration as? FirCallableMemberDeclaration<*>, reporter)
    }

    private fun checkProperty(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val propertyVisibility = declaration.firEffectiveVisibility(declaration.session)
        val restricting =
            declaration.returnTypeRef.coneTypeSafe<ConeKotlinType>()?.leastPermissiveDescriptor(declaration.session, propertyVisibility)
        if (restricting != null) {
            reporter.reportExposure(
                FirErrors.EXPOSED_PROPERTY_TYPE,
                restricting,
                propertyVisibility,
                restricting.firEffectiveVisibility(declaration.session),
                declaration.source?.getIdentifierSource() ?: declaration.source
            )
        }
        checkMemberReceiver(declaration.receiverTypeRef, declaration, reporter)
    }

    private fun checkMemberReceiver(
        typeRef: FirTypeRef?,
        memberDeclaration: FirCallableMemberDeclaration<*>?, reporter: DiagnosticReporter
    ) {
        if (typeRef == null || memberDeclaration == null) return
        val receiverParameterType = typeRef.coneTypeSafe<ConeKotlinType>()
        val memberVisibility = memberDeclaration.firEffectiveVisibility(memberDeclaration.session)
        val restricting = receiverParameterType?.leastPermissiveDescriptor(memberDeclaration.session, memberVisibility)
        if (restricting != null) {
            reporter.reportExposure(
                FirErrors.EXPOSED_RECEIVER_TYPE,
                restricting,
                memberVisibility,
                restricting.firEffectiveVisibility(memberDeclaration.session),
                typeRef.source
            )
        }
    }

    private inline fun <reified E : FirSourceElement, P : PsiElement> DiagnosticReporter.reportExposure(
        error: FirDiagnosticFactory3<E, P, FirEffectiveVisibility, DeclarationWithRelation, FirEffectiveVisibility>,
        restrictingDeclaration: DeclarationWithRelation,
        elementVisibility: FirEffectiveVisibility,
        restrictingVisibility: FirEffectiveVisibility,
        source: FirSourceElement?
    ) {
        source?.let {
            report(
                error.on(
                    it as E,
                    elementVisibility,
                    restrictingDeclaration,
                    restrictingVisibility
                )
            )
        }
    }

    private fun FirSourceElement.getIdentifierSource() = when (this) {
        is FirPsiSourceElement<*> -> (this.psi as? PsiNameIdentifierOwner)?.nameIdentifier?.toFirPsiSourceElement()
        is FirLightSourceElement -> {
            val kidsRef = Ref<Array<LighterASTNode?>>()
            this.tree.getChildren(this.element, kidsRef)
            val identifier = kidsRef.get().find { it?.tokenType == KtTokens.IDENTIFIER }
            identifier?.toFirLightSourceElement(this.tree.getStartOffset(identifier), this.tree.getEndOffset(identifier), this.tree)
        }
    }

    private fun FirRegularClass.getSupertypeSources(): List<FirSourceElement>? {
        when (val source = this.source) {
            is FirPsiSourceElement<*> -> {
                return (this.psi as? StubBasedPsiElementBase<*>)?.getStubOrPsiChild(SUPER_TYPE_LIST)?.entries.orEmpty()
                    .map { it.toFirPsiSourceElement() }
            }
            is FirLightSourceElement -> {
                val kidsRef = Ref<Array<LighterASTNode?>>()
                source.tree.getChildren(source.element, kidsRef)
                val supertypes = kidsRef.get().find { it?.tokenType == SUPER_TYPE_LIST } ?: return null
                source.tree.getChildren(supertypes, kidsRef)
                return kidsRef.get().filter { it?.tokenType == SUPER_TYPE_ENTRY }
                    .map { it!!.toFirLightSourceElement(source.tree.getStartOffset(it), source.tree.getEndOffset(it), source.tree) }
            }
        }
        return null
    }
}