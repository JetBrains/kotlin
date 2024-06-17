/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.declaredSymbol
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.effectiveVisibility
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.initializer
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.modality
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.name
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.receivers
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.referencedSymbol
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.returnTypeRef
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.scopeProvider
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.smartcastStability
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.status
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.superTypeRefs
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.typeArguments
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.typeParameterRefs
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.typeParameters
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.typeRefField
import org.jetbrains.kotlin.fir.tree.generator.FieldSets.visibility
import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFieldConfigurator
import org.jetbrains.kotlin.fir.tree.generator.context.type
import org.jetbrains.kotlin.fir.tree.generator.model.*
import org.jetbrains.kotlin.generators.tree.AbstractField
import org.jetbrains.kotlin.generators.tree.StandardTypes
import org.jetbrains.kotlin.generators.tree.TypeRef
import org.jetbrains.kotlin.generators.tree.imports.ArbitraryImportable
import org.jetbrains.kotlin.generators.tree.withArgs
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

object NodeConfigurator : AbstractFieldConfigurator<FirTreeBuilder>(FirTreeBuilder) {
    fun configureFields() = configure {
        baseFirElement.configure {
            +field("source", sourceElementType, nullable = true)
        }

        annotationContainer.configure {
            +annotations
        }

        typeParameterRef.configure {
            +referencedSymbol(typeParameterSymbolType)
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

        contextReceiver.configure {
            +field(typeRef, withReplace = true).withTransform()
            +field("customLabelName", nameType, nullable = true)
            +field("labelNameFromTypeRef", nameType, nullable = true)
        }

        elementWithResolveState.configure {
            +field("resolvePhase", resolvePhaseType).apply { isParameter = true; }
            +field("resolveState", resolveStateType).apply {
                isMutable = true; isVolatile = true; isFinal = true;
                implementationDefaultStrategy = AbstractField.ImplementationDefaultStrategy.Lateinit
                customInitializationCall = "resolvePhase.asResolveState()"
                arbitraryImportables += phaseAsResolveStateExtentionImport
                optInAnnotation = resolveStateAccessAnnotation
            }

            +field("moduleData", firModuleDataType)
            shouldBeAbstractClass()
        }

        declaration.configure {
            +declaredSymbol(firBasedSymbolType.withArgs(declaration))
            +field("moduleData", firModuleDataType)
            +field("origin", declarationOriginType)
            +field("attributes", declarationAttributesType)
            shouldBeAbstractClass()
        }

        callableDeclaration.configure {
            +field("returnTypeRef", typeRef, withReplace = true).withTransform()
            +field("receiverParameter", receiverParameter, nullable = true, withReplace = true).withTransform()
            +field("deprecationsProvider", deprecationsProviderType).withReplace().apply { isMutable = true }
            +referencedSymbol(callableSymbolType.withArgs(callableDeclaration))

            +field("containerSource", type<DeserializedContainerSource>(), nullable = true)
            +field("dispatchReceiverType", coneSimpleKotlinTypeType, nullable = true)

            +fieldList(contextReceiver, useMutableOrEmpty = true, withReplace = true)
        }

        function.configure {
            +declaredSymbol(functionSymbolType.withArgs(function))
            +fieldList(valueParameter, withReplace = true).withTransform()
            +body(nullable = true, withReplace = true).withTransform()
        }

        errorExpression.configure {
            +field("expression", expression, nullable = true)
            +field("nonExpressionElement", baseFirElement, nullable = true)
        }

        errorFunction.configure {
            +declaredSymbol(errorFunctionSymbolType)
        }

        memberDeclaration.configure {
            +status.withTransform().withReplace()
        }

        expression.configure {
            +field("coneTypeOrNull", coneKotlinTypeType, nullable = true, withReplace = true).apply {
                optInAnnotation = unresolvedExpressionTypeAccessAnnotation
            }
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
            needTransformOtherChildren()
        }

        binaryLogicExpression.configure {
            +field("leftOperand", expression).withTransform()
            +field("rightOperand", expression).withTransform()
            +field("kind", operationKindType)
            needTransformOtherChildren()
        }

        jump.configure {
            val e = withArg("E", targetElement)
            +field("target", jumpTargetType.withArgs(e))
        }

        loopJump.configure {
            parentArgs(jump, "E" to loop)
        }

        returnExpression.configure {
            parentArgs(jump, "E" to function)
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
            +field("parameter", property).withTransform()
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

        contextReceiverArgumentListOwner.configure {
            +fieldList("contextReceiverArguments", expression, useMutableOrEmpty = true, withReplace = true)
        }

        qualifiedAccessExpression.configure {
            +typeArguments.withTransform()
            +receivers
            +field("source", sourceElementType, nullable = true, withReplace = true)
            +fieldList("nonFatalDiagnostics", coneDiagnosticType, useMutableOrEmpty = true, withReplace = true)
        }

        qualifiedErrorAccessExpression.configure {
            +field("selector", errorExpression)
            +field("receiver", expression)
        }

        literalExpression.configure {
            +field("kind", constKindType, withReplace = true)
            +field("value", anyType, nullable = true)
            +field("prefix", stringType, nullable = true)
        }

        functionCall.configure {
            +field("calleeReference", namedReference)
            +field("origin", functionCallOrigin)
        }

        integerLiteralOperatorCall.configure {
            // we need methods for transformation of receivers
            +field("dispatchReceiver", expression, nullable = true, withReplace = true).withTransform()
            +field("extensionReceiver", expression, nullable = true, withReplace = true).withTransform()
        }

        comparisonExpression.configure {
            +field("operation", operationType)
            +field("compareToCall", functionCall)
        }

        typeOperatorCall.configure {
            +field("operation", operationType)
            +field("conversionTypeRef", typeRef).withTransform()
            +booleanField("argFromStubType", withReplace = true)
            needTransformOtherChildren()
        }

        augmentedAssignment.configure {
            +field("operation", operationType)
            +field("leftArgument", expression).withTransform()
            +field("rightArgument", expression).withTransform()

            element.kDoc = """
                Represents an augmented assignment statement (e.g. `x += y`) **before** it gets resolved.
                After resolution, it will be either represented as an assignment (`x = x.plus(y)`) or a call (`x.plusAssign(y)`). 
                
                Augmented assignments with an indexed access as receiver are represented as [${indexedAccessAugmentedAssignment.render()}]. 
            """.trimIndent()
        }

        incrementDecrementExpression.configure {
            +booleanField("isPrefix")
            +field("operationName", nameType)
            +field("expression", expression)
            +field("operationSource", sourceElementType, nullable = true)
        }

        equalityOperatorCall.configure {
            +field("operation", operationType)
        }

        whenBranch.configure {
            +field("condition", expression).withTransform()
            +field("result", block).withTransform()
            +booleanField("hasGuard")
            needTransformOtherChildren()
        }

        classLikeDeclaration.configure {
            +declaredSymbol(classLikeSymbolType.withArgs(classLikeDeclaration))
            +field("deprecationsProvider", deprecationsProviderType).withReplace().apply { isMutable = true }
        }

        klass.configure {
            +declaredSymbol(classSymbolType.withArgs(klass))
            +classKind
            +superTypeRefs(withReplace = true).withTransform()
            +declarations.withTransform()
            +annotations
            +scopeProvider
        }

        regularClass.configure {
            +name
            +declaredSymbol(regularClassSymbolType)
            +booleanField("hasLazyNestedClassifiers")
            +referencedSymbol("companionObjectSymbol", regularClassSymbolType, nullable = true, withReplace = true)
            +superTypeRefs(withReplace = true)
            +fieldList(contextReceiver, useMutableOrEmpty = true)
        }

        anonymousObject.configure {
            +declaredSymbol(anonymousObjectSymbolType)
        }

        anonymousObjectExpression.configure {
            +field(anonymousObject).withTransform()
        }

        typeAlias.configure {
            +typeParameters
            +name
            +declaredSymbol(typeAliasSymbolType)
            +field("expandedTypeRef", typeRef, withReplace = true).withTransform()
            +annotations
        }

        anonymousFunction.configure {
            +declaredSymbol(anonymousFunctionSymbolType)
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
            +booleanField("isTrailingLambda", withReplace = true).apply {
                replaceOptInAnnotation = rawFirApi
            }
        }

        typeParameter.configure {
            +name
            +declaredSymbol(typeParameterSymbolType)
            +referencedSymbol("containingDeclarationSymbol", firBasedSymbolType.withArgs(TypeRef.Star)).apply {
                withBindThis = false
            }
            +field(varianceType)
            +booleanField("isReified")
            // TODO: `useMutableOrEmpty = true` is a workaround for KT-60324 until KT-60445 has been fixed.
            +fieldList("bounds", typeRef, withReplace = true, useMutableOrEmpty = true)
            +annotations
        }

        simpleFunction.configure {
            +name
            +declaredSymbol(namedFunctionSymbolType)
            +annotations
            +typeParameters
        }

        contractDescriptionOwner.configure {
            +field(contractDescription, withReplace = true, nullable = true).withTransform()
        }

        property.configure {
            +fieldList(contextReceiver, useMutableOrEmpty = true, withReplace = true).withTransform()
            +declaredSymbol(propertySymbolType)
            +referencedSymbol("delegateFieldSymbol", delegateFieldSymbolType, nullable = true)
            +booleanField("isLocal")
            +field("bodyResolveState", propertyBodyResolveStateType, withReplace = true)
            +typeParameters
        }

        propertyAccessor.configure {
            +declaredSymbol(propertyAccessorSymbolType)
            +referencedSymbol("propertySymbol", firPropertySymbolType).apply {
                withBindThis = false
            }
            +booleanField("isGetter")
            +booleanField("isSetter")
            +annotations
            +typeParameters
        }

        backingField.configure {
            +declaredSymbol(backingFieldSymbolType)
            +referencedSymbol("propertySymbol", firPropertySymbolType).apply {
                withBindThis = false
            }
            +initializer.withTransform().withReplace()
            +annotations
            +typeParameters
            +status.withTransform()
        }

        declarationStatus.configure {
            +visibility
            +modality(nullable = true)
            generateBooleanFields(
                "expect", "actual", "override", "operator", "infix", "inline", "tailRec",
                "external", "const", "lateInit", "inner", "companion", "data", "suspend", "static",
                "fromSealedClass", "fromEnumClass", "fun", "hasStableParameterNames",
            )
            +field("defaultVisibility", visibilityType, nullable = false)
            +field("defaultModality", modalityType, nullable = false)
        }

        resolvedDeclarationStatus.configure {
            +modality(nullable = false)
            +effectiveVisibility
            shouldBeAnInterface()
        }

        implicitInvokeCall.configure {
            +booleanField("isCallWithExplicitReceiver")
        }

        constructor.configure {
            +annotations
            +declaredSymbol(constructorSymbolType)
            +field("delegatedConstructor", delegatedConstructorCall, nullable = true, withReplace = true).withTransform()
            +body(nullable = true)
            +booleanField("isPrimary")
        }

        delegatedConstructorCall.configure {
            +field("constructedTypeRef", typeRef, withReplace = true)
            +field("dispatchReceiver", expression, nullable = true, withReplace = true).withTransform()
            +field("calleeReference", reference, withReplace = true)
            +field("source", sourceElementType, nullable = true, withReplace = true)
            generateBooleanFields("this", "super")
        }

        multiDelegatedConstructorCall.configure {
            +fieldList("delegatedConstructorCalls", delegatedConstructorCall, withReplace = true).withTransform()
        }

        valueParameter.configure {
            +declaredSymbol(valueParameterSymbolType)
            +field("defaultValue", expression, nullable = true, withReplace = true)
            +referencedSymbol("containingFunctionSymbol", functionSymbolType.withArgs(TypeRef.Star)).apply {
                withBindThis = false
            }
            generateBooleanFields("crossinline", "noinline", "vararg")
        }

        receiverParameter.configure {
            +typeRefField.withTransform()
            +annotations
        }

        scriptReceiverParameter.configure {
            +typeRefField.withTransform()
            +booleanField("isBaseClassReceiver") // means coming from ScriptCompilationConfigurationKeys.baseClass (could be deprecated soon, see KT-68540)
        }

        variable.configure {
            +name
            +declaredSymbol(variableSymbolType.withArgs(variable))
            +initializer.withTransform().withReplace()
            +field("delegate", expression, nullable = true, withReplace = true).withTransform()
            generateBooleanFields("var", "val")
            +field("getter", propertyAccessor, nullable = true, withReplace = true).withTransform()
            +field("setter", propertyAccessor, nullable = true, withReplace = true).withTransform()
            +field("backingField", backingField, nullable = true).withTransform()
            +annotations
            needTransformOtherChildren()
        }

        functionTypeParameter.configure {
            +field("name", nameType, nullable = true)
            +field("returnTypeRef", typeRef)
        }

        errorProperty.configure {
            +declaredSymbol(errorPropertySymbolType)
        }

        enumEntry.configure {
            +declaredSymbol(enumEntrySymbolType)
        }

        field.configure {
            +declaredSymbol(fieldSymbolType)
            generateBooleanFields("hasConstantInitializer")
        }

        anonymousInitializer.configure {
            +body(nullable = true, withReplace = true)
            +declaredSymbol(anonymousInitializerSymbolType)
            // the containing declaration is nullable, because it is not immediately clear how to obtain it in all places in the fir builder
            // TODO: review and consider making not-nullable (KT-64195)
            +referencedSymbol("containingDeclarationSymbol", firBasedSymbolType.withArgs(TypeRef.Star), nullable = true).apply {
                withBindThis = false
            }
        }

        danglingModifierList.configure {
            +declaredSymbol(danglingModifierSymbolType)
        }

        file.configure {
            +field("packageDirective", packageDirective)
            +fieldList(import).withTransform()
            +declarations.withTransform()
            +stringField("name")
            +field("sourceFile", sourceFileType, nullable = true)
            +field("sourceFileLinesMapping", sourceFileLinesMappingType, nullable = true)
            +declaredSymbol(fileSymbolType)
        }

        script.configure {
            +name
            +declarations.withTransform().withReplace()
            +declaredSymbol(scriptSymbolType)
            +fieldList("parameters", property).withTransform()
            +fieldList("receivers", scriptReceiverParameter, useMutableOrEmpty = true).withTransform()
            +field("resultPropertyName", nameType, nullable = true)
        }

        codeFragment.configure {
            +declaredSymbol(codeFragmentSymbolType)
            +field(block, withReplace = true).withTransform()
        }

        packageDirective.configure {
            +field("packageFqName", fqNameType)
        }

        import.configure {
            +field("importedFqName", fqNameType, nullable = true)
            +booleanField("isAllUnder")
            +field("aliasName", nameType, nullable = true)
            +field("aliasSource", sourceElementType, nullable = true)
            shouldBeAbstractClass()
        }

        resolvedImport.configure {
            +field("delegate", import, isChild = false)
            +field("packageFqName", fqNameType)
            +field("relativeParentClassName", fqNameType, nullable = true)
            +field("resolvedParentClassId", classIdType, nullable = true)
            +field(
                "importedName",
                nameType,
                nullable = true
            )
        }

        annotation.configure {
            +field("useSiteTarget", annotationUseSiteTargetType, nullable = true, withReplace = true)
            +field("annotationTypeRef", typeRef, withReplace = true).withTransform()
            +field("argumentMapping", annotationArgumentMapping, withReplace = true)
            +typeArguments.withTransform()
        }

        annotationCall.configure {
            +field("argumentMapping", annotationArgumentMapping, withReplace = true, isChild = false)
            +field("annotationResolvePhase", annotationResolvePhaseType, withReplace = true)
            +referencedSymbol("containingDeclarationSymbol", firBasedSymbolType.withArgs(TypeRef.Star)).apply {
                withBindThis = false
            }
        }

        errorAnnotationCall.configure {
            +field("argumentMapping", annotationArgumentMapping, withReplace = true, isChild = false)
        }

        annotationArgumentMapping.configure {
            +field("mapping", StandardTypes.map.withArgs(nameType, expression))
        }

        indexedAccessAugmentedAssignment.configure {
            +field("lhsGetCall", functionCall)
            +field("rhs", expression)
            +field("operation", operationType)
            // Used for resolution errors reporting in case
            +field("calleeReference", reference, withReplace = true)
            +field("arrayAccessSource", sourceElementType, nullable = true)

            element.kDoc = """
                    Represents an augmented assignment with an indexed access as the receiver (e.g., `arr[i] += 1`)
                    **before** it gets resolved.
                    
                    After resolution, the call will be desugared into regular function calls,
                    either of the form `arr.set(i, arr.get(i).plus(1))` or `arr.get(i).plusAssign(1)`.
                """.trimIndent()
        }

        classReferenceExpression.configure {
            +field("classTypeRef", typeRef)
        }

        componentCall.configure {
            +field("explicitReceiver", expression)
            +intField("componentIndex")
        }

        smartCastExpression.configure {
            +field("originalExpression", expression, withReplace = true).withTransform()
            +field("typesFromSmartCast", StandardTypes.collection.withArgs(coneKotlinTypeType))
            +field("smartcastType", typeRef)
            +field("smartcastTypeWithoutNullableNothing", typeRef, nullable = true)
            +booleanField("isStable")
            +smartcastStability
        }

        safeCallExpression.configure {
            +field("receiver", expression).withTransform()
            // Special node that might be used as a reference to receiver of a safe call after null check
            +field("checkedSubjectRef", safeCallCheckedSubjectReferenceType)
            // One that uses checkedReceiver as a receiver
            +field("selector", statement, withReplace = true).withTransform()
        }

        checkedSafeCallSubject.configure {
            +field("originalReceiverRef", referenceToSimpleExpressionType)
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

        spreadArgumentExpression.configure {
            +booleanField("isNamed")
            +booleanField("isFakeSpread")

            element.kDoc = """
                |### Up to and including body resolution phase
                |
                |Represents a spread expression `*foo`. If a spread expression is passed as named argument `foo = *bar`, it will be
                |represented as an [${namedArgumentExpression.render()}] with [${namedArgumentExpression.render()}.isSpread] set to `true`.
                |  
                |### After body resolution phase
                |
                |Represents spread expressions `*foo` and named argument expressions for vararg parameters `foo = bar` and `foo = *bar`.
                |
                |If [isNamed] is `true`, it means the argument was passed in named form. The name is not saved since it's not required.
                |To retrieve the argument mapping of a call, [${firResolvedArgumentListType.render()}.mapping] must be used.
                |
                |If [isFakeSpread] is `true`, it means this expression is the argument to a `vararg` parameter that was passed in named form
                |without a spread operator `*`.
                |
                |The information carried by [isNamed] and [isFakeSpread] is only relevant for some checkers. Otherwise,
                |[FirSpreadArgumentExpression]s should be treated uniformly since they always represent an array that was passed to a
                |`vararg` parameter and don't influence the resulting platform code.
            """.trimMargin()
        }

        namedArgumentExpression.configure {
            +name

            element.kDoc = """
                |Represents a named argument `foo = bar` before and during body resolution phase.
                |
                |After body resolution, all [${namedArgumentExpression.render()}]s are removed from the FIR tree and the argument mapping must be
                |retrieved from [${firResolvedArgumentListType.render()}.mapping].
                |
                |For a named argument with spread operator `foo = *bar`, [isSpread] will be set to `true` but no additional
                |[${spreadArgumentExpression.render()}] will be created as the [expression].
                |
                |**Special case vor varargs**: named arguments for `vararg` parameters are replaced with [${spreadArgumentExpression.render()}] with
                |[${spreadArgumentExpression.render()}.isNamed] set to `true`.
                |
                |See [${varargArgumentsExpression.render()}] for the general structure of arguments of `vararg` parameters after resolution.
            """.trimMargin()
        }

        varargArgumentsExpression.configure {
            +fieldList("arguments", expression)
            +field("coneElementTypeOrNull", coneKotlinTypeType, nullable = true)

            element.kDoc = """
                |[${varargArgumentsExpression.render()}]s are created during body resolution phase for arguments of `vararg` parameters.
                |
                |If one or multiple elements are passed to a `vararg` parameter, the will be wrapped with a [${varargArgumentsExpression.render()}]
                |and [arguments] will contain the individual elements.
                |
                |If a named argument is passed to a `vararg` parameter, [arguments] will contain a single [${spreadArgumentExpression.render()}]
                |with [${spreadArgumentExpression.render()}.isNamed] set to `true`.
                |
                |[${spreadArgumentExpression.render()}]s are kept as is in [arguments]. 
                |
                |If no element is passed to a `vararg` parameter, no [${varargArgumentsExpression.render()}] is created regardless of whether the
                |parameter has a default value.
            """.trimMargin()
        }

        samConversionExpression.configure {
            +field("expression", expression)
        }

        resolvedQualifier.configure {
            +field("packageFqName", fqNameType)
            +field("relativeClassFqName", fqNameType, nullable = true)
            +field("classId", classIdType, nullable = true)
            +referencedSymbol("symbol", classLikeSymbolType, nullable = true)
            +booleanField("isNullableLHSForCallableReference", withReplace = true)
            +booleanField("resolvedToCompanionObject", withReplace = true)
            +booleanField("canBeValue", withReplace = true)
            +booleanField("isFullyQualified")
            +fieldList("nonFatalDiagnostics", coneDiagnosticType, useMutableOrEmpty = true)
            +typeArguments.withTransform()
        }

        resolvedReifiedParameterReference.configure {
            +referencedSymbol(typeParameterSymbolType)
        }

        stringConcatenationCall.configure {
            +stringField("interpolationPrefix")
        }

        throwExpression.configure {
            +field("exception", expression).withTransform()
        }

        variableAssignment.configure {
            +field("lValue", expression).withTransform().withReplace()
            +field("rValue", expression).withTransform()
        }

        whenSubjectExpression.configure {
            +field("whenRef", whenRefType)
        }

        desugaredAssignmentValueReferenceExpression.configure {
            +field("expressionRef", referenceToSimpleExpressionType)
        }

        wrappedExpression.configure {
            +field(expression)
        }

        wrappedDelegateExpression.configure {
            +field("provideDelegateCall", functionCall)
        }

        enumEntryDeserializedAccessExpression.configure {
            +field("enumClassId", classIdType)
            +field("enumEntryName", nameType)
        }

        namedReference.configure {
            +name
        }

        namedReferenceWithCandidateBase.configure {
            +referencedSymbol("candidateSymbol", firBasedSymbolType.withArgs(TypeRef.Star))
        }

        resolvedNamedReference.configure {
            +referencedSymbol("resolvedSymbol", firBasedSymbolType.withArgs(TypeRef.Star))
        }

        resolvedCallableReference.configure {
            +fieldList("inferredTypeArguments", coneKotlinTypeType)
            +field("mappedArguments", callableReferenceMappedArgumentsType)
        }

        delegateFieldReference.configure {
            +referencedSymbol("resolvedSymbol", delegateFieldSymbolType)
        }

        backingFieldReference.configure {
            +referencedSymbol("resolvedSymbol", backingFieldSymbolType)
        }

        superReference.configure {
            +stringField("labelName", nullable = true)
            +field("superTypeRef", typeRef, withReplace = true)
        }

        thisReference.configure {
            +stringField("labelName", nullable = true)
            +referencedSymbol("boundSymbol", firBasedSymbolType.withArgs(TypeRef.Star), nullable = true, withReplace = true)
            +intField("contextReceiverNumber", withReplace = true)
            +booleanField("isImplicit")
            +field("diagnostic", coneDiagnosticType, nullable = true, withReplace = true)
        }

        typeRef.configure {
            +annotations
        }

        resolvedTypeRef.configure {
            +field("type", coneKotlinTypeType)
            +field("delegatedTypeRef", typeRef, nullable = true, isChild = false)
            element.otherParents.add(typeRefMarkerType)
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
            +fieldList("parameters", functionTypeParameter)
            +returnTypeRef
            +booleanField("isSuspend")

            +fieldList("contextReceiverTypeRefs", typeRef)
        }

        errorTypeRef.configure {
            +field("partiallyResolvedTypeRef", typeRef, nullable = true).withTransform()
        }

        resolvedErrorReference.configure {
            element.customParentInVisitor = resolvedNamedReference
        }

        intersectionTypeRef.configure {
            +field("leftType", typeRef)
            +field("rightType", typeRef)
        }

        thisReceiverExpression.configure {
            +field("calleeReference", thisReference)
            +booleanField("isImplicit")
        }

        inaccessibleReceiverExpression.configure {
            +field("calleeReference", thisReference)
        }

        whenExpression.configure {
            +field("subject", expression, nullable = true).withTransform()
            +field("subjectVariable", variable, nullable = true)
            +fieldList("branches", whenBranch).withTransform()
            +field("exhaustivenessStatus", exhaustivenessStatusType, nullable = true, withReplace = true)
            +booleanField("usedAsExpression")
            needTransformOtherChildren()
        }

        typeProjectionWithVariance.configure {
            +field(typeRef)
            +field(varianceType)
        }

        contractElementDeclaration.configure {
            +field("effect", coneContractElementType)
        }

        effectDeclaration.configure {
            +field("effect", coneEffectDeclarationType)
        }

        rawContractDescription.configure {
            +fieldList("rawEffects", expression)
        }

        resolvedContractDescription.configure {
            +fieldList("effects", effectDeclaration)
            +fieldList("unresolvedEffects", contractElementDeclaration)
            +field("diagnostic", coneDiagnosticType, nullable = true)
        }

        legacyRawContractDescription.configure {
            +field("contractCall", functionCall)
            +field("diagnostic", coneDiagnosticType, nullable = true)
        }
    }
}
