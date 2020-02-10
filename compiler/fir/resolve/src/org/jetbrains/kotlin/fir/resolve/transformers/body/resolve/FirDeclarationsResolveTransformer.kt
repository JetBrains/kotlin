/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirFunctionTarget
import org.jetbrains.kotlin.fir.copy
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.diagnostics.FirSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.builder.buildReturnExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildUnitExpression
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.*
import org.jetbrains.kotlin.fir.resolve.transformers.FirStatusResolveTransformer.Companion.resolveStatus
import org.jetbrains.kotlin.fir.resolvedTypeFromPrototype
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope
import org.jetbrains.kotlin.fir.scopes.impl.FirMemberTypeParameterScope
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildImplicitTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addIfNotNull

class FirDeclarationsResolveTransformer(transformer: FirBodyResolveTransformer) : FirPartialBodyResolveTransformer(transformer) {
    private var primaryConstructorParametersScope: FirLocalScope? = null

    private var containingClass: FirRegularClass? = null

    override fun transformDeclaration(declaration: FirDeclaration, data: ResolutionMode): CompositeTransformResult<FirDeclaration> {
        return components.withContainer(declaration) {
            declaration.replaceResolvePhase(transformerPhase)
            transformer.transformElement(declaration, data)
        }
    }

    override fun transformDeclarationStatus(
        declarationStatus: FirDeclarationStatus,
        data: ResolutionMode
    ): CompositeTransformResult<FirDeclarationStatus> {
        return ((data as? ResolutionMode.WithStatus)?.status ?: declarationStatus).compose()
    }

    private fun prepareTypeParameterOwnerForBodyResolve(declaration: FirMemberDeclaration) {
        if (declaration.typeParameters.isNotEmpty()) {
            topLevelScopes += FirMemberTypeParameterScope(declaration)
            for (typeParameter in declaration.typeParameters) {
                typeParameter.replaceResolvePhase(FirResolvePhase.STATUS)
            }
        }
    }

    private fun prepareSignatureForBodyResolve(callableMember: FirCallableMemberDeclaration<*>) {
        callableMember.transformReturnTypeRef(transformer, ResolutionMode.ContextIndependent)
        callableMember.transformReceiverTypeRef(transformer, ResolutionMode.ContextIndependent)
        if (callableMember is FirFunction<*>) {
            callableMember.valueParameters.forEach {
                it.transformReturnTypeRef(transformer, ResolutionMode.ContextIndependent)
                it.transformVarargTypeToArrayType()
            }
        }
    }

    override fun transformProperty(property: FirProperty, data: ResolutionMode): CompositeTransformResult<FirProperty> {
        return withScopeCleanup(topLevelScopes) {
            prepareTypeParameterOwnerForBodyResolve(property)
            if (property.isLocal) {
                prepareSignatureForBodyResolve(property)
                property.transformStatus(this, property.resolveStatus(property.status).mode())
                property.getter?.let { it.transformStatus(this, it.resolveStatus(it.status).mode()) }
                property.setter?.let { it.transformStatus(this, it.resolveStatus(it.status).mode()) }
                return@withScopeCleanup transformLocalVariable(property)
            }
            val returnTypeRef = property.returnTypeRef
            if (returnTypeRef !is FirImplicitTypeRef && implicitTypeOnly) return@withScopeCleanup property.compose()
            if (property.resolvePhase == transformerPhase) return@withScopeCleanup property.compose()
            if (property.resolvePhase == FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE && transformerPhase == FirResolvePhase.BODY_RESOLVE) {
                property.replaceResolvePhase(transformerPhase)
                return@withScopeCleanup property.compose()
            }
            dataFlowAnalyzer.enterProperty(property)
            withFullBodyResolve {
                withScopeCleanup(localScopes) {
                    localScopes.addIfNotNull(primaryConstructorParametersScope)
                    components.withContainer(property) {
                        property.transformChildrenWithoutAccessors(returnTypeRef)
                        property.transformInitializer(integerLiteralTypeApproximator, null)
                        if (property.initializer != null) {
                            storeVariableReturnType(property)
                        }
                        withScopeCleanup(localScopes) {
                            localScopes.add(FirLocalScope().apply {
                                storeBackingField(property)
                            })
                            property.transformAccessors()
                        }
                    }
                    property.replaceResolvePhase(transformerPhase)
                    val controlFlowGraph = dataFlowAnalyzer.exitProperty(property)
                    property.transformControlFlowGraphReference(ControlFlowGraphReferenceTransformer, controlFlowGraph)
                    property.compose()
                }
            }
        }
    }

