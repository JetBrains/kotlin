/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator

import org.jetbrains.kotlin.fir.tree.generator.context.AbstractBuilderConfigurator
import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.fir.tree.generator.model.Field
import org.jetbrains.kotlin.fir.tree.generator.model.Implementation
import org.jetbrains.kotlin.fir.tree.generator.model.LeafBuilder
import org.jetbrains.kotlin.fir.tree.generator.util.traverseParents

object BuilderConfigurator : AbstractBuilderConfigurator<FirTreeBuilder>(FirTreeBuilder) {
    fun configureBuilders() = with(firTreeBuilder) {
        val annotationContainerBuilder by builder {
            fields from annotationContainer
        }

        val expressionBuilder by builder {
            fields from expression
        }

        val typeParametersOwnerBuilder by builder {
            fields from typeParametersOwner
        }

        val classBuilder by builder {
            parents += annotationContainerBuilder
            fields from klass without listOf("symbol", "resolvePhase")
        }

        val regularClassBuilder by builder("AbstractFirRegularClassBuilder") {
            parents += classBuilder
            parents += typeParametersOwnerBuilder
            fields from regularClass
        }

        val qualifiedAccessBuilder by builder {
            fields from qualifiedAccess without "calleeReference"
        }

        val callBuilder by builder {
            fields from call
        }

        val loopBuilder by builder {
            fields from loop
        }

        val functionBuilder by builder {
            parents += annotationContainerBuilder
            fields from function without listOf("symbol", "resolvePhase", "controlFlowGraphReference", "receiverTypeRef")
        }

        val loopJumpBuilder by builder {
            fields from loopJump without "typeRef"
        }

        val abstractConstructorBuilder by builder {
            parents += functionBuilder
            fields from constructor without "isPrimary"
        }

        for (constructorType in listOf("FirPrimaryConstructor", "FirConstructorImpl")) {
            builder(constructor, constructorType) {
                parents += abstractConstructorBuilder
                defaultNull("delegatedConstructor")
                defaultNull("body")
            }
        }

        builder(constructor, "FirConstructorImpl") {
            openBuilder()
        }

        builder(field) {
            default("resolvePhase", "FirResolvePhase.DECLARATIONS")
            openBuilder()
        }

        builder(regularClass) {
            parents += regularClassBuilder
            defaultNull("companionObject")
            openBuilder()
        }

        builder(sealedClass) {
            parents += regularClassBuilder
            defaultNull("companionObject")
        }

        builder(anonymousObject) {
            parents += classBuilder
        }

        builder(typeAlias) {
            parents += typeParametersOwnerBuilder
        }

        builder(annotationCall) {
            parents += callBuilder
            default("argumentList") {
                value = "FirEmptyArgumentList"
            }
            useTypes(emptyArgumentListType)
        }

        builder(arrayOfCall) {
            parents += callBuilder
        }

        builder(arraySetCall) {
            default("calleeReference", "FirStubReference")
            useTypes(stubReferenceType)
        }

        builder(callableReferenceAccess) {
            parents += qualifiedAccessBuilder
            defaultNull("explicitReceiver")
            defaultNoReceivers()
            defaultFalse("safe")
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

        builder(errorLoop) {
            defaultNull("label")
        }

        builder(delegatedConstructorCall) {
            parents += callBuilder
            default("argumentList") {
                value = "FirEmptyArgumentList"
            }
            useTypes(emptyArgumentListType)
        }

        builder(functionCall) {
            parents += qualifiedAccessBuilder
            parents += callBuilder
            defaultFalse("safe")
            defaultNoReceivers()
            openBuilder()
            default("argumentList") {
                value = "FirEmptyArgumentList"
            }
            useTypes(emptyArgumentListType)
        }

        builder(qualifiedAccessExpression) {
            parents += qualifiedAccessBuilder
            defaultFalse("safe")
            defaultNoReceivers()
        }

        builder(getClassCall) {
            parents += callBuilder
        }

        builder(property) {
            parents += typeParametersOwnerBuilder
            defaultNull("getter", "setter", "containerSource", "delegateFieldSymbol")
            default("resolvePhase", "FirResolvePhase.RAW_FIR")
        }

        builder(operatorCall) {
            parents += callBuilder
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
            parents += qualifiedAccessBuilder
        }

        builder(variableAssignment) {
            parents += qualifiedAccessBuilder
            defaultNoReceivers()
        }

        builder(anonymousFunction) {
            parents += functionBuilder
            defaultNull("invocationKind", "label", "body")
            default("controlFlowGraphReference", "FirEmptyControlFlowGraphReference")
            useTypes(emptyCfgReferenceType)
        }

        builder(propertyAccessor) {
            parents += functionBuilder
            defaultNull("body")
        }

        builder(whenExpression) {
            defaultFalse("isExhaustive")
            default("calleeReference", "FirStubReference")
            useTypes(stubReferenceType)
        }

        builder(resolvedTypeRef) {
            defaultNull("delegatedTypeRef")
        }

        builder(breakExpression) {
            parents += loopJumpBuilder
        }

        builder(continueExpression) {
            parents += loopJumpBuilder
        }

        builder(valueParameter, type = "FirValueParameterImpl") {
            openBuilder()
        }

        builder(valueParameter, type = "FirDefaultSetterValueParameter") {
            defaultNull("defaultValue", "initializer", "delegate", "receiverTypeRef", "delegateFieldSymbol", "getter", "setter")
            defaultFalse("isCrossinline", "isNoinline", "isVararg", "isVar")
            defaultTrue("isVal")
        }

        builder(simpleFunction) {
            parents += functionBuilder
            parents += typeParametersOwnerBuilder
            defaultNull("body")
            openBuilder()
        }

        builder(tryExpression) {
            default("calleeReference", "FirStubReference")
            useTypes(stubReferenceType)
        }

        builder(checkNotNullCall) {
            default("calleeReference", "FirStubReference")
            useTypes(stubReferenceType)
        }

        builder(anonymousInitializer) {
            default("symbol", "FirAnonymousInitializerSymbol()")
        }

        val elementsWithDefaultTypeRef = listOf(
            thisReceiverExpression,
            callableReferenceAccess,
            anonymousObject,
            qualifiedAccessExpression,
            functionCall,
            anonymousFunction,
            whenExpression,
            tryExpression,
            checkNotNullCall,
            resolvedQualifier,
            resolvedReifiedParameterReference,
            expression to "FirExpressionStub",
            varargArgumentsExpression
        )
        elementsWithDefaultTypeRef.forEach {
            val (element, name) = when (it) {
                is Pair<*, *> -> it.first as Element to it.second as String
                is Element -> it to null
                else -> throw IllegalArgumentException()
            }
            builder(element, name) {
                default("typeRef", "FirImplicitTypeRefImpl(null)")
                useTypes(implicitTypeRefType)
            }
        }

        noBuilder(constExpression)

        // -----------------------------------------------------------------------

        findImplementationsWithElementInParents(annotationContainer) {
            it.type !in setOf("FirDelegatedTypeRefImpl", "FirImplicitTypeRefImpl")
        }.forEach {
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
            field = "containerSource"
        ) {
            defaultNull(it)
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