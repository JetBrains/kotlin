/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator

import org.jetbrains.kotlin.fir.tree.generator.FieldSets.annotations
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.arguments
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.body
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.calleeReference
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.classKind
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.controlFlowGraphReferenceField
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.declarations
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.effectiveVisibility
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.initializer
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.modality
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.name
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.receivers
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.returnTypeRef
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.scopeProvider
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.status
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.superTypeRefs
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.symbol
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.symbolWithPackage
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.typeArguments
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.typeParameterRefs
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.typeParameters
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.typeRefField
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.valueParameters
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.visibility
import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFieldConfigurator
import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.context.type
import org.jetbrains.kotlin.fir.tree.generator.model.*
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

object NodeConfigurator : AbstractFieldConfigurator<FirTreeBuilder>(FirTreeBuilder) {
    fun configureFields() = configure {
        AbstractFirTreeBuilder.baseFirElement.configure {
            +field("source", sourceElementType, nullable = true)
        }

        annotationContainer.configure {
            +annotations
        }

        symbolOwner.configure {
            withArg("E", symbolOwner, declaration)
            +symbolWithPackage("fir.symbols", "AbstractFirBasedSymbol", "E")
        }

        typeParameterRef.configure {
            +symbol(typeParameterSymbolType.type)
        }

        typeParametersOwner.configure {
            +typeParameters.withTransform()
        }

        typeParameterRefsOwner.configure {
            +typeParameterRefs.withTransform()
        }

        resolvable.configure {
            +calleeReference.withTransform()
        }

        diagnosticHolder.configure {
            +field("diagnostic", coneDiagnosticType)
        }

        controlFlowGraphOwner.configure {
            +controlFlowGraphReferenceField
        }

        declaration.configure {
            +field("session", firSessionType)
            +field("resolvePhase", resolvePhaseType, withReplace = true).apply { isMutable = true }
            +field("origin", declarationOriginType)
            +field("attributes", declarationAttributesType)
        }

        typedDeclaration.configure {
            +field("returnTypeRef", typeRef, withReplace = true).withTransform()
        }

        callableDeclaration.configure {
            withArg("F", "FirCallableDeclaration<F>")
            parentArg(symbolOwner, "E", "F")
            +field("receiverTypeRef", typeRef, nullable = true, withReplace = true).withTransform()
            +symbol("FirCallableSymbol", "F")
        }

        callableMemberDeclaration.configure {
            withArg("F", "FirCallableMemberDeclaration<F>")
            parentArg(callableDeclaration, "F", "F")
            +field("containerSource", type(DeserializedContainerSource::class), nullable = true)
        }

        function.configure {
            withArg("F", "FirFunction<F>")
            parentArg(callableDeclaration, "F", "F")
            +symbol("FirFunctionSymbol", "F")
            +fieldList(valueParameter, withReplace = true).withTransform()
            +body(nullable = true).withTransform()
        }

        errorFunction.configure {
            parentArg(function, "F", errorFunction)
            +symbol("FirErrorFunctionSymbol")
            +typeParameters
        }

        memberDeclaration.configure {
            +typeParameterRefs
            +status.withTransform()
        }

        expression.configure {
            +typeRefField
            +annotations
        }

        argumentList.configure {
            +arguments.withTransform()
        }

        call.configure {
            +field(argumentList, withReplace = true)
        }

        block.configure {
            +fieldList(statement).withTransform()
            +typeRefField
            needTransformOtherChildren()
        }

        binaryLogicExpression.configure {
            +field("leftOperand", expression).withTransform()
            +field("rightOperand", expression).withTransform()
            +field("kind", operationKindType)
            needTransformOtherChildren()
        }

        jump.configure {
            withArg("E", targetElement)
            +field("target", jumpTargetType.withArgs("E"))
        }

        loopJump.configure {
            parentArg(jump, "E", loop)
        }

        returnExpression.configure {
            parentArg(jump, "E", function.withArgs("F" to "*"))
            +field("result", expression).withTransform()
            needTransformOtherChildren()
        }

        label.configure {
            +stringField("name")
        }

        loop.configure {
            +field(block).withTransform()
            +field("condition", expression).withTransform()
            +field(label, nullable = true)
            needTransformOtherChildren()
        }

        whileLoop.configure {
            +field("condition", expression).withTransform()
            +field(block).withTransform()
        }

        catchClause.configure {
            +field("parameter", valueParameter).withTransform()
            +field(block).withTransform()
            needTransformOtherChildren()
        }

        tryExpression.configure {
            +field("tryBlock", block).withTransform()
            +fieldList("catches", catchClause).withTransform()
            +field("finallyBlock", block, nullable = true).withTransform()
            needTransformOtherChildren()
        }

        elvisExpression.configure {
            +field("lhs", expression).withTransform()
            +field("rhs", expression).withTransform()
        }

        qualifiedAccessWithoutCallee.configure {
            +typeArguments.withTransform()
            +receivers
        }

        constExpression.configure {
            withArg("T")
            +field("kind", constKindType.withArgs("T"), withReplace = true)
            +field("value", "T", null)
        }

        functionCall.configure {
            +field("calleeReference", namedReference)
        }

        comparisonExpression.configure {
            +field("operation", operationType)
            +field("compareToCall", functionCall)
        }

        typeOperatorCall.configure {
            +field("operation", operationType)
            +field("conversionTypeRef", typeRef).withTransform()
            needTransformOtherChildren()
        }

        assignmentOperatorStatement.configure {
            +field("operation", operationType)
            +field("leftArgument", expression).withTransform()
            +field("rightArgument", expression).withTransform()
        }

        equalityOperatorCall.configure {
            +field("operation", operationType)
        }

        whenBranch.configure {
            +field("condition", expression).withTransform()
            +field("result", block).withTransform()
            needTransformOtherChildren()
        }

        classLikeDeclaration.configure {
            withArg("F", "FirClassLikeDeclaration<F>")
            parentArg(symbolOwner, "F", "F")
            +symbol("FirClassLikeSymbol", "F")
        }

        klass.configure {
            withArg("F", "FirClass<F>")
            parentArg(classLikeDeclaration, "F", "F")
            +symbol("FirClassSymbol", "F")
            +classKind
            +superTypeRefs(withReplace = true).withTransform()
            +declarations.withTransform()
            +annotations
            +scopeProvider
        }

        regularClass.configure {
            parentArg(klass, "F", regularClass)
            +name
            +symbol("FirRegularClassSymbol")
            +field("companionObject", regularClass, nullable = true).withTransform()
            +booleanField("hasLazyNestedClassifiers")
            +superTypeRefs(withReplace = true)
        }

        anonymousObject.configure {
            parentArg(klass, "F", anonymousObject)
            +symbol("FirAnonymousObjectSymbol")
        }

        typeAlias.configure {
            +typeParameters
            parentArg(classLikeDeclaration, "F", typeAlias)
            +name
            +symbol("FirTypeAliasSymbol")
            +field("expandedTypeRef", typeRef, withReplace = true)
            +annotations
        }

        anonymousFunction.configure {
            parentArg(function, "F", anonymousFunction)
            +symbol("FirAnonymousFunctionSymbol")
            +field(label, nullable = true)
            +field("invocationKind", eventOccurrencesRangeType, nullable = true, withReplace = true).apply {
                isMutable = true
            }
            +booleanField("isLambda")
            +typeParameters
        }

        typeParameter.configure {
            parentArg(symbolOwner, "F", typeParameter)
            +name
            +symbol("FirTypeParameterSymbol")
            +field(varianceType)
            +booleanField("isReified")
            +fieldList("bounds", typeRef, withReplace = true)
            +annotations
        }

        simpleFunction.configure {
            parentArg(function, "F", simpleFunction)
            parentArg(callableMemberDeclaration, "F", simpleFunction)
            +name
            +symbol("FirFunctionSymbol<FirSimpleFunction>")
            +annotations
            +typeParameters
        }

        contractDescriptionOwner.configure {
            +field(contractDescription, withReplace = true).withTransform()
        }

        property.configure {
            parentArg(variable, "F", property)
            parentArg(callableMemberDeclaration, "F", property)
            +symbol("FirPropertySymbol")
            +field("backingFieldSymbol", backingFieldSymbolType)
            +booleanField("isLocal")
            +status
        }

        propertyAccessor.configure {
            parentArg(function, "F", propertyAccessor)
            +symbol("FirPropertyAccessorSymbol")
            +booleanField("isGetter")
            +booleanField("isSetter")
            +status.withTransform()
            +annotations
            +typeParameters
        }

        declarationStatus.configure {
            +visibility
            +modality
            generateBooleanFields(
                "expect", "actual", "override", "operator", "infix", "inline", "tailRec",
                "external", "const", "lateInit", "inner", "companion", "data", "suspend", "static",
                "fromSealedClass", "fromEnumClass", "fun"
            )
        }

        resolvedDeclarationStatus.configure {
            shouldBeAnInterface()
        }

        constructor.configure {
            parentArg(function, "F", constructor)
            parentArg(callableMemberDeclaration, "F", constructor)
            +annotations
            +symbol("FirConstructorSymbol")
            +field("delegatedConstructor", delegatedConstructorCall, nullable = true).withTransform()
            +body(nullable = true)
            +booleanField("isPrimary")
        }

        delegatedConstructorCall.configure {
            +field("constructedTypeRef", typeRef, withReplace = true)
            +field("dispatchReceiver", expression).withTransform()
            generateBooleanFields("this", "super")
        }

        valueParameter.configure {
            parentArg(variable, "F", valueParameter)
            +field("defaultValue", expression, nullable = true)
            generateBooleanFields("crossinline", "noinline", "vararg")
        }

        variable.configure {
            withArg("F", variable)
            parentArg(callableDeclaration, "F", "F")
            +name
            +symbol("FirVariableSymbol", "F")
            +initializer.withTransform()
            +field("delegate", expression, nullable = true).withTransform()
            +field("delegateFieldSymbol", delegateFieldSymbolType, "F", nullable = true)
            generateBooleanFields("var", "val")
            +field("getter", propertyAccessor, nullable = true).withTransform()
            +field("setter", propertyAccessor, nullable = true).withTransform()
            +annotations
            needTransformOtherChildren()
        }

        errorProperty.configure {
            parentArg(variable, "F", errorProperty)
            +symbol("FirErrorPropertySymbol")
        }

        enumEntry.configure {
            parentArg(variable, "F", enumEntry)
            parentArg(callableMemberDeclaration, "F", enumEntry)
        }

        field.configure {
            parentArg(variable, "F", field)
            parentArg(callableMemberDeclaration, "F", field)
        }

        anonymousInitializer.configure {
            parentArg(symbolOwner, "E", anonymousInitializer)
            +body(nullable = true)
            +symbol(anonymousInitializerSymbolType.type)
        }

        file.configure {
            +fieldList(import).withTransform()
            +declarations.withTransform()
            +stringField("name")
            +field("packageFqName", fqNameType)
        }

        import.configure {
            +field("importedFqName", fqNameType, nullable = true)
            +booleanField("isAllUnder")
            +field("aliasName", nameType, nullable = true)
        }

        resolvedImport.configure {
            +field("delegate", import)
            +field("packageFqName", fqNameType)
            +field("relativeClassName", fqNameType, nullable = true)
            +field("resolvedClassId", classIdType, nullable = true)
            +field(
                "importedName",
                nameType,
                nullable = true
            )
        }

        annotationCall.configure {
            +field("useSiteTarget", annotationUseSiteTargetType, nullable = true)
            +field("annotationTypeRef", typeRef).withTransform()
            +field("resolveStatus", annotationResolveStatusType, withReplace = true)
        }

        arraySetCall.configure {
            +field("assignCall", functionCall)
            +field("setGetBlock", block)
            +field("operation", operationType)
            +field("calleeReference", reference, withReplace = true)
        }

        classReferenceExpression.configure {
            +field("classTypeRef", typeRef)
        }

        componentCall.configure {
            +field("explicitReceiver", expression)
            +intField("componentIndex")
        }

        expressionWithSmartcast.configure {
            +field("originalExpression", qualifiedAccessExpression)
            +field("typesFromSmartCast", "Collection<ConeKotlinType>", null, customType = coneKotlinTypeType)
            +field("originalType", typeRef)
        }

        safeCallExpression.configure {
            +field("receiver", expression).withTransform()
            // Special node that might be used as a reference to receiver of a safe call after null check
            +field("checkedSubjectRef", safeCallCheckedSubjectReferenceType)
            // One that uses checkedReceiver as a receiver
            +field("regularQualifiedAccess", qualifiedAccess, withReplace = true).withTransform()
        }

        checkedSafeCallSubject.configure {
            +field("originalReceiverRef", safeCallOriginalReceiverReferenceType)
        }

        callableReferenceAccess.configure {
            +field("calleeReference", namedReference, withReplace = true).withTransform()
            +booleanField("hasQuestionMarkAtLHS", withReplace = true)
        }

        getClassCall.configure {
            +field("argument", expression)
        }

        wrappedArgumentExpression.configure {
            +booleanField("isSpread")
        }

        namedArgumentExpression.configure {
            +name
        }

        varargArgumentsExpression.configure {
            +fieldList("arguments", expression)
            +field("varargElementType", typeRef)
        }

        resolvedQualifier.configure {
            +field("packageFqName", fqNameType)
            +field("relativeClassFqName", fqNameType, nullable = true)
            +field("classId", classIdType, nullable = true)
            +field("symbol", classLikeSymbolType, nullable = true)
            +booleanField("isNullableLHSForCallableReference", withReplace = true)
            +typeArguments.withTransform()
        }

        resolvedReifiedParameterReference.configure {
            +field("symbol", typeParameterSymbolType)
        }

        stringConcatenationCall.configure {
        }

        throwExpression.configure {
            +field("exception", expression)
        }

        variableAssignment.configure {
            +field("lValue", reference)
            +field("rValue", expression).withTransform()
        }

        whenSubjectExpression.configure {
            +field("whenRef", whenRefType)
        }

        wrappedExpression.configure {
            +field(expression)
        }

        wrappedDelegateExpression.configure {
            +field("delegateProvider", expression)
        }

        namedReference.configure {
            +name
            +field("candidateSymbol", abstractFirBasedSymbolType, "*", nullable = true)
        }

        resolvedNamedReference.configure {
            +field("resolvedSymbol", abstractFirBasedSymbolType, "*")
        }

        resolvedCallableReference.configure {
            +fieldList("inferredTypeArguments", coneKotlinTypeType)
        }

        delegateFieldReference.configure {
            +field("resolvedSymbol", delegateFieldSymbolType.withArgs("*"))
        }

        backingFieldReference.configure {
            +field("resolvedSymbol", backingFieldSymbolType)
        }

        superReference.configure {
            +stringField("labelName", nullable = true)
            +field("superTypeRef", typeRef, withReplace = true)
        }

        thisReference.configure {
            +stringField("labelName", nullable = true)
            +field("boundSymbol", abstractFirBasedSymbolType, "*", nullable = true, withReplace = true)
        }

        typeRef.configure {
            +annotations
        }

        resolvedTypeRef.configure {
            +field("type", coneKotlinTypeType)
            +field("delegatedTypeRef", typeRef, nullable = true)
            +booleanField("isSuspend")
        }

        delegatedTypeRef.configure {
            +field("delegate", expression, nullable = true)
            +field(typeRef)
        }

        typeRefWithNullability.configure {
            +booleanField("isMarkedNullable")
        }

        userTypeRef.configure {
            +fieldList("qualifier", firQualifierPartType)
        }

        functionTypeRef.configure {
            +field("receiverTypeRef", typeRef, nullable = true)
            +valueParameters
            +returnTypeRef
            +booleanField("isSuspend")
        }

        composedSuperTypeRef.configure {
            +fieldList("superTypeRefs", resolvedTypeRef)
        }

        thisReceiverExpression.configure {
            +field("calleeReference", thisReference)
        }

        whenExpression.configure {
            +field("subject", expression, nullable = true).withTransform()
            +field("subjectVariable", variable.withArgs("F" to "*"), nullable = true)
            +fieldList("branches", whenBranch).withTransform()
            +booleanField("isExhaustive", withReplace = true)
            needTransformOtherChildren()
        }

        typeProjectionWithVariance.configure {
            +field(typeRef)
            +field(varianceType)
        }

        rawContractDescription.configure {
            +field("contractCall", functionCall)
        }

        resolvedContractDescription.configure {
            +fieldList("effects", effectDeclarationType)
            +fieldList("unresolvedEffects", statement)
        }
    }
}

private fun Element.withArgs(vararg replacements: Pair<String, String>): AbstractElement {
    val replaceMap = replacements.toMap()
    val newArguments = typeArguments.map { replaceMap[it.name]?.let { SimpleTypeArgument(it, null) } ?: it }
    return ElementWithArguments(this, newArguments)
}
