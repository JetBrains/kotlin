/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator

import org.jetbrains.kotlin.fir.tree.generator.context.AbstractBuilderConfigurator
import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.fir.tree.generator.model.Field
import org.jetbrains.kotlin.fir.tree.generator.model.Implementation
import org.jetbrains.kotlin.fir.tree.generator.model.LeafBuilder
import org.jetbrains.kotlin.fir.tree.generator.printer.invisibleField
import org.jetbrains.kotlin.fir.tree.generator.util.traverseParents

object BuilderConfigurator : AbstractBuilderConfigurator<FirTreeBuilder>(FirTreeBuilder) {
    fun configureBuilders() = with(firTreeBuilder) {
        val declarationBuilder by builder {
            fields from declaration without "symbol"
        }

        val annotationContainerBuilder by builder {
            fields from annotationContainer
        }

        val expressionBuilder by builder {
            fields from expression
        }

        val typeParametersOwnerBuilder by builder {
            fields from typeParametersOwner
        }

        val typeParameterRefsOwnerBuilder by builder {
            fields from typeParameterRefsOwner
        }

        val classBuilder by builder {
            parents += declarationBuilder
            parents += annotationContainerBuilder
            fields from klass without listOf("symbol", "resolvePhase", "resolveState", "controlFlowGraphReference")
        }

        builder(file) {
            defaultNull("annotationsContainer")
        }

        builder(regularClass) {
            parents += classBuilder
            parents += typeParameterRefsOwnerBuilder
            defaultNull("companionObjectSymbol")
            openBuilder()
            withCopy()
        }

        val qualifiedAccessExpressionBuilder by builder {
            fields from qualifiedAccessExpression without "calleeReference"
        }

        val callBuilder by builder {
            fields from call
        }

        val loopBuilder by builder {
            fields from loop
        }

        val functionBuilder by builder {
            parents += declarationBuilder
            parents += annotationContainerBuilder
            fields from function without listOf(
                "symbol",
                "resolvePhase",
                "resolveState",
                "controlFlowGraphReference",
                "receiverParameter",
                "typeParameters",
            )
        }

        val loopJumpBuilder by builder {
            fields from loopJump without "typeRef"
        }

        val abstractConstructorBuilder by builder {
            parents += functionBuilder
            fields from constructor without listOf("isPrimary")
        }

        val abstractFunctionCallBuilder by builder {
            parents += qualifiedAccessExpressionBuilder
            parents += callBuilder
            fields from functionCall
        }

        for (constructorType in listOf("FirPrimaryConstructor", "FirConstructorImpl")) {
            builder(constructor, constructorType) {
                parents += abstractConstructorBuilder
                defaultNull("delegatedConstructor")
                defaultNull("body")
                default("contractDescription", "FirEmptyContractDescription")
                useTypes(emptyContractDescriptionType)
            }
        }

        builder(errorPrimaryConstructor) {
            parents += abstractConstructorBuilder
            defaultNull("delegatedConstructor")
            defaultNull("body")
            default("contractDescription", "FirEmptyContractDescription")
            useTypes(emptyContractDescriptionType)
        }

        builder(constructor, "FirConstructorImpl") {
            openBuilder()
            withCopy()
        }

        builder(typeParameter) {
            withCopy()
        }

        builder(anonymousObject) {
            parents += declarationBuilder
            parents += classBuilder
        }

        builder(typeAlias) {
            parents += declarationBuilder
            parents += typeParametersOwnerBuilder
            withCopy()
        }

        builder(receiverParameter) {
            withCopy()
        }

        builder(annotation) {
            withCopy()
        }

        builder(annotationCall) {
            parents += callBuilder
            default("argumentList") {
                value = "FirEmptyArgumentList"
            }
            default("argumentMapping", "FirEmptyAnnotationArgumentMapping")
            default("annotationTypeRef", "FirImplicitTypeRefImplWithoutSource")
            default("annotationResolvePhase", "FirAnnotationResolvePhase.Unresolved")
            useTypes(emptyArgumentListType, emptyAnnotationArgumentMappingType, firImplicitTypeWithoutSourceType)
            withCopy()
        }

        builder(errorAnnotationCall) {
            parents += callBuilder
            default("argumentList", "FirEmptyArgumentList")
            default("argumentMapping", "FirEmptyAnnotationArgumentMapping")
            default("annotationTypeRef", "FirImplicitTypeRefImplWithoutSource")
            useTypes(emptyArgumentListType, emptyAnnotationArgumentMappingType, firImplicitTypeWithoutSourceType)
        }

        builder(arrayLiteral) {
            parents += callBuilder
        }

        builder(augmentedArraySetCall) {
            default("calleeReference", "FirStubReference")
            useTypes(stubReferenceType)
        }

        builder(propertyAccessExpression) {
            parents += qualifiedAccessExpressionBuilder
            defaultNoReceivers()
        }

        builder(callableReferenceAccess) {
            parents += qualifiedAccessExpressionBuilder
            defaultNull("explicitReceiver")
            defaultNoReceivers()
            defaultFalse("hasQuestionMarkAtLHS")
        }

        builder(componentCall) {
            parents += callBuilder
            defaultNoReceivers(notNullExplicitReceiver = true)
            default("argumentList") {
                value = "FirEmptyArgumentList"
            }
            useTypes(emptyArgumentListType)
        }

        builder(whileLoop) {
            parents += loopBuilder
            defaultNull("label")
        }

        builder(doWhileLoop) {
            parents += loopBuilder
            defaultNull("label")
        }

        builder(errorExpression) {
            defaultNull("expression")
        }

        builder(errorLoop) {
            defaultNull("label")
        }

        builder(delegatedConstructorCall, type = "FirDelegatedConstructorCallImpl") {
            parents += callBuilder
            default("argumentList") {
                value = "FirEmptyArgumentList"
            }
            default("dispatchReceiver", "FirNoReceiverExpression")
            useTypes(noReceiverExpressionType)
            useTypes(emptyArgumentListType)
        }

        val configurationForFunctionCallBuilder: LeafBuilderConfigurationContext.() -> Unit = {
            parents += abstractFunctionCallBuilder
            defaultNoReceivers()
            openBuilder()
            default("argumentList") {
                value = "FirEmptyArgumentList"
            }
            useTypes(emptyArgumentListType)
        }

        builder(functionCall) {
            configurationForFunctionCallBuilder()
            default("origin") {
                value = "FirFunctionCallOrigin.Regular"
            }
        }
        builder(integerLiteralOperatorCall, init = configurationForFunctionCallBuilder)
        builder(implicitInvokeCall, init = configurationForFunctionCallBuilder)

        builder(getClassCall) {
            parents += callBuilder
        }

        val variableBuilder by builder {
            fields from variable without listOf("symbol", "typeParameters", "isVal")
            parents += declarationBuilder
        }

        builder(property) {
            parents += variableBuilder
            parents += typeParametersOwnerBuilder
            defaultNull("getter", "setter", "containerSource", "delegateFieldSymbol")
            default("resolvePhase", "FirResolvePhase.RAW_FIR")
            default("bodyResolveState", "FirPropertyBodyResolveState.NOTHING_RESOLVED")
            withCopy()
        }

        builder(field) {
            parents += variableBuilder
            default("resolvePhase", "FirResolvePhase.DECLARATIONS")
            openBuilder()
            withCopy()
        }

        builder(typeOperatorCall) {
            parents += callBuilder
            default("argumentList") {
                value = "FirEmptyArgumentList"
            }
            useTypes(emptyArgumentListType)
        }

        builder(stringConcatenationCall) {
            parents += callBuilder
        }

        builder(thisReceiverExpression) {
            parents += qualifiedAccessExpressionBuilder
            default("isImplicit", "false")
            withCopy()
        }

        builder(thisReference, "FirExplicitThisReference") {
            default("contextReceiverNumber", "-1")
        }

        builder(thisReference, "FirImplicitThisReference") {
            default("contextReceiverNumber", "-1")
        }

        builder(anonymousFunction) {
            parents += functionBuilder
            defaultNull("invocationKind", "label", "body", "controlFlowGraphReference")
            default("inlineStatus", "InlineStatus.Unknown")
            default("contractDescription", "FirEmptyContractDescription")
            default("status", "FirResolvedDeclarationStatusImpl.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS")
            default("typeRef", "FirImplicitTypeRefImplWithoutSource")
            withCopy()
            useTypes(emptyContractDescriptionType, resolvedDeclarationStatusImport, firImplicitTypeWithoutSourceType)
        }

        builder(propertyAccessor) {
            parents += functionBuilder
            defaultNull("body")
            default("contractDescription", "FirEmptyContractDescription")
            useTypes(emptyContractDescriptionType)
            withCopy()
        }

        builder(whenExpression) {
            defaultNull("exhaustivenessStatus")
            default("calleeReference", "FirStubReference")
            useTypes(stubReferenceType)
        }

        builder(resolvedTypeRef) {
            defaultNull("delegatedTypeRef")
            withCopy()
        }

        builder(functionTypeRef) {
            withCopy()
        }

        builder(breakExpression) {
            parents += loopJumpBuilder
        }

        builder(continueExpression) {
            parents += loopJumpBuilder
        }

        builder(contextReceiver) {
            withCopy()
        }

        builder(valueParameter, type = "FirValueParameterImpl") {
            openBuilder()
            withCopy()
        }

        builder(valueParameter, type = "FirDefaultSetterValueParameter") {
            defaultNull("defaultValue", "initializer", "delegate", "receiverParameter", "getter", "setter")
            defaultFalse("isCrossinline", "isNoinline", "isVararg", "isVar")
            defaultTrue("isVal")
        }

        builder(simpleFunction) {
            parents += functionBuilder
            parents += typeParametersOwnerBuilder
            defaultNull("body")
            default("contractDescription", "FirEmptyContractDescription")
            useTypes(emptyContractDescriptionType)
            openBuilder()
            withCopy()
        }

        builder(smartCastExpression) {
        }

        builder(tryExpression) {
            default("calleeReference", "FirStubReference")
            useTypes(stubReferenceType)
        }

        builder(checkNotNullCall) {
            default("calleeReference", "FirStubReference")
            useTypes(stubReferenceType)
        }

        builder(elvisExpression) {
            default("calleeReference", "FirStubReference")
            useTypes(stubReferenceType)
        }

        builder(anonymousInitializer) {
            parents += declarationBuilder
            default("symbol", "FirAnonymousInitializerSymbol()")
        }

        val abstractResolvedQualifierBuilder by builder {
            fields from resolvedQualifier
        }

        builder(script) {
            withCopy()
        }

        builder(codeFragment) {
            withCopy()
        }

        builder(resolvedQualifier) {
            parents += abstractResolvedQualifierBuilder
            defaultFalse("isNullableLHSForCallableReference", "isFullyQualified", "canBeValue")
        }

        builder(errorResolvedQualifier) {
            parents += abstractResolvedQualifierBuilder
            defaultFalse("isNullableLHSForCallableReference", "isFullyQualified", "canBeValue")
        }

//        builder(safeCallExpression) {
//            useTypes(safeCallCheckedSubjectType)
//        }
//
//        builder(checkedSafeCallSubject) {
//            useTypes(expressionType)
//        }
//
//        builder(whenSubjectExpression) {
//            useTypes(whenExpressionType)
//        }

        noBuilder(constExpression)

        // -----------------------------------------------------------------------

        findImplementationsWithElementInParents(annotationContainer).forEach {
            it.builder?.parents?.add(annotationContainerBuilder)
        }

        findImplementationsWithElementInParents(expression).forEach {
            it.builder?.parents?.add(expressionBuilder)
        }

        configureFieldInAllLeafBuilders(
            field = "resolvePhase",
            fieldPredicate = { it.defaultValueInImplementation == null }
        ) {
            default(it, "FirResolvePhase.RAW_FIR")
        }

        configureFieldInAllLeafBuilders(
            field = "resolveState",
            fieldPredicate = { it.invisibleField },
            builderPredicate = { it.wantsCopy },
        ) {
            useTypes(resolvePhaseExtensionImport)
        }

        configureFieldInAllLeafBuilders(
            field = "containerSource"
        ) {
            defaultNull(it)
        }

        configureFieldInAllLeafBuilders(
            field = "attributes",
            fieldPredicate = { it.type == declarationAttributesType.type }
        ) {
            default(it, "${declarationAttributesType.type}()")
        }

        configureFieldInAllLeafBuilders(
            field = "deprecationsProvider"
        ) {
            default(it, "UnresolvedDeprecationProvider")
            useTypes(unresolvedDeprecationsProviderType)
        }
    }