    private fun transformLocalVariable(variable: FirProperty): CompositeTransformResult<FirProperty> {
        assert(variable.isLocal)
        val resolutionMode = withExpectedType(variable.returnTypeRef)
        variable.transformInitializer(transformer, resolutionMode)
            .transformOtherChildren(transformer, resolutionMode)
            .transformInitializer(integerLiteralTypeApproximator, null)
        if (variable.initializer != null) {
            storeVariableReturnType(variable)
        }
        variable.transformAccessors()
        localScopes.lastOrNull()?.storeVariable(variable)
        variable.replaceResolvePhase(transformerPhase)
        dataFlowAnalyzer.exitLocalVariableDeclaration(variable)
        return variable.compose()
    }

    private fun FirProperty.transformChildrenWithoutAccessors(returnTypeRef: FirTypeRef): FirProperty {
        val data = withExpectedType(returnTypeRef)
        return transformReturnTypeRef(transformer, data)
            .transformInitializer(transformer, data)
            .transformOtherChildren(transformer, data)
    }

    private fun <F : FirVariable<F>> FirVariable<F>.transformAccessors() {
        var enhancedTypeRef = returnTypeRef
        getter?.let {
            transformAccessor(it, enhancedTypeRef, this)
        }
        if (returnTypeRef is FirImplicitTypeRef) {
            storeVariableReturnType(this)
            enhancedTypeRef = returnTypeRef
        }
        setter?.let {
            it.valueParameters[0].transformReturnTypeRef(StoreType, enhancedTypeRef)
            transformAccessor(it, enhancedTypeRef, this)
        }
    }

    private fun transformAccessor(
        accessor: FirPropertyAccessor,
        enhancedTypeRef: FirTypeRef,
        owner: FirVariable<*>
    ) {
        if (accessor is FirDefaultPropertyAccessor || accessor.body == null) {
            transformFunction(accessor, withExpectedType(enhancedTypeRef))
        }
        val returnTypeRef = accessor.returnTypeRef
        val expectedReturnTypeRef = if (enhancedTypeRef is FirResolvedTypeRef && returnTypeRef !is FirResolvedTypeRef) {
            enhancedTypeRef
        } else {
            returnTypeRef
        }
        val receiverTypeRef = owner.receiverTypeRef
        if (receiverTypeRef != null) {
            withLabelAndReceiverType(owner.name, owner, receiverTypeRef.coneTypeUnsafe()) {
                transformFunctionWithGivenSignature(accessor, expectedReturnTypeRef)
            }
        } else {
            transformFunctionWithGivenSignature(accessor, expectedReturnTypeRef)
        } as CompositeTransformResult<FirStatement>
    }

    private fun FirDeclaration.resolveStatus(status: FirDeclarationStatus, containingClass: FirClass<*>? = null): FirDeclarationStatus {
        val containingDeclaration = components.containerIfAny
        return resolveStatus(
            status, containingClass as? FirRegularClass, isLocal = containingDeclaration != null && containingClass == null
        )
    }

    private fun prepareLocalClassForBodyResolve(klass: FirClass<*>) {
        if (klass.superTypeRefs.any { it !is FirResolvedTypeRef }) {
            klass.replaceSuperTypeRefs(
                klass.superTypeRefs.map { superTypeRef ->
                    this.transformer.transformTypeRef(superTypeRef, ResolutionMode.ContextIndependent).single
                }
            )
        }
        if (klass is FirRegularClass) {
            klass.transformStatus(transformer, klass.resolveStatus(klass.status).mode())
        }
        // This is necessary because of possible jumps from implicit bodies inside
        for (declaration in klass.declarations) {
            when (declaration) {
                is FirRegularClass -> {
                    prepareLocalClassForBodyResolve(declaration)
                }
                is FirCallableMemberDeclaration<*> -> {
                    withScopeCleanup(topLevelScopes) {
                        prepareTypeParameterOwnerForBodyResolve(declaration)
                        prepareSignatureForBodyResolve(declaration)
                        declaration.transformStatus(transformer, declaration.resolveStatus(declaration.status, klass).mode())
                        if (declaration is FirProperty) {
                            declaration.getter?.let { it.transformStatus(this, it.resolveStatus(it.status, klass).mode()) }
                            declaration.setter?.let { it.transformStatus(this, it.resolveStatus(it.status, klass).mode()) }
                        }
                    }
                }
            }
            declaration.replaceResolvePhase(FirResolvePhase.STATUS)
        }
        if (klass is FirRegularClass) {
            for (typeParameter in klass.typeParameters) {
                typeParameter.replaceResolvePhase(FirResolvePhase.STATUS)
            }
        }
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        localScopes.lastOrNull()?.storeClass(regularClass)
        return withScopeCleanup(topLevelScopes) {
            prepareTypeParameterOwnerForBodyResolve(regularClass)
            if (regularClass.symbol.classId.isLocal) {
                prepareLocalClassForBodyResolve(regularClass)
            }

            val oldConstructorScope = primaryConstructorParametersScope
            val oldContainingClass = containingClass
            primaryConstructorParametersScope = null
            containingClass = regularClass
            val type = regularClass.defaultType()
            val result = withLabelAndReceiverType(regularClass.name, regularClass, type) {
                val constructor = regularClass.declarations.firstOrNull() as? FirConstructor
                if (constructor?.isPrimary == true) {
                    primaryConstructorParametersScope = FirLocalScope().apply {
                        constructor.valueParameters.forEach { this.storeVariable(it) }
                    }
                }
                transformDeclaration(regularClass, data)
            }
            containingClass = oldContainingClass
            primaryConstructorParametersScope = oldConstructorScope
            result as CompositeTransformResult<FirStatement>
        }
    }

