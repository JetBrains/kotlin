/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

        impl(arrayOfCall)

        impl(callableReferenceAccess)

        impl(componentCall) {
            default("calleeReference", "FirSimpleNamedReference(source, Name.identifier(\"component\$componentIndex\"), null)")
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

        impl(block, "FirLazyBlock") {
            val error = """error("FirLazyBlock should be calculated before accessing")"""
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
            publicImplementation()
        }

        impl(errorLoop) {
            default("block", "FirEmptyExpressionBlock()")
            default("condition", "FirErrorExpressionImpl(source, mutableListOf(), ConeStubDiagnostic(diagnostic), null)")
            useTypes(emptyExpressionBlock, coneStubDiagnosticType)
        }

        impl(expression, "FirExpressionStub") {
            publicImplementation()
        }

        impl(expression, "FirLazyExpression") {
            val error = """error("FirLazyExpression should be calculated before accessing")"""
            default("typeRef") {
                value = error
                withGetter = true
            }
            default("annotations") {
                value = error
                withGetter = true
            }
            publicImplementation()
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

        noImpl(expressionWithSmartcast)
        noImpl(expressionWithSmartcastToNull)

        noImpl(whenSubjectExpressionWithSmartcast)
        noImpl(whenSubjectExpressionWithSmartcastToNull)

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
                "receiverTypeRef",
                "initializer",
                "delegate",
                "getter", "setter",
                withGetter = true
            )
            default("returnTypeRef", "FirErrorTypeRefImpl(null, null, diagnostic)")
            useTypes(errorTypeRefImplType)
        }

        impl(field) {
            default("isVal") {
                value = "!isVar"
                withGetter = true
            }
            publicImplementation()

            defaultNull("receiverTypeRef", "delegate", "getter", "setter", withGetter = true)
        }

        impl(enumEntry) {
            defaultTrue("isVal", withGetter = true)
            defaultFalse("isVar", withGetter = true)
            defaultNull("receiverTypeRef", "delegate", "getter", "setter", withGetter = true)
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

        impl(variableAssignment) {
            default("lValue") {
                value = "calleeReference"
                customSetter = "calleeReference = value"
            }
        }

        impl(anonymousFunction) {
            default("resolvePhase", "FirResolvePhase.DECLARATIONS")
        }

        noImpl(anonymousFunctionExpression)

        impl(propertyAccessor) {
            default("receiverTypeRef") {
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
            useTypes(whenExpressionType)
        }

        impl(wrappedDelegateExpression) {
            default("typeRef") {
                delegate = "expression"
            }
        }

        impl(resolvedNamedReference) {
            defaultNull("candidateSymbol", withGetter = true)
        }

        impl(resolvedNamedReference, "FirPropertyFromParameterResolvedNamedReference") {
            defaultNull("candidateSymbol", withGetter = true)
            publicImplementation()
        }

        impl(resolvedCallableReference) {
            defaultNull("candidateSymbol", withGetter = true)
        }

        impl(namedReference, "FirSimpleNamedReference") {
            kind = OpenClass
        }

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
            default("typeRef", "FirErrorTypeRefImpl(source, null, ConeStubDiagnostic(diagnostic))")
            useTypes(errorTypeRefImplType, coneStubDiagnosticType)
        }

        impl(errorFunction) {
            defaultNull("receiverTypeRef", "body", withGetter = true)
            default("returnTypeRef", "FirErrorTypeRefImpl(null, null, diagnostic)")
            useTypes(errorTypeRefImplType)
        }

        impl(functionTypeRef)
        impl(implicitTypeRef) {
            defaultEmptyList("annotations")
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
            defaultNull("getter", "setter", "initializer", "delegate", "receiverTypeRef", withGetter = true)
        }

        impl(valueParameter, "FirDefaultSetterValueParameter") {
            default("name", "Name.identifier(\"value\")")
        }

        impl(simpleFunction)

        impl(safeCallExpression) {
            useTypes(safeCallCheckedSubjectType)
        }

        impl(checkedSafeCallSubject) {
            useTypes(expressionType)
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
            default("symbol", "FirFileSymbol()")
        }

        noImpl(argumentList)
        noImpl(annotationArgumentMapping)

        val implementationsWithoutStatusAndTypeParameters = listOf(
            "FirAnonymousFunctionImpl",
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
            implementationPredicate = { it.type != "FirAnonymousFunctionImpl" && it.type in implementationsWithoutStatusAndTypeParameters }
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
            "FirClassReferenceExpressionImpl",
            "FirGetClassCallImpl"
        )
        configureFieldInAllImplementations(
            field = "typeRef",
            implementationPredicate = { it.type !in implementationWithConfigurableTypeRef },
            fieldPredicate = { it.defaultValueInImplementation == null }
        ) {
            default(it, "FirImplicitTypeRefImpl(null)")
            useTypes(implicitTypeRefType)
        }

        configureFieldInAllImplementations(
            field = "lValueTypeRef",
            implementationPredicate = { it.type in "FirVariableAssignmentImpl" },
            fieldPredicate = { it.defaultValueInImplementation == null }
        ) {
            default(it, "FirImplicitTypeRefImpl(null)")
            useTypes(implicitTypeRefType)
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


