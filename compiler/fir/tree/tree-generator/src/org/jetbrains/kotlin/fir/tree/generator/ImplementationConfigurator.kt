/*
 * Copyright 2000-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
            defaultTrue("isPrimary", withGetter = true)
        }

        impl(typeParameterRef, "FirOuterClassTypeParameterRef")
        impl(typeParameterRef, "FirConstructedClassTypeParameterRef")

        noImpl(declarationStatus)
        noImpl(resolvedDeclarationStatus)

        val regularClassConfig: ImplementationContext.() -> Unit = {
            defaultFalse("hasLazyNestedClassifiers", withGetter = true)
        }

        impl(regularClass, "FirClassImpl", regularClassConfig)

        impl(anonymousObject)

        impl(typeAlias)

        impl(import)

        impl(resolvedImport) {
            delegateFields(listOf("aliasName", "importedFqName", "isAllUnder"), "delegate")

            default("source") {
                delegate = "delegate"
            }

            default("resolvedClassId") {
                delegate = "relativeClassName"
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

        impl(annotationCall) {
            default("typeRef") {
                value = "annotationTypeRef"
                withGetter = true
            }

            defaultFalse("resolved")
        }

        impl(arrayOfCall)

        val modifiableQualifiedAccess = impl(qualifiedAccessWithoutCallee, "FirModifiableQualifiedAccess") {
            isMutable("safe")
        }

        impl(callableReferenceAccess) {
            parents += modifiableQualifiedAccess
        }

        impl(componentCall) {
            default("safe") {
                value = "false"
                withGetter = true
            }
            default("calleeReference", "FirSimpleNamedReference(source, Name.identifier(\"component\$componentIndex\"), null)")
            useTypes(simpleNamedReferenceType, nameType)
            optInToInternals()
        }

        impl(whileLoop)

        impl(doWhileLoop)

        impl(delegatedConstructorCall) {
            default(
                "calleeReference",
                "if (isThis) FirExplicitThisReference(source, null) else FirExplicitSuperReference(source, null, constructedTypeRef)"
            )
            default("isSuper") {
                value = "!isThis"
                withGetter = true
            }
            useTypes(explicitThisReferenceType, explicitSuperReferenceType)
        }

        impl(expression, "FirElseIfTrueCondition") {
            default("typeRef", "FirImplicitBooleanTypeRef(source)")
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

        impl(errorLoop) {
            default("block", "FirEmptyExpressionBlock()")
            default("condition", "FirErrorExpressionImpl(source, ConeStubDiagnostic(diagnostic))")
            useTypes(emptyExpressionBlock, coneStubDiagnosticType)
        }

        impl(expression, "FirExpressionStub") {
            publicImplementation()
        }

        impl(functionCall) {
            parents += modifiableQualifiedAccess
            kind = OpenClass
        }

        impl(qualifiedAccessExpression) {
            parents += modifiableQualifiedAccess
        }


        noImpl(expressionWithSmartcast)

        impl(getClassCall) {
            default("argument") {
                value = "argumentList.arguments.first()"
                withGetter = true
            }
        }

        impl(property) {
            default("isVal") {
                value = "!isVar"
                withGetter = true
            }

            default("backingFieldSymbol", "FirBackingFieldSymbol(symbol.callableId)")
            useTypes(backingFieldSymbolType, delegateFieldSymbolType)
        }

        impl(field) {
            default("isVal") {
                value = "!isVar"
                withGetter = true
            }

            defaultNull("delegateFieldSymbol", "receiverTypeRef", "initializer", "delegate", "getter", "setter", withGetter = true)
        }

        impl(enumEntry) {
            defaultTrue("isVal", withGetter = true)
            defaultFalse("isVar", withGetter = true)
            defaultNull("delegateFieldSymbol", "receiverTypeRef", "delegate", "getter", "setter", withGetter = true)
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

        impl(operatorCall) {
            default("typeRef", """
                |if (operation in FirOperation.BOOLEANS) {
                |        FirImplicitBooleanTypeRef(null)
                |    } else {
                |        FirImplicitTypeRefImpl(null)
                |    }
                """.trimMargin())

            useTypes(implicitTypeRefType, implicitBooleanTypeRefType)
        }

        impl(comparisonExpression) {
            default("typeRef", "FirImplicitBooleanTypeRef(null)")
            useTypes(implicitBooleanTypeRefType)
        }

        impl(typeOperatorCall)

        impl(resolvedQualifier) {
            isMutable("packageFqName", "relativeClassFqName", "safe")
            default("classId") {
                value = """
                    |relativeClassFqName?.let {
                    |    ClassId(packageFqName, it, false)
                    |}
                """.trimMargin()
                withGetter = true
            }
        }

        impl(resolvedReifiedParameterReference)

        impl(returnExpression) {
            default("typeRef", "FirImplicitNothingTypeRef(source)")
            useTypes(implicitNothingTypeRefType)
        }

        impl(stringConcatenationCall) {
            default("typeRef", "FirImplicitStringTypeRef(source)")
            useTypes(implicitStringTypeRefType)
        }

        impl(throwExpression) {
            default("typeRef", "FirImplicitNothingTypeRef(source)")
            useTypes(implicitNothingTypeRefType)
        }

        impl(thisReceiverExpression) {
            parents += modifiableQualifiedAccess
            default("safe") {
                value = "false"
                withGetter = true
            }
            defaultNoReceivers()
        }

        impl(expression, "FirUnitExpression") {
            default("typeRef", "FirImplicitUnitTypeRef(source)")
            useTypes(implicitUnitTypeRefType)
            publicImplementation()
        }

        impl(variableAssignment) {
            parents += modifiableQualifiedAccess

            default("lValue") {
                value = "calleeReference"
                customSetter = "calleeReference = value"
            }
        }

        impl(anonymousFunction) {
            default("resolvePhase", "FirResolvePhase.DECLARATIONS")
        }

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

        impl(whenSubjectExpression) {
            default("typeRef") {
                value = "whenSubject.whenExpression.subject!!.typeRef"
                withGetter = true
            }
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

        impl(controlFlowGraphReference, "FirEmptyControlFlowGraphReference") {
            noSource()
            kind = Object
        }

        impl(resolvedTypeRef)

        val errorTypeRefImpl = impl(errorTypeRef) {
            default("type", "ConeClassErrorType(diagnostic.reason)")
            default("delegatedTypeRef") {
                value = "null"
                withGetter = true
            }
            default("annotations", "mutableListOf()")
            defaultFalse("isSuspend")
            useTypes(coneClassErrorTypeType)
        }

        impl(errorExpression) {
            defaultEmptyList("annotations")
            default("typeRef", "FirErrorTypeRefImpl(source, ConeStubDiagnostic(diagnostic))")
            useTypes(errorTypeRefImpl, coneStubDiagnosticType)
        }

        impl(resolvedFunctionTypeRef) {
            default("delegatedTypeRef") {
                value = "null"
                withGetter = true
            }
        }

        impl(errorFunction) {
            defaultNull("receiverTypeRef", "body", withGetter = true)
            default("returnTypeRef", "FirErrorTypeRefImpl(null, diagnostic)")
            useTypes(errorTypeRefImpl)
        }

        impl(functionTypeRef)
        impl(implicitTypeRef) {
            defaultEmptyList("annotations")
        }

        impl(composedSuperTypeRef)

        impl(reference, "FirStubReference") {
            default("source") {
                value = "null"
                withGetter = true
            }
            kind = Object
        }

        impl(errorNamedReference) {
            default("name", "Name.special(\"<\${diagnostic.reason}>\")")
            defaultNull("candidateSymbol", withGetter = true)
        }

        impl(typeProjection, "FirTypePlaceholderProjection") {
            kind = Object
            noSource()
        }

        impl(breakExpression) {
            default("typeRef", "FirImplicitNothingTypeRef(source)")
            useTypes(implicitNothingTypeRefType)
        }

        impl(continueExpression) {
            default("typeRef", "FirImplicitNothingTypeRef(source)")
            useTypes(implicitNothingTypeRefType)
        }

        impl(valueParameter) {
            kind = OpenClass
            defaultTrue("isVal", withGetter = true)
            defaultFalse("isVar", withGetter = true)
            defaultNull("getter", "setter", "initializer", "delegate", "receiverTypeRef", "delegateFieldSymbol", withGetter = true)
        }

        impl(valueParameter, "FirDefaultSetterValueParameter") {
            default("name", "Name.identifier(\"value\")")
        }

        impl(simpleFunction) {
            kind = OpenClass
        }

        impl(delegatedTypeRef) {
            listOf("source", "annotations").forEach {
                default(it) {
                    delegate = "typeRef"
                }
            }
        }

        noImpl(userTypeRef)
    }

    private fun configureAllImplementations() {
        configureFieldInAllImplementations(
            field = "controlFlowGraphReference",
            implementationPredicate = { it.type != "FirAnonymousFunctionImpl"}
        ) {
            default(it, "FirEmptyControlFlowGraphReference")
            useTypes(emptyCfgReferenceType)
        }

        val implementationWithConfigurableTypeRef = listOf(
            "FirDelegatedTypeRefImpl",
            "FirTypeProjectionWithVarianceImpl",
            "FirCallableReferenceAccessImpl",
            "FirThisReceiverExpressionImpl",
            "FirAnonymousObjectImpl",
            "FirQualifiedAccessExpressionImpl",
            "FirFunctionCallImpl",
            "FirAnonymousFunctionImpl",
            "FirWhenExpressionImpl",
            "FirTryExpressionImpl",
            "FirCheckNotNullCallImpl",
            "FirResolvedQualifierImpl",
            "FirResolvedReifiedParameterReferenceImpl",
            "FirExpressionStub",
            "FirVarargArgumentsExpressionImpl",
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
            field = "attributes",
            fieldPredicate = { it.type == declarationAttributesType.type }
        ) {
            default(it, "${declarationAttributesType.type}()")
        }
    }
}