    override fun transformAnonymousObject(anonymousObject: FirAnonymousObject, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        prepareLocalClassForBodyResolve(anonymousObject)
        return withScopeCleanup(topLevelScopes) {
            val type = anonymousObject.defaultType()
            anonymousObject.resultType = buildResolvedTypeRef {
                source = anonymousObject.source
                this.type = type
            }
            val result = withLabelAndReceiverType(null, anonymousObject, type) {
                transformDeclaration(anonymousObject, data)
            }
            result as CompositeTransformResult<FirStatement>
        }
    }

    private fun transformAnonymousFunctionWithLambdaResolution(
        anonymousFunction: FirAnonymousFunction, lambdaResolution: ResolutionMode.LambdaResolution
    ): FirAnonymousFunction {
        val receiverTypeRef = anonymousFunction.receiverTypeRef
        fun transform(): FirAnonymousFunction {
            val expectedReturnType =
                lambdaResolution.expectedReturnTypeRef ?: anonymousFunction.returnTypeRef.takeUnless { it is FirImplicitTypeRef }
            val result = transformFunction(anonymousFunction, withExpectedType(expectedReturnType)).single as FirAnonymousFunction
            val body = result.body
            return if (result.returnTypeRef is FirImplicitTypeRef && body != null) {
                result.transformReturnTypeRef(transformer, withExpectedType(body.resultType))
                result
            } else {
                result
            }
        }

        val label = anonymousFunction.label
        return if (label != null && receiverTypeRef is FirResolvedTypeRef) {
            withLabelAndReceiverType(Name.identifier(label.name), anonymousFunction, receiverTypeRef.type) {
                transform()
            }
        } else {
            transform()
        }
    }

    override fun transformSimpleFunction(simpleFunction: FirSimpleFunction, data: ResolutionMode): CompositeTransformResult<FirSimpleFunction> {
        if (simpleFunction.resolvePhase == transformerPhase) return simpleFunction.compose()
        if (simpleFunction.resolvePhase == FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE && transformerPhase == FirResolvePhase.BODY_RESOLVE) {
            simpleFunction.replaceResolvePhase(transformerPhase)
            return simpleFunction.compose()
        }
        val returnTypeRef = simpleFunction.returnTypeRef
        if ((returnTypeRef !is FirImplicitTypeRef) && implicitTypeOnly) {
            return simpleFunction.compose()
        }

        return withScopeCleanup(topLevelScopes) {
            prepareTypeParameterOwnerForBodyResolve(simpleFunction)
            val containingDeclaration = components.containerIfAny
            if (containingDeclaration != null && containingDeclaration !is FirClass<*>) {
                // For class members everything should be already prepared
                prepareSignatureForBodyResolve(simpleFunction)
                simpleFunction.transformStatus(this, simpleFunction.resolveStatus(simpleFunction.status).mode())
            }

            withFullBodyResolve {
                val receiverTypeRef = simpleFunction.receiverTypeRef
                if (receiverTypeRef != null) {
                    withLabelAndReceiverType(simpleFunction.name, simpleFunction, receiverTypeRef.coneTypeUnsafe()) {
                        transformFunctionWithGivenSignature(simpleFunction, returnTypeRef)
                    }
                } else {
                    transformFunctionWithGivenSignature(simpleFunction, returnTypeRef)
                }
            }
        }
    }

