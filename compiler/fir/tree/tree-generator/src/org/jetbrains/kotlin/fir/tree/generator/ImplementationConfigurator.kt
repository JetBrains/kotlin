/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator

import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeImplementationConfigurator
import org.jetbrains.kotlin.fir.tree.generator.model.Implementation.Kind.Object
import org.jetbrains.kotlin.fir.tree.generator.model.Implementation.Kind.OpenClass

object ImplementationConfigurator : AbstractFirTreeImplementationConfigurator() {
    fun configureImplementations() {
        configure()
        generateDefaultImplementations(FirTreeBuilder)
        configureAllImplementations()
    }

    private fun configure() = with(FirTreeBuilder) {
        impl(constructor) {
            defaultFalse("isPrimary", withGetter = true)
        }

        impl(constructor, "FirPrimaryConstructor") {
            publicImplementation()
            defaultTrue("isPrimary", withGetter = true)
        }

        impl(typeParameterRef, "FirOuterClassTypeParameterRef") {
            publicImplementation()
        }
        impl(typeParameterRef, "FirConstructedClassTypeParameterRef")

        noImpl(declarationStatus)
        noImpl(resolvedDeclarationStatus)

        impl(regularClass) {
            defaultFalse("hasLazyNestedClassifiers", withGetter = true)
        }

        impl(anonymousInitializer) {
            defaultEmptyList("annotations")
        }

        impl(anonymousObject)
        impl(danglingModifierList)
        noImpl(anonymousObjectExpression)

        impl(typeAlias)

        impl(import)

        impl(resolvedImport) {
            delegateFields(listOf("aliasName", "aliasSource", "importedFqName", "isAllUnder"), "delegate")

            default("source") {
                delegate = "delegate"
            }

            default("resolvedParentClassId") {
                delegate = "relativeParentClassName"
                delegateCall = "let { ClassId(packageFqName, it, false) }"
                withGetter = true
            }

            default("importedName") {
                delegate = "importedFqName"
                delegateCall = "shortName()"
                withGetter = true
            }

            default("delegate") {
                needAcceptAndTransform = false
            }
        }

        impl(errorImport) {
            delegateFields(listOf("aliasName", "importedFqName", "isAllUnder", "source"), "delegate")
        }

        fun ImplementationContext.commonAnnotationConfig() {
            defaultEmptyList("annotations")
            default("typeRef") {
                value = "annotationTypeRef"
                withGetter = true
            }
        }

        impl(annotation) {
            commonAnnotationConfig()
        }

        impl(annotationCall) {
            commonAnnotationConfig()
            default("argumentMapping") {
                needAcceptAndTransform = false
            }
        }

        impl(errorAnnotationCall) {
            commonAnnotationConfig()
            default("argumentMapping") {
                needAcceptAndTransform = false
            }
            default("annotationResolvePhase") {
                value = "FirAnnotationResolvePhase.Types"
            }
        }

        impl(arrayOfCall)

        impl(callableReferenceAccess)

        impl(componentCall) {
            default("calleeReference", "FirSimpleNamedReference(source, Name.identifier(\"component\$componentIndex\"))")
            useTypes(simpleNamedReferenceType, nameType)
            optInToInternals()
        }

        impl(whileLoop)

        impl(doWhileLoop)

        impl(delegatedConstructorCall) {
            default("isSuper") {
                value = "!isThis"
                withGetter = true
            }
            useTypes(explicitThisReferenceType, explicitSuperReferenceType)
        }

        impl(multiDelegatedConstructorCall) {
            default("source") {
                value = "delegatedConstructorCalls.last().source"
                withGetter = true
            }
            default("annotations") {
                value = "delegatedConstructorCalls.last().annotations"
                withGetter = true
            }
            default("argumentList") {
                value = "delegatedConstructorCalls.last().argumentList"
                withGetter = true
            }
            default("contextReceiverArguments") {
                value = "delegatedConstructorCalls.last().contextReceiverArguments"
                withGetter = true
            }
            default("constructedTypeRef") {
                value = "delegatedConstructorCalls.last().constructedTypeRef"
                withGetter = true
            }
            default("dispatchReceiver") {
                value = "delegatedConstructorCalls.last().dispatchReceiver"
                withGetter = true
            }
            default("calleeReference") {
                value = "delegatedConstructorCalls.last().calleeReference"
                withGetter = true
            }
            default("isThis") {
                value = "delegatedConstructorCalls.last().isThis"
                withGetter = true
            }
            default("isSuper") {
                value = "!isThis"
                withGetter = true
            }
            publicImplementation()
        }

        impl(delegatedConstructorCall, "FirLazyDelegatedConstructorCall") {
            val error = """error("FirLazyDelegatedConstructorCall should be calculated before accessing")"""
            default("source") {
                value = error
                withGetter = true
            }
            default("annotations") {
                value = error
                withGetter = true
            }
            default("argumentList") {
                value = error
                withGetter = true
            }
            default("contextReceiverArguments") {
                value = error
                withGetter = true
            }
            default("dispatchReceiver") {
                value = error
                withGetter = true
            }
            default("isSuper") {
                value = "!isThis"
                withGetter = true
            }
            publicImplementation()
        }

        impl(expression, "FirElseIfTrueCondition") {
            defaultTypeRefWithSource("FirImplicitBooleanTypeRef")
            useTypes(implicitBooleanTypeRefType)
            publicImplementation()
        }

        impl(block)

        val emptyExpressionBlock = impl(block, "FirEmptyExpressionBlock") {
            noSource()
            defaultEmptyList("statements")
            defaultEmptyList("annotations")
            publicImplementation()
        }

        impl(lazyBlock) {
            val error = """error("FirLazyBlock should be calculated before accessing")"""
            default("source") {
                value = error
                withGetter = true
            }
            default("statements") {
                value = error
                withGetter = true
            }
            default("annotations") {
                value = error
                withGetter = true
            }
            default("typeRef") {
                value = error
                withGetter = true
            }
        }

        impl(errorLoop) {
            default("block", "FirEmptyExpressionBlock()")
            default("condition", "FirErrorExpressionImpl(source, MutableOrEmptyList.empty(), ConeStubDiagnostic(diagnostic), null, null)")
            useTypes(emptyExpressionBlock, coneStubDiagnosticType)
        }

        impl(expression, "FirExpressionStub") {
            publicImplementation()
        }

        impl(lazyExpression) {
            val error = """error("FirLazyExpression should be calculated before accessing")"""
            default("typeRef") {
                value = error
                withGetter = true
            }
            default("annotations") {
                value = error
                withGetter = true
            }
        }

        impl(functionCall) {
            kind = OpenClass
        }

        impl(implicitInvokeCall) {
            default("origin", "FirFunctionCallOrigin.Operator")
        }

        impl(componentCall) {
            default("origin", "FirFunctionCallOrigin.Operator")
        }

        impl(propertyAccessExpression) {
            publicImplementation()
        }

        impl(getClassCall) {
            default("argument") {
                value = "argumentList.arguments.first()"
                withGetter = true
            }
        }

        noImpl(errorTypeRef)

        impl(property) {
            default("isVal") {
                value = "!isVar"
                withGetter = true
            }

            useTypes(backingFieldSymbolType, delegateFieldSymbolType)
        }

        impl(errorProperty) {
            defaultTrue("isVal", withGetter = true)
            defaultFalse("isVar", withGetter = true)

            defaultNull(
                "receiverParameter",
                "initializer",
                "delegate",
                "getter", "setter",
                withGetter = true
            )
            default("returnTypeRef", "FirErrorTypeRefImpl(null, null, diagnostic, false)")
            useTypes(errorTypeRefImplType)
        }

        impl(field) {
            default("isVal") {
                value = "!isVar"
                withGetter = true
            }
            publicImplementation()

            defaultNull("receiverParameter", "delegate", "getter", "setter", withGetter = true)
        }

        impl(enumEntry) {
            defaultTrue("isVal", withGetter = true)
            defaultFalse("isVar", withGetter = true)
            defaultNull("receiverParameter", "delegate", "getter", "setter", withGetter = true)
        }

        impl(namedArgumentExpression) {
            default("typeRef") {
                delegate = "expression"
            }
        }

        impl(lambdaArgumentExpression) {
            default("isSpread") {
                value = "false"
                withGetter = true
            }
            default("typeRef") {
                delegate = "expression"
            }
        }

        impl(spreadArgumentExpression) {
            default("isSpread") {
                value = "true"
                withGetter = true
            }
            default("typeRef") {
                delegate = "expression"
            }
        }

        impl(comparisonExpression) {
            default("typeRef", "FirImplicitBooleanTypeRef(null)")
            useTypes(implicitBooleanTypeRefType)
        }

        impl(typeOperatorCall)

        impl(assignmentOperatorStatement)

        impl(incrementDecrementExpression)

        impl(equalityOperatorCall) {
            default("typeRef", "FirImplicitBooleanTypeRef(null)")
            useTypes(implicitBooleanTypeRefType)
        }

        impl(resolvedQualifier) {
            isMutable("packageFqName", "relativeClassFqName", "isNullableLHSForCallableReference")
            defaultClassIdFromRelativeClassName()
        }

        impl(resolvedReifiedParameterReference)

        impl(returnExpression) {
            defaultTypeRefWithSource("FirImplicitNothingTypeRef")
            useTypes(implicitNothingTypeRefType)
        }

        impl(stringConcatenationCall) {
            defaultTypeRefWithSource("FirImplicitStringTypeRef")
            useTypes(implicitStringTypeRefType)
        }

        impl(throwExpression) {
            defaultTypeRefWithSource("FirImplicitNothingTypeRef")
            useTypes(implicitNothingTypeRefType)
        }

        impl(thisReceiverExpression) {
            defaultNoReceivers()
        }

        impl(expression, "FirUnitExpression") {
            defaultTypeRefWithSource("FirImplicitUnitTypeRef")
            useTypes(implicitUnitTypeRefType)
            publicImplementation()
        }

        impl(anonymousFunction) {
        }

        noImpl(anonymousFunctionExpression)

        impl(propertyAccessor) {
            default("receiverParameter") {
                value = "null"
                withGetter = true
            }
            default("isSetter") {
                value = "!isGetter"
                withGetter = true
            }
            useTypes(modalityType)
            kind = OpenClass
        }

        impl(backingField) {
            kind = OpenClass
        }

        impl(whenSubjectExpression) {
            default("typeRef") {
                value = "whenRef.value.subject!!.typeRef"
                withGetter = true
            }
            useTypes(whenExpression)
        }

        impl(desugaredAssignmentValueReferenceExpression) {
            useTypes(expression)
        }

        impl(wrappedDelegateExpression) {
            default("typeRef") {
                delegate = "expression"
            }
        }

        impl(enumEntryDeserializedAccessExpression) {
            noSource()
            default("typeRef") {
                value = "buildResolvedTypeRef { type = enumClassId.toLookupTag().constructClassType(emptyArray(), false) }"
                useTypes(buildResolvedTypeRefImport, toLookupTagImport, constructClassTypeImport)
            }
        }

        impl(smartCastExpression) {
            default("isStable") {
                value = "smartcastStability == SmartcastStability.STABLE_VALUE"
                withGetter = true
            }
        }

        impl(resolvedNamedReference)

        impl(resolvedNamedReference, "FirPropertyFromParameterResolvedNamedReference") {
            publicImplementation()
        }

        impl(resolvedErrorReference)

        impl(resolvedCallableReference)

        impl(namedReference, "FirSimpleNamedReference") {
            publicImplementation()
        }

        noImpl(namedReferenceWithCandidateBase)

        impl(delegateFieldReference) {
            default("name") {
                value = "Name.identifier(\"\\\$delegate\")"
                withGetter = true
            }
        }

        impl(backingFieldReference) {
            default("name") {
                value = "Name.identifier(\"\\\$field\")"
                withGetter = true
            }
        }

        impl(thisReference, "FirExplicitThisReference") {
            default("boundSymbol") {
                value = "null"
                isMutable = true
            }
            defaultFalse("isImplicit")
        }

        impl(thisReference, "FirImplicitThisReference") {
            noSource()
            default("labelName") {
                value = "null"
                withGetter = true
            }
            default("boundSymbol") {
                isMutable = false
            }
            defaultTrue("isImplicit")
        }

        impl(superReference, "FirExplicitSuperReference")

        noImpl(controlFlowGraphReference)

        impl(resolvedTypeRef) {
            publicImplementation()
            default("delegatedTypeRef") {
                needAcceptAndTransform = false
            }
        }

        impl(errorExpression) {
            default("typeRef", "FirErrorTypeRefImpl(source, null, ConeStubDiagnostic(diagnostic), false)")
            useTypes(errorTypeRefImplType, coneStubDiagnosticType)
        }

        impl(qualifiedErrorAccessExpression) {
            default("typeRef", "FirErrorTypeRefImpl(source, null, ConeStubDiagnostic(diagnostic), false)")
            useTypes(errorTypeRefImplType, coneStubDiagnosticType)
        }

        impl(errorFunction) {
            defaultNull("receiverParameter", "body", withGetter = true)
            default("returnTypeRef", "FirErrorTypeRefImpl(null, null, diagnostic, false)")
            useTypes(errorTypeRefImplType)
        }

        impl(functionTypeRef)
        impl(implicitTypeRef) {
            defaultEmptyList("annotations")
            default("source") {
                notNull = true
            }
        }

        impl(reference, "FirStubReference") {
            default("source") {
                value = "null"
                withGetter = true
            }
            kind = Object
        }

        impl(errorNamedReference) {
            default("name", "Name.special(\"<\${diagnostic.reason}>\")")
        }

        impl(fromMissingDependenciesNamedReference)

        impl(breakExpression) {
            defaultTypeRefWithSource("FirImplicitNothingTypeRef")
            useTypes(implicitNothingTypeRefType)
        }

        impl(continueExpression) {
            defaultTypeRefWithSource("FirImplicitNothingTypeRef")
            useTypes(implicitNothingTypeRefType)
        }

        impl(valueParameter) {
            defaultTrue("isVal", withGetter = true)
            defaultFalse("isVar", withGetter = true)
            defaultNull("getter", "setter", "initializer", "delegate", "receiverParameter", withGetter = true)
        }

        impl(valueParameter, "FirDefaultSetterValueParameter") {
            default("name", "Name.identifier(\"value\")")
        }

        impl(simpleFunction)

        impl(safeCallExpression) {
            useTypes(checkedSafeCallSubject)
        }

        impl(checkedSafeCallSubject) {
            useTypes(expression)
        }

        impl(resolvedQualifier) {
            // Initialize the value to true if only the companion object is present. This makes a standalone class reference expression
            // correctly resolve to the companion object. For example
            // ```
            // class A {
            //   companion object
            // }
            //
            // val companionOfA = A // This standalone class reference `A` here should resolve to the companion object.
            // ```
            //
            // If this `FirResolvedQualifier` is a receiver expression of some other qualified access, the value is updated in
            // `FirCallResolver` according to the resolution result.
            default("resolvedToCompanionObject", "(symbol?.fir as? FirRegularClass)?.companionObjectSymbol != null")
            useTypes(regularClass)
        }

        impl(errorResolvedQualifier) {
            defaultFalse("resolvedToCompanionObject", withGetter = true)
            defaultClassIdFromRelativeClassName()
        }

        noImpl(userTypeRef)

        impl(file) {
            default("annotations") {
                value = "annotationsContainer.annotations"
                withGetter = true
            }
        }

        noImpl(argumentList)
        noImpl(annotationArgumentMapping)

        impl(contractElementDeclaration)

        val implementationsWithoutStatusAndTypeParameters = listOf(
            "FirValueParameterImpl",
            "FirDefaultSetterValueParameter",
            "FirErrorPropertyImpl",
            "FirErrorFunctionImpl"
        )

        configureFieldInAllImplementations(
            "status",
            implementationPredicate = { it.type in implementationsWithoutStatusAndTypeParameters }
        ) {
            default(it, "FirResolvedDeclarationStatusImpl.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS")
            useTypes(resolvedDeclarationStatusImplType)
        }

        configureFieldInAllImplementations(
            "typeParameters",
            implementationPredicate = { it.type in implementationsWithoutStatusAndTypeParameters }
        ) {
            defaultEmptyList(it)
            useTypes(resolvedDeclarationStatusImplType)
        }
    }

