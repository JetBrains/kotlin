/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.web.common.checkers.declaration

import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT

abstract class FirWebCommonExternalChecker(
    private val allowCompanionInInterface: Boolean
) : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    abstract fun isNativeOrEffectivelyExternal(symbol: FirBasedSymbol<*>, session: FirSession): Boolean

    context(context: CheckerContext, reporter: DiagnosticReporter)
    abstract fun reportExternalEnum(declaration: FirDeclaration)

    context(context: CheckerContext, reporter: DiagnosticReporter)
    abstract fun additionalCheck(declaration: FirDeclaration)

    abstract fun isDefinedExternallyCallableId(callableId: CallableId?): Boolean

    abstract fun hasExternalLikeAnnotations(declaration: FirDeclaration, session: FirSession): Boolean

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        if (!isNativeOrEffectivelyExternal(declaration.symbol, context.session)) return

        if (!context.isTopLevel) {
            if (declaration !is FirPropertyAccessor && declaration.isDirectlyExternal(context.session)) {
                reporter.reportOn(declaration.source, FirWebCommonErrors.NESTED_EXTERNAL_DECLARATION)
            }
        }

        if (declaration is FirClass) {
            // TODO: KT-55600: Stop generating diagnostic
            //  messages inside checkers
            val classKind = when {
                declaration.status.isData -> "data class"
                declaration.status.isInner -> "inner class"
                declaration.status.isInline || declaration.status.isValue -> "value class"
                declaration.status.isFun -> "fun interface"
                declaration.classKind == ClassKind.ANNOTATION_CLASS -> "annotation class"
                else -> null
            }

            if (classKind != null) {
                reporter.reportOn(declaration.source, FirWebCommonErrors.WRONG_EXTERNAL_DECLARATION, classKind)
            }

            if (declaration.isEnumClass) {
                reportExternalEnum(declaration)
            }
        }

        if (declaration !is FirConstructor &&
            declaration !is FirField &&
            declaration.isPrivateMemberOfExternalClass(context.session)
        ) {
            reporter.reportOn(declaration.source, FirWebCommonErrors.WRONG_EXTERNAL_DECLARATION, "private member of class")
        }

        val container = context.containingDeclarations.lastOrNull()

        if (
            declaration is FirClass &&
            !declaration.classKind.isInterface && (!allowCompanionInInterface || !declaration.status.isCompanion) &&
            container is FirClassSymbol<*> && container.classKind.isInterface
        ) {
            reporter.reportOn(declaration.source, FirWebCommonErrors.NESTED_CLASS_IN_EXTERNAL_INTERFACE)
        }

        if (
            allowCompanionInInterface &&
            declaration is FirClass &&
            declaration.status.isCompanion &&
            container is FirClassSymbol<*> &&
            container.isInterface &&
            declaration.nameOrSpecialName != DEFAULT_NAME_FOR_COMPANION_OBJECT
        ) {
            reporter.reportOn(declaration.source, FirWebCommonErrors.NAMED_COMPANION_IN_EXTERNAL_INTERFACE)
        }


        if (declaration !is FirPropertyAccessor && declaration is FirCallableDeclaration && declaration.isExtension) {
            val target = when (declaration) {
                is FirFunction -> "extension function"
                is FirProperty -> "extension property"
                else -> "extension member"
            }
            reporter.reportOn(declaration.source, FirWebCommonErrors.WRONG_EXTERNAL_DECLARATION, target)
        }

        if (
            declaration is FirCallableDeclaration &&
            declaration.isNonAbstractMemberIfInterface() &&
            !declaration.isNullableProperty()
        ) {
            reporter.reportOn(declaration.source, FirWebCommonErrors.NON_ABSTRACT_MEMBER_OF_EXTERNAL_INTERFACE)
        }

        declaration.checkBody()
        declaration.checkDelegation()
        declaration.checkAnonymousInitializer()

        if (!LanguageFeature.JsExternalPropertyParameters.isEnabled()) {
            declaration.checkConstructorPropertyParam()
        }

        additionalCheck(declaration)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun FirDeclaration.checkBody() {
        if (this is FirDefaultPropertyAccessor) return

        val body = when (this) {
            is FirFunction -> body
            is FirAnonymousInitializer -> body
            else -> null
        }

        val initializer = when {
            this is FirEnumEntry -> null
            source?.kind == KtFakeSourceElementKind.PropertyFromParameter -> null
            this is FirVariable -> initializer
            body is FirSingleExpressionBlock -> (body.statement as? FirReturnExpression)?.result
            else -> null
        }

        // we shouldn't check such things as the
        // copy() function of a data class
        if (source?.kind !is KtRealSourceElementKind) {
            return
        }

        val isWrong = body !is FirSingleExpressionBlock && !hasValidExternalBody()
                || initializer != null && !initializer.isDefinedExternallyExpression()

        if (isWrong && body != null) {
            reporter.reportOn(body.source, FirWebCommonErrors.WRONG_BODY_OF_EXTERNAL_DECLARATION)
        } else if (isWrong && initializer != null) {
            reporter.reportOn(initializer.source, FirWebCommonErrors.WRONG_INITIALIZER_OF_EXTERNAL_DECLARATION)
        }

        if (this is FirFunction) {
            for (defaultValue in valueParameters.mapNotNull { it.defaultValue }) {
                if (!defaultValue.isDefinedExternallyExpression()) {
                    reporter.reportOn(defaultValue.source, FirWebCommonErrors.WRONG_DEFAULT_VALUE_FOR_EXTERNAL_FUN_PARAMETER)
                }
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun FirDeclaration.checkDelegation() {
        if (this !is FirMemberDeclaration || !isNativeOrEffectivelyExternal(symbol, context.session)) return

        if (this is FirClass) {
            primaryConstructorIfAny(context.session)?.let {
                val constructorCall = it.resolvedDelegatedConstructorCall

                if (constructorCall?.source?.kind is KtRealSourceElementKind) {
                    reporter.reportOn(constructorCall.source, FirWebCommonErrors.EXTERNAL_DELEGATED_CONSTRUCTOR_CALL)
                }
            }

            for ((superType, delegate) in collectSupertypesWithDelegates()) {
                when {
                    delegate != null -> {
                        reporter.reportOn(superType.source, FirWebCommonErrors.EXTERNAL_DELEGATION)
                    }
                }
            }
        } else if (this is FirConstructor && !isPrimary) {
            val delegationCall = delegatedConstructor

            if (delegationCall?.source?.kind is KtRealSourceElementKind) {
                reporter.reportOn(delegationCall.source, FirWebCommonErrors.EXTERNAL_DELEGATED_CONSTRUCTOR_CALL)
            }
        } else if (this is FirProperty) {
            delegate?.let {
                reporter.reportOn(it.source, FirWebCommonErrors.EXTERNAL_DELEGATION)
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun FirDeclaration.checkAnonymousInitializer() {
        if (this !is FirClass) return

        for (anonymousInitializer in anonymousInitializers) {
            reporter.reportOn(anonymousInitializer.source, FirWebCommonErrors.EXTERNAL_ANONYMOUS_INITIALIZER)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun FirDeclaration.checkConstructorPropertyParam() {
        if (this !is FirProperty || source?.kind != KtFakeSourceElementKind.PropertyFromParameter) return
        val containingClass = getContainingClassSymbol() as? FirClassSymbol<*> ?: return
        if (containingClass.isData || containingClass.classKind == ClassKind.ANNOTATION_CLASS) return
        reporter.reportOn(source, FirWebCommonErrors.EXTERNAL_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER)
    }

    private fun FirDeclaration.isDirectlyExternal(session: FirSession): Boolean {
        // source kind is checked, otherwise this function
        // may return true for a primary constructor of an external class
        if (this is FirDefaultPropertyAccessor || this.source?.kind !is KtRealSourceElementKind) return false
        return hasModifier(KtTokens.EXTERNAL_KEYWORD) || hasExternalLikeAnnotations(this, session)
    }

    private fun FirDeclaration.isPrivateMemberOfExternalClass(session: FirSession): Boolean {
        if (this is FirBackingField) return false
        if (this is FirPropertyAccessor && visibility == propertySymbol.visibility) return false
        if (this !is FirMemberDeclaration || visibility != Visibilities.Private) return false

        val containingDeclaration = getContainingClassSymbol() ?: return false
        return isNativeOrEffectivelyExternal(containingDeclaration, session)
    }

    private fun FirDeclaration.isNonAbstractMemberIfInterface(): Boolean {
        if (this is FirBackingField) return false
        return this is FirCallableDeclaration
                && modality != Modality.ABSTRACT
                && (getContainingClassSymbol() as? FirClassSymbol<*>)?.classKind == ClassKind.INTERFACE
                && this !is FirPropertyAccessor
    }

    private fun FirCallableDeclaration.isNullableProperty() = this is FirProperty && returnTypeRef.coneType.isMarkedOrFlexiblyNullable

    private fun FirDeclaration.hasValidExternalBody(): Boolean {
        val body = when (this) {
            is FirFunction -> body
            is FirAnonymousInitializer -> body
            else -> return true
        }

        return when {
            body is FirSingleExpressionBlock -> body.isDefinedExternallyExpression()
            body != null -> {
                val statement = body.statements.singleOrNull() ?: return false
                statement.isDefinedExternallyExpression()
            }

            else -> false
        }
    }

    private fun FirElement.isDefinedExternallyExpression(): Boolean {
        val declaration = (this as? FirPropertyAccessExpression)
            ?.calleeReference?.toResolvedPropertySymbol() ?: return false
        return isDefinedExternallyCallableId(declaration.callableId)
    }
}