    private inline fun findImplementationsWithElementInParents(
        element: Element,
        implementationPredicate: (Implementation) -> Boolean = { true }
    ): Collection<Implementation> {
        return FirTreeBuilder.elements.flatMap { it.allImplementations }.mapNotNullTo(mutableSetOf()) {
            if (!implementationPredicate(it)) return@mapNotNullTo null
            var hasAnnotations = false
            if (it.element == element) return@mapNotNullTo null
            it.element.traverseParents {
                if (it == element) {
                    hasAnnotations = true
                }
            }
            it.takeIf { hasAnnotations }
        }
    }

    private fun configureFieldInAllLeafBuilders(
        field: String,
        builderPredicate: ((LeafBuilder) -> Boolean)? = null,
        fieldPredicate: ((Field) -> Boolean)? = null,
        init: LeafBuilderConfigurationContext.(field: String) -> Unit
    ) {
        val builders = FirTreeBuilder.elements.flatMap { it.allImplementations }.mapNotNull { it.builder }
        for (builder in builders) {
            if (builderPredicate != null && !builderPredicate(builder)) continue
            if (!builder.allFields.any { it.name == field }) continue
            if (fieldPredicate != null && !fieldPredicate(builder[field])) continue
            LeafBuilderConfigurationContext(builder).init(field)
        }
    }

}