    private fun configureAllImplementations() {
        configureFieldInAllImplementations(
            field = "controlFlowGraphReference",
            implementationPredicate = { it.type != "FirAnonymousFunctionImpl" }
        ) {
            defaultNull(it)
        }

        val implementationWithConfigurableTypeRef = listOf(
            "FirTypeProjectionWithVarianceImpl",
            "FirCallableReferenceAccessImpl",
            "FirThisReceiverExpressionImpl",
            "FirPropertyAccessExpressionImpl",
            "FirFunctionCallImpl",
            "FirAnonymousFunctionImpl",
            "FirWhenExpressionImpl",
            "FirTryExpressionImpl",
            "FirCheckNotNullCallImpl",
            "FirResolvedQualifierImpl",
            "FirResolvedReifiedParameterReferenceImpl",
            "FirExpressionStub",
            "FirVarargArgumentsExpressionImpl",
            "FirSafeCallExpressionImpl",
            "FirCheckedSafeCallSubjectImpl",
            "FirArrayOfCallImpl",
            "FirIntegerLiteralOperatorCallImpl",
            "FirContextReceiverImpl",
            "FirReceiverParameterImpl",
            "FirClassReferenceExpressionImpl",
            "FirGetClassCallImpl",
            "FirSmartCastExpressionImpl",
            "FirInaccessibleReceiverExpressionImpl"
        )
        configureFieldInAllImplementations(
            field = "typeRef",
            implementationPredicate = { it.type !in implementationWithConfigurableTypeRef },
            fieldPredicate = { it.defaultValueInImplementation == null }
        ) {
            default(it, "FirImplicitTypeRefImplWithoutSource")
            useTypes(firImplicitTypeWithoutSourceType)
        }

        configureFieldInAllImplementations(
            field = "lValueTypeRef",
            implementationPredicate = { it.type in "FirVariableAssignmentImpl" },
            fieldPredicate = { it.defaultValueInImplementation == null }
        ) {
            default(it, "FirImplicitTypeRefImplWithoutSource")
            useTypes(firImplicitTypeWithoutSourceType)
        }
    }

    private fun ImplementationContext.defaultClassIdFromRelativeClassName() {
        default("classId") {
            value = """
                |relativeClassFqName?.let {
                |    ClassId(packageFqName, it, false)
                |}
                """.trimMargin()
            withGetter = true
        }
    }
}