    private fun <F : FirFunction<F>> transformFunctionWithGivenSignature(
        function: F,
        returnTypeRef: FirTypeRef
    ): CompositeTransformResult<F> {
        if (function is FirSimpleFunction) {
            localScopes.lastOrNull()?.storeFunction(function)
        }

        @Suppress("UNCHECKED_CAST")
        val result = transformFunction(function, withExpectedType(returnTypeRef)).single as F

        val body = result.body
        if (result.returnTypeRef is FirImplicitTypeRef) {
            if (body != null) {
                result.transformReturnTypeRef(transformer, withExpectedType(body.resultType))
            } else {
                result.transformReturnTypeRef(
                    transformer,
                    withExpectedType(buildErrorTypeRef { diagnostic = FirSimpleDiagnostic("empty body", DiagnosticKind.Other) })
                )
            }
        }

        return result.compose()
    }

    override fun <F : FirFunction<F>> transformFunction(function: FirFunction<F>, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        return withScopeCleanup(localScopes) {
            localScopes += FirLocalScope()
            dataFlowAnalyzer.enterFunction(function)
            transformDeclaration(function, data).also {
                val result = it.single as FirFunction<*>
                dataFlowAnalyzer.exitFunction(result)?.let { controlFlowGraph ->
                    result.transformControlFlowGraphReference(ControlFlowGraphReferenceTransformer, controlFlowGraph)
                }
            } as CompositeTransformResult<FirStatement>
        }
    }

    override fun transformConstructor(constructor: FirConstructor, data: ResolutionMode): CompositeTransformResult<FirDeclaration> {
        if (implicitTypeOnly) return constructor.compose()
        return transformFunction(constructor, data) as CompositeTransformResult<FirDeclaration>
    }

