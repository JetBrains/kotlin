/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator

import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirBuilderConfigurator
import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder

class BuilderConfigurator(model: Model) : AbstractFirBuilderConfigurator<AbstractFirTreeBuilder>(model) {
    override fun configureBuilders() = with(FirTree) {
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
            isSealed = true
        }

        builder(file) {
            default("symbol", "FirFileSymbol()")
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
                defaultNull("delegatedConstructor", "body", "contractDescription")
            }
        }

        builder(errorPrimaryConstructor) {
            parents += abstractConstructorBuilder
            defaultNull("delegatedConstructor", "body", "contractDescription")
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
            parents += typeParameterRefsOwnerBuilder
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
            additionalImports(emptyArgumentListType, emptyAnnotationArgumentMappingType, firImplicitTypeWithoutSourceType)
            withCopy()
        }

        builder(errorAnnotationCall) {
            parents += callBuilder
            default("argumentList", "FirEmptyArgumentList")
            default("argumentMapping", "FirEmptyAnnotationArgumentMapping")
            default("annotationTypeRef", "FirImplicitTypeRefImplWithoutSource")
            additionalImports(emptyArgumentListType, emptyAnnotationArgumentMappingType, firImplicitTypeWithoutSourceType)
        }

        builder(arrayLiteral) {
            parents += callBuilder
        }

        builder(indexedAccessAugmentedAssignment) {
            default("calleeReference", "FirStubReference")
            additionalImports(stubReferenceType)
        }

        builder(propertyAccessExpression) {
            parents += qualifiedAccessExpressionBuilder
            defaultNoReceivers()
            withCopy()
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
            additionalImports(emptyArgumentListType)
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
            defaultNull("dispatchReceiver")
            additionalImports(emptyArgumentListType)
        }

        val configurationForFunctionCallBuilder: LeafBuilderConfigurationContext.() -> Unit = {
            parents += abstractFunctionCallBuilder
            defaultNoReceivers()
            openBuilder()
            default("argumentList") {
                value = "FirEmptyArgumentList"
            }
            additionalImports(emptyArgumentListType)
        }

        builder(functionCall) {
            configurationForFunctionCallBuilder()
            default("origin") {
                value = "FirFunctionCallOrigin.Regular"
            }
        }
        builder(integerLiteralOperatorCall, config = configurationForFunctionCallBuilder)
        builder(implicitInvokeCall) {
            configurationForFunctionCallBuilder()
            defaultFalse("isCallWithExplicitReceiver")
        }

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

        builder(backingField) {
            parents += variableBuilder
            default("resolvePhase", "FirResolvePhase.DECLARATIONS")
        }

        builder(enumEntry) {
            withCopy()
        }

        builder(typeOperatorCall) {
            parents += callBuilder
            default("argumentList") {
                value = "FirEmptyArgumentList"
            }
            additionalImports(emptyArgumentListType)
        }

        builder(stringConcatenationCall) {
            parents += callBuilder
        }

        builder(thisReceiverExpression) {
            parents += qualifiedAccessExpressionBuilder
            default("isImplicit", "false")
            withCopy()
        }

        builder(anonymousFunction) {
            parents += functionBuilder
            defaultNull("invocationKind", "label", "body", "controlFlowGraphReference", "contractDescription")
            default("inlineStatus", "InlineStatus.Unknown")
            default("status", "FirResolvedDeclarationStatusImpl.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS")
            default("typeRef", "FirImplicitTypeRefImplWithoutSource")
            additionalImports(resolvedDeclarationStatusImport, firImplicitTypeWithoutSourceType)
        }

        builder(propertyAccessor) {
            parents += functionBuilder
            defaultNull("body", "contractDescription")
            withCopy()
        }

        builder(whenExpression) {
            defaultNull("exhaustivenessStatus")
            default("calleeReference", "FirStubReference")
            additionalImports(stubReferenceType)
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

        builder(valueParameter, type = "FirValueParameterImpl") {
            openBuilder()
            withCopy()
            defaultFalse("isCrossinline", "isNoinline", "isVararg")
            default("valueParameterKind", "FirValueParameterKind.Regular")
        }

        builder(valueParameter, type = "FirDefaultSetterValueParameter") {
        }

        builder(simpleFunction) {
            parents += functionBuilder
            parents += typeParametersOwnerBuilder
            defaultNull("body", "contractDescription")
            openBuilder()
            withCopy()
        }

        builder(smartCastExpression) {
        }

        builder(tryExpression) {
            default("calleeReference", "FirStubReference")
            additionalImports(stubReferenceType)
        }

        builder(checkNotNullCall) {
            default("calleeReference", "FirStubReference")
            additionalImports(stubReferenceType)
        }

        builder(elvisExpression) {
            default("calleeReference", "FirStubReference")
            additionalImports(stubReferenceType)
        }

        builder(anonymousInitializer) {
            parents += declarationBuilder
            default("symbol", "FirAnonymousInitializerSymbol()")
        }

        builder(spreadArgumentExpression) {
            defaultFalse("isNamed", "isFakeSpread")
        }

        val abstractWhenBranchBuilder by builder {
            fields from whenBranch without "hasGuard"
        }
        builder(whenBranch, type = "FirRegularWhenBranch") {
            parents += abstractWhenBranchBuilder
        }
        builder(whenBranch, type = "FirGuardedWhenBranch") {
            parents += abstractWhenBranchBuilder
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

        noBuilder(literalExpression)

        builder(samConversionExpression) {
            withCopy()
        }

        // -----------------------------------------------------------------------

        findImplementationsWithElementInParents(annotationContainer).forEach {
            it.builder?.parents?.add(annotationContainerBuilder)
        }

        findImplementationsWithElementInParents(expression).forEach {
            it.builder?.parents?.add(expressionBuilder)
        }

        configureFieldInAllLeafBuilders(
            field = "resolvePhase",
            fieldPredicate = { it.implementationDefaultStrategy!!.defaultValue == null }
        ) {
            default(it, "FirResolvePhase.RAW_FIR")
        }

        configureFieldInAllLeafBuilders(
            field = "resolveState",
            fieldPredicate = { it.invisibleField },
            builderPredicate = { it.wantsCopy },
        ) {
            additionalImports(resolvePhaseExtensionImport)
        }

        configureFieldInAllLeafBuilders(
            field = "containerSource"
        ) {
            defaultNull(it)
        }

        configureFieldInAllLeafBuilders(
            field = "attributes",
            fieldPredicate = { it.typeRef == declarationAttributesType }
        ) {
            default(it, "${declarationAttributesType.typeName}()")
        }

        configureFieldInAllLeafBuilders(
            field = "deprecationsProvider"
        ) {
            default(it, "UnresolvedDeprecationProvider")
            additionalImports(unresolvedDeprecationsProviderType)
        }
    }
}
