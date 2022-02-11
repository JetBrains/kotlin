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
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.smartcastStability
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
            +symbolWithPackage("fir.symbols", "FirBasedSymbol", "out FirDeclaration")
            +field("moduleData", firModuleDataType)
            +field("resolvePhase", resolvePhaseType, withReplace = true).apply { isMutable = true; isVolatile = true }
            +field("origin", declarationOriginType)
            +field("attributes", declarationAttributesType)
            shouldBeAbstractClass()
        }

        typedDeclaration.configure {
            +field("returnTypeRef", typeRef, withReplace = true).withTransform()
        }

        callableDeclaration.configure {
            +field("receiverTypeRef", typeRef, nullable = true, withReplace = true).withTransform()
            +field("deprecation", deprecationsPerUseSiteType, nullable = true).withReplace().apply { isMutable = true }
            +symbol("FirCallableSymbol", "out FirCallableDeclaration")

            +field("containerSource", type(DeserializedContainerSource::class), nullable = true)
            +field("dispatchReceiverType", coneSimpleKotlinTypeType, nullable = true)
        }

        function.configure {
            +symbol("FirFunctionSymbol", "out FirFunction")
            +fieldList(valueParameter, withReplace = true).withTransform()
            +body(nullable = true, withReplace = true).withTransform()
        }

        errorExpression.configure {
            +field("expression", expression, nullable = true)
        }

        errorFunction.configure {
            +symbol("FirErrorFunctionSymbol")
            +typeParameters
        }

        memberDeclaration.configure {
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
            parentArg(jump, "E", function.withArgs("E" to "*"))
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

        qualifiedAccess.configure {
            +typeArguments.withTransform()
            +receivers
            +field("source", sourceElementType, nullable = true, withReplace = true)
        }

        propertyAccessExpression.configure {
            +fieldList("nonFatalDiagnostics", coneDiagnosticType)
        }

        constExpression.configure {
            withArg("T")
            +field("kind", constKindType.withArgs("T"), withReplace = true)
            +field("value", "T", null)
        }

        functionCall.configure {
            +field("calleeReference", namedReference)
            +field("origin", functionCallOrigin)
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
            +symbol("FirClassLikeSymbol", "out FirClassLikeDeclaration")
            +field("deprecation", deprecationsPerUseSiteType, nullable = true).withReplace().apply { isMutable = true}
        }

        klass.configure {
            +symbol("FirClassSymbol", "out FirClass")
            +classKind
            +superTypeRefs(withReplace = true).withTransform()
            +declarations.withTransform()
            +annotations
            +scopeProvider
        }

        regularClass.configure {
            +name
            +symbol("FirRegularClassSymbol")
            +booleanField("hasLazyNestedClassifiers")
            +field("companionObjectSymbol", regularClassSymbolType, nullable = true, withReplace = true)
            +superTypeRefs(withReplace = true)
        }

        anonymousObject.configure {
            +symbol("FirAnonymousObjectSymbol")
        }

        anonymousObjectExpression.configure {
            +field(anonymousObject).withTransform()
        }

        typeAlias.configure {
            +typeParameters
            +name
            +symbol("FirTypeAliasSymbol")
            +field("expandedTypeRef", typeRef, withReplace = true).withTransform()
            +annotations
        }

        anonymousFunction.configure {
            +symbol("FirAnonymousFunctionSymbol")
            +field(label, nullable = true)
            +field("invocationKind", eventOccurrencesRangeType, nullable = true, withReplace = true).apply {
                isMutable = true
            }
            +field("inlineStatus", inlineStatusType, withReplace = true).apply {
                isMutable = true
            }
            +booleanField("isLambda")
            +booleanField("hasExplicitParameterList")
            +typeParameters
            +field(typeRef, withReplace = true)
        }

        anonymousFunctionExpression.configure {
            +field(anonymousFunction).withTransform()
        }

        typeParameter.configure {
            +name
            +symbol("FirTypeParameterSymbol")
            +field("containingDeclarationSymbol", firBasedSymbolType, "*", nullable = true).apply {
                withBindThis = false
            }
            +field(varianceType)
            +booleanField("isReified")
            +fieldList("bounds", typeRef, withReplace = true)
            +annotations
        }

        simpleFunction.configure {
            +name
            +symbol("FirNamedFunctionSymbol")
            +annotations
            +typeParameters
        }

        contractDescriptionOwner.configure {
            +field(contractDescription, withReplace = true).withTransform()
        }

        property.configure {
            +symbol("FirPropertySymbol")
            +field("delegateFieldSymbol", delegateFieldSymbolType, nullable = true)
            +booleanField("isLocal")
            +field("bodyResolveState", propertyBodyResolveStateType, withReplace = true)
            +typeParameters
        }

        propertyAccessor.configure {
            +symbol("FirPropertyAccessorSymbol")
            +field("propertySymbol", firPropertySymbolType, nullable = true).apply {
                withBindThis = false
            }
            +booleanField("isGetter")
            +booleanField("isSetter")
            +annotations
            +typeParameters
        }

        backingField.configure {
            +field("symbol", backingFieldSymbolType)
            +field("propertySymbol", firPropertySymbolType).apply {
                withBindThis = false
            }
            +initializer.withTransform().withReplace()
            +annotations
            +typeParameters
            +status.withTransform()
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
            +effectiveVisibility
            shouldBeAnInterface()
        }

        constructor.configure {
            +annotations
            +symbol("FirConstructorSymbol")
            +field("delegatedConstructor", delegatedConstructorCall, nullable = true).withTransform()
            +body(nullable = true)
            +booleanField("isPrimary")
        }

        delegatedConstructorCall.configure {
            +field("constructedTypeRef", typeRef, withReplace = true)
            +field("dispatchReceiver", expression).withTransform()
            +field("calleeReference", reference, withReplace = true)
            generateBooleanFields("this", "super")
        }

        valueParameter.configure {
            +symbol("FirValueParameterSymbol")
            +field("defaultValue", expression, nullable = true)
            generateBooleanFields("crossinline", "noinline", "vararg")
        }

        variable.configure {
            +name
            +symbol("FirVariableSymbol", "out FirVariable")
            +initializer.withTransform().withReplace()
            +field("delegate", expression, nullable = true).withTransform()
            generateBooleanFields("var", "val")
            +field("getter", propertyAccessor, nullable = true, withReplace = true).withTransform()
            +field("setter", propertyAccessor, nullable = true, withReplace = true).withTransform()
            +field("backingField", backingField, nullable = true).withTransform()
            +annotations
            needTransformOtherChildren()
        }

        errorProperty.configure {
            +symbol("FirErrorPropertySymbol")
        }

        enumEntry.configure {
            +symbol("FirEnumEntrySymbol")
        }

        field.configure {
            +symbol("FirFieldSymbol")
        }

        anonymousInitializer.configure {
            +body(nullable = true)
            +symbol("FirAnonymousInitializerSymbol")
        }

        file.configure {
            +field("packageDirective", packageDirective)
            +fieldList(import).withTransform()
            +declarations.withTransform()
            +stringField("name")
            +stringField("path", nullable = true)
            +symbol("FirFileSymbol")
        }

        packageDirective.configure {
            +field("packageFqName", fqNameType)
        }

        import.configure {
            +field("importedFqName", fqNameType, nullable = true)
            +booleanField("isAllUnder")
            +field("aliasName", nameType, nullable = true)
            +field("aliasSource", sourceElementType, nullable = true)
        }

        resolvedImport.configure {
            +field("delegate", import)
            +field("packageFqName", fqNameType)
            +field("relativeParentClassName", fqNameType, nullable = true)
            +field("resolvedParentClassId", classIdType, nullable = true)
            +field(
                "importedName",
                nameType,
                nullable = true
            )
        }

        errorImport.configure {
            +field("delegate", import)
        }

        annotation.configure {
            +field("useSiteTarget", annotationUseSiteTargetType, nullable = true)
            +field("annotationTypeRef", typeRef).withTransform()
            +field("argumentMapping", annotationArgumentMapping, withReplace = true)
        }

        annotationCall.configure {
            +field("argumentMapping", annotationArgumentMapping, withReplace = true)
        }

        annotationArgumentMapping.configure {
            +field("mapping", type("Map") to listOf(nameType, expression))
        }

        augmentedArraySetCall.configure {
            +field("lhsGetCall", functionCall)
            +field("rhs", expression)
            +field("rhs2", expression)
            +field("operation", operationType)
            // Used for resolution errors reporting in case
            +field("calleeReference", reference, withReplace = true)
            +field("arrayAccessSource", sourceElementType, nullable = true)
        }

        classReferenceExpression.configure {
            +field("classTypeRef", typeRef)
        }

        componentCall.configure {
            +field("explicitReceiver", expression)
            +intField("componentIndex")
        }

        wrappedExpressionWithSmartcast.configure {
            withArg("E", expression)
            +typeRefField
            +field("originalExpression", "E", packageName = null)
            +field("typesFromSmartCast", "Collection<ConeKotlinType>", null, customType = coneKotlinTypeType)
            +field("originalType", typeRef)
            +field("smartcastType", typeRef)
            +booleanField("isStable")
            +smartcastStability
        }

        wrappedExpressionWithSmartcastToNull.configure {
            withArg("E", expression)
            parentArg(wrappedExpressionWithSmartcast, "E", "E")
            +field("smartcastTypeWithoutNullableNothing", typeRef)
        }

        expressionWithSmartcast.configure {
            parentArg(wrappedExpressionWithSmartcast, "E", qualifiedAccessExpression)
        }

        expressionWithSmartcastToNull.configure {
            parentArg(wrappedExpressionWithSmartcastToNull, "E", qualifiedAccessExpression)
        }

        safeCallExpression.configure {
            +field("receiver", expression).withTransform()
            // Special node that might be used as a reference to receiver of a safe call after null check
            +field("checkedSubjectRef", safeCallCheckedSubjectReferenceType)
            // One that uses checkedReceiver as a receiver
            +field("selector", statement, withReplace = true).withTransform()
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
            +booleanField("resolvedToCompanionObject", withReplace = true)
            +fieldList("nonFatalDiagnostics", coneDiagnosticType)
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
            +field("lValueTypeRef", typeRef).withReplace()
            +field("rValue", expression).withTransform()
        }

        whenSubjectExpression.configure {
            +field("whenRef", whenRefType)
        }

        whenSubjectExpressionWithSmartcast.configure {
            parentArg(wrappedExpressionWithSmartcast, "E", whenSubjectExpression)
        }

        whenSubjectExpressionWithSmartcastToNull.configure {
            parentArg(wrappedExpressionWithSmartcastToNull, "E", whenSubjectExpression)
        }

        wrappedExpression.configure {
            +field(expression).withReplace()
        }

        wrappedDelegateExpression.configure {
            +field("delegateProvider", expression)
        }

        namedReference.configure {
            +name
            +field("candidateSymbol", firBasedSymbolType, "*", nullable = true)
        }

        resolvedNamedReference.configure {
            +field("resolvedSymbol", firBasedSymbolType, "*")
        }

        resolvedCallableReference.configure {
            +fieldList("inferredTypeArguments", coneKotlinTypeType)
            +field("mappedArguments", callableReferenceMappedArgumentsType)
        }

        delegateFieldReference.configure {
            +field("resolvedSymbol", delegateFieldSymbolType)
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
            +field("boundSymbol", firBasedSymbolType, "*", nullable = true, withReplace = true)
        }

        typeRef.configure {
            +annotations
        }

        resolvedTypeRef.configure {
            +field("type", coneKotlinTypeType)
            +field("delegatedTypeRef", typeRef, nullable = true)
        }

        typeRefWithNullability.configure {
            +booleanField("isMarkedNullable")
        }

        userTypeRef.configure {
            +fieldList("qualifier", firQualifierPartType)
            +booleanField("customRenderer")
        }

        functionTypeRef.configure {
            +field("receiverTypeRef", typeRef, nullable = true)
            +valueParameters
            +returnTypeRef
            +booleanField("isSuspend")
        }

        intersectionTypeRef.configure {
            +field("leftType", typeRef)
            +field("rightType", typeRef)
        }

        thisReceiverExpression.configure {
            +field("calleeReference", thisReference)
            +booleanField("isImplicit")
        }

        whenExpression.configure {
            +field("subject", expression, nullable = true).withTransform()
            +field("subjectVariable", variable.withArgs("E" to "*"), nullable = true)
            +fieldList("branches", whenBranch).withTransform()
            +field("exhaustivenessStatus", exhaustivenessStatusType, nullable = true, withReplace = true)
            +booleanField("usedAsExpression")
            needTransformOtherChildren()
        }

        typeProjectionWithVariance.configure {
            +field(typeRef)
            +field(varianceType)
        }

        effectDeclaration.configure {
            +field("effect", coneEffectDeclarationType)
        }

        rawContractDescription.configure {
            +fieldList("rawEffects", expression)
        }

        resolvedContractDescription.configure {
            +fieldList("effects", effectDeclaration)
            +fieldList("unresolvedEffects", statement)
        }

        legacyRawContractDescription.configure {
            +field("contractCall", functionCall)
        }
    }
}

fun Element.withArgs(vararg replacements: Pair<String, String>): AbstractElement {
    val replaceMap = replacements.toMap()
    val newArguments = typeArguments.map { replaceMap[it.name]?.let { SimpleTypeArgument(it, null) } ?: it }
    return ElementWithArguments(this, newArguments)
}
