/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isSupertypeOf
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClass
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.calls.isPotentiallyArray
import org.jetbrains.kotlin.fir.resolve.toFirRegularClass
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId

object FirInlineClassDeclarationChecker : FirRegularClassChecker() {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.isInlineOrValueClass())
            return

        if (context.containingDeclarations.size > 1) {
            reporter.reportOn(declaration.source, FirErrors.INLINE_CLASS_NOT_TOP_LEVEL, context)
        }

        if (declaration.modality != Modality.FINAL) {
            reporter.reportOn(declaration.source, FirErrors.INLINE_CLASS_NOT_FINAL, context)
        }

        for (supertypeEntry in declaration.superTypeRefs) {
            if (supertypeEntry.toRegularClass(context.session)?.isInterface != true) {
                reporter.reportOn(supertypeEntry.source, FirErrors.INLINE_CLASS_CANNOT_EXTEND_CLASSES, context)
            } else if (supertypeEntry.implementedByDelegation()) {
                reporter.reportOn(supertypeEntry.source, FirErrors.INLINE_CLASS_CANNOT_IMPLEMENT_INTERFACE_BY_DELEGATION, context)
            }
        }

        if (declaration.isSubtypeOfCloneable(context)) {
            reporter.reportOn(declaration.source, FirErrors.VALUE_CLASS_CANNOT_BE_CLONEABLE, context)
        }

        declaration.declarations.filter { it.isSecondaryConstructorWithBody() }.forEach {
            reporter.reportOn((it as FirConstructor).body!!.source, FirErrors.SECONDARY_CONSTRUCTOR_WITH_BODY_INSIDE_INLINE_CLASS, context)
        }

        val primaryConstructor = declaration.getRealPrimaryConstructor() ?: run {
            reporter.reportOn(declaration.source, FirErrors.ABSENCE_OF_PRIMARY_CONSTRUCTOR_FOR_INLINE_CLASS, context)
            return
        }

        val parameter = primaryConstructor.valueParameters.singleOrNull() ?: run {
            reporter.reportOn(primaryConstructor.source, FirErrors.INLINE_CLASS_CONSTRUCTOR_WRONG_PARAMETERS_SIZE, context)
            return
        }

        when {
            parameter.isNotFinalReadOnly(declaration) ->
                reporter.reportOn(parameter.source, FirErrors.INLINE_CLASS_CONSTRUCTOR_NOT_FINAL_READ_ONLY_PARAMETER, context)

            parameter.returnTypeRef.isInapplicableParameterType() ->
                reporter.reportOn(
                    parameter.returnTypeRef.source,
                    FirErrors.INLINE_CLASS_HAS_INAPPLICABLE_PARAMETER_TYPE,
                    parameter.returnTypeRef.coneType,
                    context
                )

            parameter.returnTypeRef.coneType.isRecursiveInlineClassType(context.session) ->
                reporter.reportOn(parameter.returnTypeRef.source, FirErrors.INLINE_CLASS_CANNOT_BE_RECURSIVE, context)
        }
    }

    private fun FirDeclaration.isSecondaryConstructorWithBody() =
        this is FirConstructor && !isPrimary && body != null

    private fun FirValueParameter.isNotFinalReadOnly(declaration: FirRegularClass): Boolean {
        val associatedProperty = declaration.getFirstPrimaryConstructorProperty() ?: return true

        val modifierList = with(FirModifierList) { source.getModifierList() }
        val isOpen = modifierList?.modifiers?.any { it.token == KtTokens.OPEN_KEYWORD } == true

        return isVararg || !associatedProperty.isVal || isOpen
    }

    private fun FirTypeRef.isInapplicableParameterType() =
        isUnit || isNothing || coneType is ConeTypeParameterType || coneType.isGenericArrayOfTypeParameter()

    private fun ConeKotlinType.isGenericArrayOfTypeParameter(): Boolean {
        if (this.typeArguments.firstOrNull() is ConeStarProjection || !isPotentiallyArray())
            return false

        val arrayElementType = arrayElementType()?.type ?: return false
        return arrayElementType is ConeTypeParameterType ||
                arrayElementType.isGenericArrayOfTypeParameter()
    }

    private fun ConeKotlinType.isRecursiveInlineClassType(session: FirSession) =
        isRecursiveInlineClassType(hashSetOf(), session)

    private fun ConeKotlinType.isRecursiveInlineClassType(visited: HashSet<ConeKotlinType>, session: FirSession): Boolean {
        if (!visited.add(this)) return true

        val asRegularClass = this.toRegularClass(session) ?: return false

        return asRegularClass.isInlineOrValueClass() &&
                asRegularClass.getRealPrimaryConstructor()
                    ?.valueParameters
                    ?.firstOrNull()
                    ?.returnTypeRef
                    ?.coneType
                    ?.isRecursiveInlineClassType(visited, session) == true
    }

    private fun FirTypeRef.implementedByDelegation() =
        source?.let {
            it.treeStructure.getParent(it.lighterASTNode)?.tokenType == KtNodeTypes.DELEGATED_SUPER_TYPE_ENTRY
        } ?: false

    private fun FirRegularClass.isSubtypeOfCloneable(context: CheckerContext): Boolean {
        val kotlinCloneable = ConeClassLikeLookupTagImpl(ClassId.fromString("kotlin/Cloneable"))
            .toFirRegularClass(context.session)
        val javaCloneable = ConeClassLikeLookupTagImpl(ClassId.fromString("java/lang/Cloneable"))
            .toFirRegularClass(context.session)

        return kotlinCloneable?.isSupertypeOf(this) == true || javaCloneable?.isSupertypeOf(this) == true
    }
}