    override fun transformAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer,
        data: ResolutionMode
    ): CompositeTransformResult<FirDeclaration> {
        if (implicitTypeOnly) return anonymousInitializer.compose()
        return withScopeCleanup(localScopes) {
            dataFlowAnalyzer.enterInitBlock(anonymousInitializer)
            localScopes.addIfNotNull(primaryConstructorParametersScope)
            transformDeclaration(anonymousInitializer, ResolutionMode.ContextIndependent).also {
                dataFlowAnalyzer.exitInitBlock(it.single as FirAnonymousInitializer)
            }
        }
    }

    override fun transformValueParameter(valueParameter: FirValueParameter, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        localScopes.lastOrNull()?.storeVariable(valueParameter)
        if (valueParameter.returnTypeRef is FirImplicitTypeRef) {
            valueParameter.replaceResolvePhase(transformerPhase)
            return valueParameter.compose() // TODO
        }
        val valueParameter = valueParameter.transformInitializer(integerLiteralTypeApproximator, valueParameter.returnTypeRef.coneTypeSafe())
        return (transformDeclaration(valueParameter, withExpectedType(valueParameter.returnTypeRef)).single as FirStatement).compose()
    }

    override fun transformAnonymousFunction(
        anonymousFunction: FirAnonymousFunction,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        // Either ContextDependent, ContextIndependent or WithExpectedType could be here
        if (data !is ResolutionMode.LambdaResolution) {
            anonymousFunction.transformReturnTypeRef(transformer, ResolutionMode.ContextIndependent)
            anonymousFunction.transformReceiverTypeRef(transformer, ResolutionMode.ContextIndependent)
            anonymousFunction.valueParameters.forEach { it.transformReturnTypeRef(transformer, ResolutionMode.ContextIndependent) }
        }
        return when (data) {
            ResolutionMode.ContextDependent -> {
                dataFlowAnalyzer.visitPostponedAnonymousFunction(anonymousFunction)
                anonymousFunction.addReturn().compose()
            }
            is ResolutionMode.LambdaResolution -> {
                transformAnonymousFunctionWithLambdaResolution(anonymousFunction, data).addReturn().compose()
            }
            is ResolutionMode.WithExpectedType, is ResolutionMode.ContextIndependent -> {
                val expectedTypeRef = (data as? ResolutionMode.WithExpectedType)?.expectedTypeRef ?: buildImplicitTypeRef()
                val resolvedLambdaAtom = (expectedTypeRef as? FirResolvedTypeRef)?.let {
                    extractLambdaInfoFromFunctionalType(
                        it.type, it, anonymousFunction, session
                    )
                }
                var af = anonymousFunction
                val valueParameters =
                    if (resolvedLambdaAtom == null) af.valueParameters
                    else {
                        val singleParameterType = resolvedLambdaAtom.parameters.singleOrNull()
                        val itParam = when {
                            af.valueParameters.isEmpty() && singleParameterType != null -> {
                                val name = Name.identifier("it")
                                buildValueParameter {
                                    session = this@FirDeclarationsResolveTransformer.session
                                    returnTypeRef = buildResolvedTypeRef { type = singleParameterType }
                                    this.name = name
                                    symbol = FirVariableSymbol(name)
                                    isCrossinline = false
                                    isNoinline = false
                                    isVararg = false
                                }
                            }
                            else -> null
                        }
                        if (itParam != null) {
                            listOf(itParam)
                        } else {
                            af.valueParameters.mapIndexed { index, param ->
                                if (param.returnTypeRef is FirResolvedTypeRef) {
                                    param
                                } else {
                                    param.transformReturnTypeRef(
                                        StoreType,
                                        param.returnTypeRef.resolvedTypeFromPrototype(
                                            resolvedLambdaAtom.parameters[index]
                                        )
                                    )
                                    param
                                }
                            }
                        }

                    }
                val returnTypeRefFromResolvedAtom = resolvedLambdaAtom?.returnType?.let { af.returnTypeRef.resolvedTypeFromPrototype(it) }
                af = af.copy(
                    receiverTypeRef = af.receiverTypeRef?.takeIf { it !is FirImplicitTypeRef }
                        ?: resolvedLambdaAtom?.receiver?.let { af.receiverTypeRef?.resolvedTypeFromPrototype(it) },
                    valueParameters = valueParameters,
                    returnTypeRef = (af.returnTypeRef as? FirResolvedTypeRef)
                        ?: returnTypeRefFromResolvedAtom
                        ?: af.returnTypeRef
                )
                af = af.transformValueParameters(ImplicitToErrorTypeTransformer, null)
                val bodyExpectedType = returnTypeRefFromResolvedAtom ?: expectedTypeRef
                af = transformFunction(af, withExpectedType(bodyExpectedType)).single as FirAnonymousFunction
                // To separate function and separate commit
                val writer = FirCallCompletionResultsWriterTransformer(
                    session,
                    ConeSubstitutor.Empty,
                    components.returnTypeCalculator,
                    inferenceComponents.approximator,
                    integerOperatorsTypeUpdater,
                    integerLiteralTypeApproximator
                )
                af.transformSingle(writer, expectedTypeRef.coneTypeSafe<ConeKotlinType>()?.toExpectedType())
                val returnTypes = dataFlowAnalyzer.returnExpressionsOfAnonymousFunction(af).mapNotNull { (it as? FirExpression)?.resultType?.coneTypeUnsafe() }
                af.replaceReturnTypeRef(af.returnTypeRef.resolvedTypeFromPrototype(inferenceComponents.ctx.commonSuperTypeOrNull(returnTypes) ?: session.builtinTypes.unitType.coneTypeUnsafe()))
                af.replaceTypeRef(af.constructFunctionalTypeRef(session))
                af.addReturn().compose()
            }
            is ResolutionMode.WithStatus -> {
                throw AssertionError("Should not be here in WithStatus mode")
            }
        }
    }

    private fun FirAnonymousFunction.addReturn(): FirAnonymousFunction {
        val lastStatement = body?.statements?.lastOrNull()
        val returnType = (body?.typeRef as? FirResolvedTypeRef) ?: return this
        val returnNothing = returnType.isNothing || returnType.isUnit
        if (lastStatement is FirExpression && !returnNothing) {
            body?.transformChildren(
                object : FirDefaultTransformer<FirExpression>() {
                    override fun <E : FirElement> transformElement(element: E, data: FirExpression): CompositeTransformResult<E> {
                        if (element == lastStatement) {
                            val returnExpression = buildReturnExpression {
                                source = element.source
                                result = lastStatement
                                target = FirFunctionTarget(null, isLambda = this@addReturn.isLambda).also {
                                    it.bind(this@addReturn)
                                }
                            }
                            return (returnExpression as E).compose()
                        }
                        return element.compose()
                    }

                    override fun transformReturnExpression(
                        returnExpression: FirReturnExpression,
                        data: FirExpression
                    ): CompositeTransformResult<FirStatement> {
                        return returnExpression.compose()
                    }
                },
                buildUnitExpression()
            )
        }
        return this
    }

    private inline fun <T> withLabelAndReceiverType(
        labelName: Name?,
        owner: FirDeclaration,
        type: ConeKotlinType,
        block: () -> T
    ): T {
        val implicitCompanionValues = mutableListOf<ImplicitReceiverValue<*>>()
        val implicitReceiverValue = when (owner) {
            is FirClass<*> -> {
                // Questionable: performance
                (owner as? FirRegularClass)?.companionObject?.let { companion ->
                    implicitCompanionValues += ImplicitDispatchReceiverValue(
                        companion.symbol, session, scopeSession, kind = ImplicitDispatchReceiverKind.COMPANION
                    )
                }
                lookupSuperTypes(owner, lookupInterfaces = false, deep = true, useSiteSession = session).mapNotNull {
                    val superClass = (it as? ConeClassLikeType)?.lookupTag?.toSymbol(session)?.fir as? FirRegularClass
                    superClass?.companionObject?.let { companion ->
                        implicitCompanionValues += ImplicitDispatchReceiverValue(
                            companion.symbol, session, scopeSession, kind = ImplicitDispatchReceiverKind.COMPANION_FROM_SUPERTYPE
                        )
                    }
                }
                // ---
                ImplicitDispatchReceiverValue(owner.symbol, type, session, scopeSession)
            }
            is FirFunction<*> -> {
                ImplicitExtensionReceiverValue(owner.symbol, type, session, scopeSession)
            }
            is FirVariable<*> -> {
                ImplicitExtensionReceiverValue(owner.symbol, type, session, scopeSession)
            }
            else -> {
                throw IllegalArgumentException("Incorrect label & receiver owner: ${owner.javaClass}")
            }
        }
        for (implicitCompanionValue in implicitCompanionValues.asReversed()) {
            implicitReceiverStack.add(null, implicitCompanionValue)
        }
        implicitReceiverStack.add(labelName, implicitReceiverValue)
        val result = block()
        implicitReceiverStack.pop(labelName)
        for (implicitCompanionValue in implicitCompanionValues) {
            implicitReceiverStack.pop(null)
        }
        return result
    }

    private fun storeVariableReturnType(variable: FirVariable<*>) {
        val initializer = variable.initializer
        if (variable.returnTypeRef is FirImplicitTypeRef) {
            when {
                initializer != null -> {
                    val expectedType = when (val resultType = initializer.resultType) {
                        is FirImplicitTypeRef -> buildErrorTypeRef {
                            diagnostic = FirSimpleDiagnostic("No result type for initializer", DiagnosticKind.InferenceError)
                        }
                        else -> resultType
                    }
                    variable.transformReturnTypeRef(
                        transformer,
                        withExpectedType(expectedType)
                    )
                }
                variable.getter != null && variable.getter !is FirDefaultPropertyAccessor -> {
                    val expectedType = when (val resultType = variable.getter?.returnTypeRef) {
                        is FirImplicitTypeRef -> buildErrorTypeRef {
                            diagnostic = FirSimpleDiagnostic("No result type for getter", DiagnosticKind.InferenceError)
                        }
                        else -> resultType
                    }
                    variable.transformReturnTypeRef(
                        transformer,
                        withExpectedType(expectedType)
                    )
                }
                else -> {
                    variable.transformReturnTypeRef(
                        transformer,
                        withExpectedType(
                            buildErrorTypeRef {
                                diagnostic = FirSimpleDiagnostic(
                                    "Cannot infer variable type without initializer / getter / delegate",
                                    DiagnosticKind.InferenceError,
                                )
                            },
                        )
                    )
                }
            }
            if (variable.getter?.returnTypeRef is FirImplicitTypeRef) {
                variable.getter?.transformReturnTypeRef(transformer, withExpectedType(variable.returnTypeRef))
            }
        }
    }

    private object ImplicitToErrorTypeTransformer : FirTransformer<Nothing?>() {
        override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
            return element.compose()
        }

        override fun transformValueParameter(valueParameter: FirValueParameter, data: Nothing?): CompositeTransformResult<FirStatement> {
            if (valueParameter.returnTypeRef is FirImplicitTypeRef) {
                valueParameter.transformReturnTypeRef(
                    StoreType,
                    valueParameter.returnTypeRef.resolvedTypeFromPrototype(ConeKotlinErrorType("No type for parameter"))
                )
            }
            return valueParameter.compose()
        }
    }
}
