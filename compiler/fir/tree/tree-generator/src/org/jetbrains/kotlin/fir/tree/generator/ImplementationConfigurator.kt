/*
 * Copyright 2000-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator

import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeImplementationConfigurator
import org.jetbrains.kotlin.fir.tree.generator.model.Implementation
import org.jetbrains.kotlin.fir.tree.generator.model.Implementation.Kind.Object
import org.jetbrains.kotlin.fir.tree.generator.model.Implementation.Kind.OpenClass

object ImplementationConfigurator : AbstractFirTreeImplementationConfigurator() {
    private lateinit var abstractAnnotatedElement: Implementation

    fun configureImplementations() {
        configure()
        generateDefaultImplementations(FirTreeBuilder)
        configureAllImplementations()
    }

    private fun configure() = with(FirTreeBuilder) {
        val callWithArgumentList = impl(call, "FirCallWithArgumentList")

        abstractAnnotatedElement = impl(annotationContainer, "FirAbstractAnnotatedElement")

        val modifiableTypeParametersOwner = impl(typeParametersOwner, "FirModifiableTypeParametersOwner")

        val modifiableConstructor = impl(constructor, "FirModifiableConstructor") {
            parents += modifiableTypeParametersOwner
        }

        impl(constructor) {
            kind = OpenClass
            parents += modifiableConstructor
            defaultNull("delegatedConstructor")
            defaultNull("body")
            default("name", "Name.special(\"<init>\")")

            default("isPrimary") {
                value = "false"
                withGetter = true
            }

            default("typeParameters") {
                needAcceptAndTransform = false
            }
        }

        noImpl(declarationStatus)
        noImpl(resolvedDeclarationStatus)

        val modifiableClass = impl(klass, "FirModifiableClass")

        val modifiableRegularClass = impl(regularClass, "FirModifiableRegularClass") {
            parents += modifiableClass.withArg(regularClass)
            parents += modifiableTypeParametersOwner
        }

        val regularClassConfig: ImplementationContext.() -> Unit = {
            parents += modifiableRegularClass
            defaultNull("companionObject")
        }
        impl(regularClass, "FirClassImpl", regularClassConfig)

        impl(sealedClass, config = regularClassConfig)

        impl(anonymousObject) {
            parents += modifiableClass.withArg(anonymousObject)
        }


        impl(typeAlias) {
            parents += modifiableTypeParametersOwner
        }

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

        impl(breakExpression) {
            lateinit("target")
        }

        impl(continueExpression) {
            lateinit("target")
        }

        impl(annotationCall) {
            parents += callWithArgumentList
            default("typeRef") {
                value = "annotationTypeRef"
                withGetter = true
            }
        }

        impl(arrayOfCall) {
            parents += callWithArgumentList
        }

        val modifiableQualifiedAccess = impl(qualifiedAccessWithoutCallee, "FirModifiableQualifiedAccess") {
            isMutable("safe")
        }

        impl(arraySetCall) {
            parents += modifiableQualifiedAccess
            lateinit("calleeReference")
            default("arguments") {
                value = "indexes + rValue"
                withGetter = true
            }
            default("lValue") {
                value = "calleeReference"
                customSetter = "calleeReference = value"
            }
            default("safe", "false")
            defaultNoReceivers()
        }

        impl(callableReferenceAccess) {
            parents += modifiableQualifiedAccess
            defaultNull("explicitReceiver")
            default("safe", "false")
            defaultNoReceivers()
            lateinit("calleeReference")
        }

        impl(componentCall) {
            parents += callWithArgumentList // modifiableQualifiedAccess
            default("safe") {
                value = "false"
                withGetter = true
            }
            listOf("dispatchReceiver", "extensionReceiver").forEach {
                default(it) {
                    value = "FirNoReceiverExpression"
                    withGetter = true
                }
            }
            default("calleeReference", "FirSimpleNamedReference(source, Name.identifier(\"component\$componentIndex\"), null)")
            useTypes(simpleNamedReferenceType, nameType, noReceiverExpressionType)
        }

        val abstractLoop = impl(loop, "FirAbstractLoop")

        impl(whileLoop) {
            parents += abstractLoop
            defaultNull("label")
            lateinit("block")
        }

        impl(doWhileLoop) {
            parents += abstractLoop
            defaultNull("label")
            lateinit("block")
        }

        impl(delegatedConstructorCall) {
            parents += callWithArgumentList
            default(
                "calleeReference",
                "if (isThis) FirExplicitThisReference(source, null) else FirExplicitSuperReference(source, constructedTypeRef)"
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
        }

        impl(block)

        val emptyExpressionBlock = impl(block, "FirEmptyExpressionBlock") {
            noSource()
        }

        impl(errorLoop) {
            default("block", "FirEmptyExpressionBlock()")
            default("condition", "FirErrorExpressionImpl(source, diagnostic)")
            defaultNull("label")
            useTypes(emptyExpressionBlock)
        }

        impl(expression, "FirExpressionStub")

        impl(functionCall) {
            parents += modifiableQualifiedAccess
            parents += callWithArgumentList
            defaultFalse("safe")
            lateinit("calleeReference")
            defaultNoReceivers()
            kind = OpenClass
        }

        impl(qualifiedAccessExpression) {
            parents += modifiableQualifiedAccess
            defaultFalse("safe")
            lateinit("calleeReference")
            defaultNoReceivers()
        }

        impl(checkNotNullCall) {
            default("calleeReference", "FirStubReference()")
            useTypes(stubReferenceType)
        }

        noImpl(expressionWithSmartcast)

        impl(getClassCall) {
            parents += callWithArgumentList
            default("argument") {
                value = "arguments.first()"
                withGetter = true
            }
        }

        val modifiableVariable = impl(variable, "FirModifiableVariable")

        impl(property) {
            parents += modifiableVariable.withArg(property)
            parents += modifiableTypeParametersOwner
            default("isVal") {
                value = "!isVar"
                withGetter = true
            }

            default("backingFieldSymbol", "FirBackingFieldSymbol(symbol.callableId)")
            default("delegateFieldSymbol", "delegate?.let { FirDelegateFieldSymbol(symbol.callableId) }")
            defaultNull("getter", "setter")
            default("resolvePhase") {
                value = "if (isLocal) FirResolvePhase.DECLARATIONS else FirResolvePhase.RAW_FIR"
            }
            useTypes(backingFieldSymbolType, delegateFieldSymbolType)
        }

        impl(field) {
            default("isVal") {
                value = "!isVar"
                withGetter = true
            }

            default("resolvePhase") {
                value = "FirResolvePhase.DECLARATIONS"
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
            parents += callWithArgumentList
            default("typeRef", """
                |if (operation in FirOperation.BOOLEANS) {
                |        FirImplicitBooleanTypeRef(null)
                |    } else {
                |        FirImplicitTypeRefImpl(null)
                |    }
                """.trimMargin())

            useTypes(implicitTypeRefType, implicitBooleanTypeRefType)
        }

        impl(typeOperatorCall) {
            parents += callWithArgumentList
        }

        impl(resolvedQualifier) {
            isMutable("packageFqName", "relativeClassFqName")
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
            lateinit("target")
            default("typeRef", "FirImplicitNothingTypeRef(source)")
            useTypes(implicitNothingTypeRefType)
        }

        impl(stringConcatenationCall) {
            parents += callWithArgumentList
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

        impl(tryExpression) {
            default("calleeReference", "FirStubReference()")
            useTypes(stubReferenceType)
        }

        impl(expression, "FirUnitExpression") {
            default("typeRef", "FirImplicitUnitTypeRef(source)")
            useTypes(implicitUnitTypeRefType)
        }

        impl(variableAssignment) {
            parents += modifiableQualifiedAccess
            lateinit("calleeReference")
            defaultNoReceivers()

            default("lValue") {
                value = "calleeReference"
                customSetter = "calleeReference = value"
            }
        }

        val modifiableFunction = impl(function, "FirModifiableFunction")

        impl(anonymousFunction) {
            parents += modifiableFunction.withArg(anonymousFunction)
            defaultNull("invocationKind", "label", "body")
            default("resolvePhase", "FirResolvePhase.DECLARATIONS")
        }

        impl(propertyAccessor) {
            parents += modifiableFunction.withArg(propertyAccessor)
            default("receiverTypeRef") {
                value = "null"
                withGetter = true
            }
            default("isSetter") {
                value = "!isGetter"
                withGetter = true
            }
            defaultNull("body")
            default("contractDescription", "FirEmptyContractDescription")
            useTypes(modalityType, emptyContractDescriptionType)
            kind = OpenClass
        }

        impl(whenExpression) {
            default("calleeReference", "FirStubReference()")
            defaultFalse("isExhaustive")
            useTypes(stubReferenceType)
        }

        impl(whenSubjectExpression) {
            default("typeRef") {
                value = "whenSubject.whenExpression.subject!!.typeRef"
                withGetter = true
            }
        }

        impl(wrappedDelegateExpression) {
            lateinit("delegateProvider")
            default("typeRef") {
                delegate = "expression"
            }
        }

        impl(resolvedNamedReference) {
            defaultNull("candidateSymbol", withGetter = true)
        }

        impl(resolvedNamedReference, "FirPropertyFromParameterResolvedNamedReference") {
            defaultNull("candidateSymbol", withGetter = true)
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
        }

        impl(resolvedTypeRef) {
            default("delegatedTypeRef") {
                value = "null"
            }
        }

        val errorTypeRefImpl = impl(errorTypeRef) {
            default("type", "ConeClassErrorType(diagnostic.reason)")
            default("delegatedTypeRef") {
                value = "null"
                withGetter = true
            }
            useTypes(coneClassErrorTypeType)
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

        impl(implicitTypeRef, "FirComputingImplicitTypeRef") {
            kind = Object
            defaultNull("source", withGetter = true)
        }

        impl(reference, "FirStubReference") {
            default("source") {
                value = "null"
                withGetter = true
            }
        }

        impl(errorNamedReference) {
            default("name", "Name.special(\"<\${diagnostic.reason}>\")")
            defaultNull("candidateSymbol", withGetter = true)
        }

        impl(typeProjection, "FirTypePlaceholderProjection") {
            kind = Object
            noSource()
        }

        val abstractLoopJump = impl(loopJump, "FirAbstractLoopJump") {
            lateinit("target")
        }

        impl(breakExpression) {
            parents += abstractLoopJump
            default("typeRef", "FirImplicitNothingTypeRef(source)")
            useTypes(implicitNothingTypeRefType)
        }

        impl(continueExpression) {
            parents += abstractLoopJump
            default("typeRef", "FirImplicitNothingTypeRef(source)")
            useTypes(implicitNothingTypeRefType)
        }

        impl(valueParameter) {
            kind = OpenClass
            parents += modifiableVariable.withArg(valueParameter)
            defaultTrue("isVal", true)
            defaultFalse("isVar", withGetter = true)
            defaultNull("getter", "setter", "initializer", "delegate", "receiverTypeRef", "delegateFieldSymbol", withGetter = true)
        }

        impl(valueParameter, "FirDefaultSetterValueParameter") {
            default("name", "Name.identifier(\"value\")")
            defaultNull("defaultValue", "initializer", "delegate", "receiverTypeRef", "delegateFieldSymbol", "getter", "setter")
            defaultFalse("isCrossinline", "isNoinline", "isVararg", "isVar")
            defaultTrue("isVal")
        }

        impl(simpleFunction) {
            kind = OpenClass
            parents += modifiableFunction.withArg(simpleFunction)
            parents += modifiableTypeParametersOwner
            defaultNull("body")
            default("contractDescription", "FirEmptyContractDescription")
            useTypes(emptyContractDescriptionType)
        }

        impl(delegatedTypeRef) {
            listOf("source", "annotations").forEach {
                default(it) {
                    delegate = "typeRef"
                }
            }
        }

        noImpl(userTypeRef)

//        impl(delegatedConstructorCall) {
//            defaultTrue("safe", withGetter = true)
//            listOf("dispatchReceiver", "extensionReceiver", "explicitReceiver").forEach {
//                default(it) {
//                    value = "FirNoReceiverExpression"
//                    withGetter = true
//                }
//            }
//        }
    }

    private fun findImplementationsWithAnnotations(implementationPredicate: (Implementation) -> Boolean): Collection<Implementation> {
        return FirTreeBuilder.elements.flatMap { it.allImplementations }.mapNotNullTo(mutableSetOf()) {
            if (!implementationPredicate(it)) return@mapNotNullTo null
            var hasAnnotations = false
            if (it.element == FirTreeBuilder.annotationContainer) return@mapNotNullTo null
            it.element.traverseParents {
                if (it == FirTreeBuilder.annotationContainer) {
                    hasAnnotations = true
                }
            }
            it.takeIf { hasAnnotations }
        }
    }

    private fun configureAllImplementations() {
        configureFieldInAllImplementations("controlFlowGraphReference") {
            default(it, "FirEmptyControlFlowGraphReference()")
            useTypes(emptyCfgReferenceType)
        }

        configureFieldInAllImplementations(
            field = "typeRef",
            implementationPredicate = { it.type !in listOf("FirDelegatedTypeRefImpl", "FirTypeProjectionWithVarianceImpl") },
            fieldPredicate = { it.defaultValue == null }
        ) {
            default(it, "FirImplicitTypeRefImpl(null)")
            useTypes(implicitTypeRefType)
        }

        configureFieldInAllImplementations(
            field = "resolvePhase",
            fieldPredicate = { it.defaultValue == null }
        ) {
            default(it, "FirResolvePhase.RAW_FIR")
        }

        configureFieldInAllImplementations(
            field = "containerSource"
        ) {
            default(it) {
                value = "null"
                isMutable = true
            }
        }

        findImplementationsWithAnnotations {
            it.type !in setOf("FirDelegatedTypeRefImpl", "FirImplicitTypeRefImpl")
        }.forEach {
            it.addParent(abstractAnnotatedElement)
        }
    }
}
