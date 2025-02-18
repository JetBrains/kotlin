/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import com.intellij.psi.PsiElement
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirDesignation
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.withFirDesignationEntry
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.forEachDeclaration
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.builder.PsiRawFirBuilder
import org.jetbrains.kotlin.fir.contracts.FirErrorContractDescription
import org.jetbrains.kotlin.fir.contracts.FirLegacyRawContractDescription
import org.jetbrains.kotlin.fir.contracts.FirRawContractDescription
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.getExplicitBackingField
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirContractCallBlock
import org.jetbrains.kotlin.fir.expressions.impl.FirLazyDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.references.FirDelegateFieldReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildDelegateFieldReference
import org.jetbrains.kotlin.fir.references.builder.buildImplicitThisReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.scopes.impl.originalConstructorIfTypeAlias
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

object FirLazyBodiesCalculator {
    fun calculateBodies(designation: FirDesignation) {
        designation.target.transformSingle(
            FirTargetLazyBodiesCalculatorTransformer,
            designation.path.toPersistentList(),
        )
    }

    fun calculateContracts(designation: FirDesignation) {
        designation.target.transformSingle(
            FirTargetLazyContractsCalculatorTransformer,
            designation.path.toPersistentList(),
        )
    }

    @TestOnly
    fun calculateAllLazyExpressionsInFile(firFile: FirFile) {
        firFile.transformSingle(FirAllLazyContractsCalculatorTransformer, persistentListOf())
        firFile.accept(RecursiveLazyAnnotationCalculatorVisitor, firFile.moduleData.session)
        firFile.transformSingle(FirAllLazyBodiesCalculatorTransformer, persistentListOf())
    }

    fun calculateAnnotations(firElement: FirElementWithResolveState) {
        firElement.accept(LazyAnnotationCalculatorVisitor, firElement.moduleData.session)
    }

    fun calculateLazyArgumentsForAnnotation(annotationCall: FirAnnotationCall, session: FirSession): FirArgumentList {
        require(needCalculatingAnnotationCall(annotationCall))
        return createArgumentsForAnnotation(annotationCall, session)
    }

    fun createArgumentsForAnnotation(annotationCall: FirAnnotationCall, session: FirSession): FirArgumentList {
        val builder = PsiRawFirBuilder(session, baseScopeProvider = session.kotlinScopeProvider)
        val ktAnnotationEntry = annotationCall.psi as KtAnnotationEntry
        builder.context.packageFqName = ktAnnotationEntry.containingKtFile.packageFqName
        val newAnnotationCall = builder.buildAnnotationCall(ktAnnotationEntry, annotationCall.containingDeclarationSymbol)
        return newAnnotationCall.argumentList
    }

    fun needCalculatingAnnotationCall(firAnnotationCall: FirAnnotationCall): Boolean =
        firAnnotationCall.argumentList.arguments.any { it is FirLazyExpression }
}

inline fun <reified T : FirDeclaration> revive(
    designation: FirDesignation,
    psi: PsiElement? = designation.target.psi,
): T {
    val session = designation.target.moduleData.session

    return RawFirNonLocalDeclarationBuilder.buildWithFunctionSymbolRebind(
        session = session,
        scopeProvider = session.kotlinScopeProvider,
        designation = designation,
        rootNonLocalDeclaration = psi as KtAnnotated,
    ) as T
}

fun replaceLazyValueParameters(target: FirFunction, copy: FirFunction) {
    val targetParameters = target.valueParameters
    val copyParameters = copy.valueParameters
    require(targetParameters.size == copyParameters.size)

    for ((valueParameter, newValueParameter) in targetParameters.zip(copyParameters)) {
        if (valueParameter.defaultValue is FirLazyExpression) {
            valueParameter.replaceDefaultValue(newValueParameter.defaultValue)
        }
    }
}

/**
 * @param isContractResolved is **false** during [FirResolvePhase.CONTRACTS]
 * and **true** for the following phases.
 * **true** flag assumes that the declaration already passes the [FirResolvePhase.CONTRACTS] phase,
 * so it is possible to depend on [FirContractDescriptionOwner.contractDescription].
 *
 * Raw body may have false-positive contracts, so the final decision will be made only during the [FirResolvePhase.CONTRACTS] phase.
 * In the case of a false positive the redundant [FirContractCallBlock] should be unwrapped to allow the body be processed
 * correctly by other transformers and checkers.
 */
fun replaceLazyBody(target: FirFunction, copy: FirFunction, isContractResolved: Boolean = true) {
    if (target.body !is FirLazyBlock) return

    val newBody = copy.body
    if (isContractResolved && target is FirContractDescriptionOwner) {
        val newContractBlock = newBody?.statements?.firstOrNull() as? FirContractCallBlock
        if (newContractBlock != null && target.contractDescription !is FirResolvedContractDescription) {
            newBody.replaceFirstStatement<FirStatement> { newContractBlock.call }
        }
    }

    target.replaceBody(newBody)
}

fun replaceLazyDelegatedConstructor(target: FirConstructor, copy: FirConstructor) {
    val targetCall = target.delegatedConstructor
    val copyCall = copy.delegatedConstructor

    when (targetCall) {
        is FirLazyDelegatedConstructorCall -> {
            require(copyCall !is FirMultiDelegatedConstructorCall)
            target.replaceDelegatedConstructor(copyCall)
        }
        is FirMultiDelegatedConstructorCall -> {
            require(copyCall is FirMultiDelegatedConstructorCall)
            require(targetCall.delegatedConstructorCalls.size == copyCall.delegatedConstructorCalls.size)

            val newCalls = targetCall.delegatedConstructorCalls.zip(copyCall.delegatedConstructorCalls)
                .map { (target, copy) -> target.takeUnless { it is FirLazyDelegatedConstructorCall } ?: copy }

            targetCall.replaceDelegatedConstructorCalls(newCalls)
        }
    }
}

fun replaceLazyInitializer(target: FirVariable, copy: FirVariable) {
    if (target.initializer is FirLazyExpression) {
        target.replaceInitializer(copy.initializer)
    }
}

fun replaceLazyDelegate(target: FirVariable, copy: FirVariable) {
    if (target.delegate is FirLazyExpression) {
        target.replaceDelegate(copy.delegate)
    }
}

val FirCallableDeclaration.originalPsi: PsiElement? get() = unwrapFakeOverridesOrDelegated().psi

fun calculateLazyBodiesForFunction(designation: FirDesignation) {
    val simpleFunction = designation.target as FirSimpleFunction
    require(needCalculatingLazyBodyForFunction(simpleFunction))

    val newSimpleFunction = revive<FirSimpleFunction>(designation, simpleFunction.originalPsi)

    replaceLazyBody(simpleFunction, newSimpleFunction)
    replaceLazyValueParameters(simpleFunction, newSimpleFunction)
}

fun calculateLazyBodyForConstructor(designation: FirDesignation) {
    val constructor = designation.target as FirConstructor
    require(needCalculatingLazyBodyForConstructor(constructor))

    // TODO A temporary hack to avoid problems with lazy resolve of typealiased constructors; see KT-73481
    val constructorPsi = (constructor.originalConstructorIfTypeAlias ?: constructor).psi

    val newConstructor = revive<FirConstructor>(designation, constructorPsi)

    replaceLazyBody(constructor, newConstructor)
    replaceLazyDelegatedConstructor(constructor, newConstructor)
    replaceLazyValueParameters(constructor, newConstructor)
}

fun calculateLazyBodyForProperty(designation: FirDesignation) {
    val firProperty = designation.target as FirProperty
    if (!needCalculatingLazyBodyForProperty(firProperty)) return
    if (firProperty.origin == FirDeclarationOrigin.ScriptCustomization.ResultProperty) {
        calculateLazyBodyForResultProperty(firProperty, designation)
        return
    }

    val recreatedProperty = revive<FirProperty>(designation, firProperty.originalPsi)

    firProperty.getter?.let { getter ->
        val recreatedGetter = recreatedProperty.getter!!
        replaceLazyBody(getter, recreatedGetter)
        rebindDelegatedAccessorBody(newTarget = getter, oldTarget = recreatedGetter)
    }

    firProperty.setter?.let { setter ->
        val recreatedSetter = recreatedProperty.setter!!
        replaceLazyBody(setter, recreatedSetter)
        rebindDelegatedAccessorBody(newTarget = setter, oldTarget = recreatedSetter)
    }

    replaceLazyInitializer(firProperty, recreatedProperty)
    replaceLazyDelegate(firProperty, recreatedProperty)
    rebindDelegate(newTarget = firProperty, oldTarget = recreatedProperty)

    firProperty.getExplicitBackingField()?.let { backingField ->
        val newBackingField = recreatedProperty.getExplicitBackingField()!!
        replaceLazyInitializer(backingField, newBackingField)
    }
}

fun calculateLazyBodyForResultProperty(firProperty: FirProperty, designation: FirDesignation) {
    val newInitializer = revive<FirAnonymousInitializer>(designation)
    val body = newInitializer.body
    requireWithAttachment(body != null, { "${FirAnonymousInitializer::class.simpleName} without body" }) {
        withFirDesignationEntry("designation", designation)
        withFirEntry("initializer", newInitializer)
    }

    val singleStatement = body.statements.singleOrNull()
    requireWithAttachment(singleStatement is FirExpression, { "Unexpected body content" }) {
        withFirDesignationEntry("designation", designation)
        withFirEntry("initializer", newInitializer)
        singleStatement?.let {
            withFirEntry("statement", it)
        }
    }

    firProperty.replaceInitializer(singleStatement)
}

/**
 * This function is required to correctly rebind symbols
 * after [generateAccessorsByDelegate][org.jetbrains.kotlin.fir.builder.generateAccessorsByDelegate]
 * for correct work
 *
 * @see org.jetbrains.kotlin.fir.builder.generateAccessorsByDelegate
 */
fun rebindDelegate(newTarget: FirProperty, oldTarget: FirProperty) {
    val delegate = newTarget.delegate ?: return
    requireWithAttachment(
        delegate is FirWrappedDelegateExpression,
        { "Unexpected delegate type: ${delegate::class.simpleName}" },
    ) {
        withFirEntry("newTarget", newTarget)
        withFirEntry("oldTarget", oldTarget)
        withFirEntry("delegate", delegate)
    }

    val delegateProvider = delegate.provideDelegateCall
    rebindArgumentList(
        delegateProvider.argumentList,
        newTarget = newTarget.symbol,
        oldTarget = oldTarget.symbol,
        isSetter = false,
        canHavePropertySymbolAsThisReference = false,
    )
}

/**
 * This function is required to correctly rebind symbols
 * after [generateAccessorsByDelegate][org.jetbrains.kotlin.fir.builder.generateAccessorsByDelegate]
 * for correct work
 *
 * @see org.jetbrains.kotlin.fir.builder.generateAccessorsByDelegate
 * @see rebindDelegate
 */
fun rebindDelegatedAccessorBody(newTarget: FirPropertyAccessor, oldTarget: FirPropertyAccessor) {
    if (newTarget.source?.kind != KtFakeSourceElementKind.DelegatedPropertyAccessor) return
    val body = newTarget.body
    requireWithAttachment(
        body is FirSingleExpressionBlock,
        { "Unexpected body for generated accessor ${body?.let { it::class.simpleName }}" },
    ) {
        withFirSymbolEntry("newTarget", newTarget.propertySymbol)
        withFirSymbolEntry("oldTarget", oldTarget.propertySymbol)
        body?.let { withFirEntry("body", it) } ?: withEntry("body", "null")
    }

    val returnExpression = body.statement
    rebindReturnExpression(returnExpression = returnExpression, newTarget = newTarget, oldTarget = oldTarget)
}

fun rebindReturnExpression(returnExpression: FirStatement, newTarget: FirPropertyAccessor, oldTarget: FirPropertyAccessor) {
    requireWithAttachment(returnExpression is FirReturnExpression, { "Unexpected single statement" }) {
        withFirSymbolEntry("newTarget", newTarget.propertySymbol)
        withFirSymbolEntry("oldTarget", oldTarget.propertySymbol)
        withFirEntry("expression", returnExpression)
    }

    val functionCall = returnExpression.result
    rebindFunctionCall(functionCall, newTarget, oldTarget)
}

fun rebindFunctionCall(functionCall: FirExpression, newTarget: FirPropertyAccessor, oldTarget: FirPropertyAccessor) {
    requireWithAttachment(functionCall is FirFunctionCall, { "Unexpected result expression ${functionCall::class.simpleName}" }) {
        withFirSymbolEntry("newTarget", newTarget.propertySymbol)
        withFirSymbolEntry("oldTarget", oldTarget.propertySymbol)
        withFirEntry("functionCall", functionCall)
    }

    rebindDelegateAccess(
        expression = functionCall.explicitReceiver,
        newPropertySymbol = newTarget.propertySymbol,
        oldPropertySymbol = oldTarget.propertySymbol,
    )

    rebindArgumentList(
        argumentList = functionCall.argumentList,
        newTarget = newTarget.propertySymbol,
        oldTarget = oldTarget.propertySymbol,
        isSetter = newTarget.isSetter,
        canHavePropertySymbolAsThisReference = true,
    )
}

/**
 * To cover `thisRef` function
 *
 * @see org.jetbrains.kotlin.fir.builder.generateAccessorsByDelegate
 */
fun rebindThisRef(
    expression: FirExpression,
    newTarget: FirPropertySymbol,
    oldTarget: FirPropertySymbol,
    canHavePropertySymbolAsThisReference: Boolean,
) {
    if (expression is FirLiteralExpression) return

    requireWithAttachment(
        expression is FirThisReceiverExpression,
        { "Unexpected this reference expression: ${expression::class.simpleName}" },
    ) {
        withFirSymbolEntry("newTarget", newTarget)
        withFirSymbolEntry("oldTarget", oldTarget)
        withFirEntry("expression", expression)
    }

    val boundSymbol = expression.calleeReference.boundSymbol
    if (boundSymbol is FirClassSymbol<*>) return
    requireWithAttachment(
        canHavePropertySymbolAsThisReference,
        { "Class bound symbol is not found: ${boundSymbol?.let { it::class.simpleName }}" },
    ) {
        withFirSymbolEntry("newTarget", newTarget)
        withFirSymbolEntry("oldTarget", oldTarget)
        boundSymbol?.let { withFirSymbolEntry("boundSymbol", boundSymbol as FirBasedSymbol<*>) }
    }

    requireWithAttachment(
        boundSymbol is FirReceiverParameterSymbol && boundSymbol.containingDeclarationSymbol == oldTarget,
        {
            "Unexpected bound symbol: ${boundSymbol?.let { it::class.simpleName }}"
        }
    ) {
        withFirSymbolEntry("newTarget", newTarget)
        withFirSymbolEntry("oldTarget", oldTarget)
        boundSymbol?.let { withFirSymbolEntry("boundSymbol", boundSymbol as FirBasedSymbol<*>) }
    }

    expression.replaceCalleeReference(buildImplicitThisReference {
        this.boundSymbol = newTarget.receiverParameter!!.symbol
    })
}

fun rebindArgumentList(
    argumentList: FirArgumentList,
    newTarget: FirPropertySymbol,
    oldTarget: FirPropertySymbol,
    isSetter: Boolean,
    canHavePropertySymbolAsThisReference: Boolean,
) {
    val arguments = argumentList.arguments
    val expectedSize = 2 + if (isSetter) 1 else 0
    requireWithAttachment(
        arguments.size == expectedSize,
        { "Unexpected arguments size. Expected: $expectedSize, actual: ${arguments.size}" },
    ) {
        withFirSymbolEntry("newTarget", newTarget)
        withFirSymbolEntry("oldTarget", oldTarget)
        withFirEntry("expression", argumentList)
    }

    rebindThisRef(
        expression = arguments[0],
        newTarget = newTarget,
        oldTarget = oldTarget,
        canHavePropertySymbolAsThisReference = canHavePropertySymbolAsThisReference,
    )

    rebindPropertyRef(expression = arguments[1], newPropertySymbol = newTarget, oldPropertySymbol = oldTarget)

    if (isSetter) {
        rebindSetterParameter(expression = arguments[2], newPropertySymbol = newTarget, oldPropertySymbol = oldTarget)
    }
}

/**
 * To cover third argument in setter body
 *
 * @see org.jetbrains.kotlin.fir.builder.generateAccessorsByDelegate
 */
fun rebindSetterParameter(expression: FirExpression, newPropertySymbol: FirPropertySymbol, oldPropertySymbol: FirPropertySymbol) {
    requireWithAttachment(
        expression is FirPropertyAccessExpression,
        { "Unexpected third argument: ${expression::class.simpleName}" }) {
        withFirSymbolEntry("newTarget", newPropertySymbol)
        withFirSymbolEntry("oldTarget", oldPropertySymbol)
        withFirEntry("expression", expression)
    }

    val calleeReference = expression.resolvedCalleeReference(newPropertySymbol = newPropertySymbol, oldPropertySymbol = oldPropertySymbol)
    val resolvedParameterSymbol = calleeReference.resolvedSymbol
    val oldValueParameterSymbol = oldPropertySymbol.setterSymbol?.valueParameterSymbols?.first()
    requireWithAttachment(
        resolvedParameterSymbol == oldValueParameterSymbol,
        { "Unexpected symbol: ${resolvedParameterSymbol::class.simpleName}" },
    ) {
        withFirEntry("expression", expression)
        withFirSymbolEntry("actualOldParameter", resolvedParameterSymbol)
        oldValueParameterSymbol?.let { withFirSymbolEntry("expectedOldParameter", it) }
        withFirSymbolEntry("oldProperty", oldPropertySymbol)
        withFirSymbolEntry("newProperty", newPropertySymbol)
    }

    expression.replaceCalleeReference(buildResolvedNamedReference {
        source = calleeReference.source
        name = calleeReference.name
        resolvedSymbol = newPropertySymbol.setterSymbol?.valueParameterSymbols?.first() ?: errorWithAttachment("Parameter is not found") {
            withFirSymbolEntry("oldProperty", oldPropertySymbol)
            withFirSymbolEntry("newProperty", newPropertySymbol)
        }
    })
}

fun FirQualifiedAccessExpression.resolvedCalleeReference(
    newPropertySymbol: FirPropertySymbol,
    oldPropertySymbol: FirPropertySymbol,
): FirResolvedNamedReference {
    val calleeReference = calleeReference
    requireWithAttachment(
        calleeReference is FirResolvedNamedReference,
        { "Unexpected callee reference: ${calleeReference::class.simpleName}" },
    ) {
        withFirSymbolEntry("oldProperty", oldPropertySymbol)
        withFirSymbolEntry("newProperty", newPropertySymbol)
        withFirEntry("calleeReference", calleeReference)
    }

    return calleeReference
}

/**
 * To cover `propertyRef` function
 *
 * @see org.jetbrains.kotlin.fir.builder.generateAccessorsByDelegate
 */
fun rebindPropertyRef(
    expression: FirExpression,
    newPropertySymbol: FirPropertySymbol,
    oldPropertySymbol: FirPropertySymbol,
) {
    requireWithAttachment(
        expression is FirCallableReferenceAccess,
        { "Unexpected second argument: ${expression::class.simpleName}" },
    ) {
        withFirSymbolEntry("newTarget", newPropertySymbol)
        withFirSymbolEntry("oldTarget", oldPropertySymbol)
        withFirEntry("expression", expression)
    }

    val calleeReference = expression.resolvedCalleeReference(newPropertySymbol = newPropertySymbol, oldPropertySymbol = oldPropertySymbol)
    val resolvedPropertySymbol = calleeReference.resolvedSymbol
    requireWithAttachment(
        resolvedPropertySymbol == oldPropertySymbol,
        { "Unexpected symbol: ${resolvedPropertySymbol::class.simpleName}" },
    ) {
        withFirEntry("expression", expression)
        withFirSymbolEntry("actualOldProperty", resolvedPropertySymbol)
        withFirSymbolEntry("expectedOldProperty", oldPropertySymbol)
        withFirSymbolEntry("newProperty", newPropertySymbol)
    }

    expression.replaceCalleeReference(buildResolvedNamedReference {
        source = calleeReference.source
        name = calleeReference.name
        resolvedSymbol = newPropertySymbol
    })

    expression.replaceTypeArguments(newPropertySymbol.fir.typeParameters.map {
        buildTypeProjectionWithVariance {
            source = expression.source
            variance = Variance.INVARIANT
            typeRef = buildResolvedTypeRef {
                coneType = ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), false)
            }
        }
    })
}

/**
 * To cover `delegateAccess` function
 *
 * @see org.jetbrains.kotlin.fir.builder.generateAccessorsByDelegate
 */
fun rebindDelegateAccess(expression: FirExpression?, newPropertySymbol: FirPropertySymbol, oldPropertySymbol: FirPropertySymbol) {
    requireWithAttachment(
        expression is FirPropertyAccessExpression,
        { "Unexpected delegate accessor expression: ${expression?.let { it::class.simpleName }}" },
    ) {
        withFirSymbolEntry("newTarget", newPropertySymbol)
        withFirSymbolEntry("oldTarget", oldPropertySymbol)
        expression?.let { withFirEntry("expression", it) }
    }

    val delegateFieldReference = expression.calleeReference
    requireWithAttachment(
        delegateFieldReference is FirDelegateFieldReference,
        { "Unexpected callee reference: ${delegateFieldReference::class.simpleName}" },
    ) {
        withFirSymbolEntry("newTarget", newPropertySymbol)
        withFirSymbolEntry("oldTarget", oldPropertySymbol)
        withFirEntry("delegateFieldReference", delegateFieldReference)
    }

    requireWithAttachment(
        delegateFieldReference.resolvedSymbol == oldPropertySymbol.delegateFieldSymbol,
        { "Unexpected delegate field symbol" }
    ) {
        withFirSymbolEntry("newTarget", newPropertySymbol)
        withFirSymbolEntry("oldTarget", oldPropertySymbol)
        withFirSymbolEntry("field", delegateFieldReference.resolvedSymbol)
    }

    expression.replaceCalleeReference(buildDelegateFieldReference {
        source = delegateFieldReference.source
        resolvedSymbol = newPropertySymbol.delegateFieldSymbol ?: errorWithAttachment("Delegate field is missing") {
            withFirSymbolEntry("newTarget", newPropertySymbol)
            withFirSymbolEntry("oldTarget", oldPropertySymbol)
        }
    })

    expression.dispatchReceiver?.let {
        rebindThisRef(
            expression = it,
            newTarget = newPropertySymbol,
            oldTarget = oldPropertySymbol,
            canHavePropertySymbolAsThisReference = false,
        )
    }
}

fun calculateLazyInitializerForEnumEntry(designation: FirDesignation) {
    val enumEntry = designation.target as FirEnumEntry
    require(enumEntry.initializer is FirLazyExpression)

    val newEnumEntry = revive<FirEnumEntry>(designation)
    enumEntry.replaceInitializer(newEnumEntry.initializer)
}

fun calculateLazyBodyForAnonymousInitializer(designation: FirDesignation) {
    val initializer = designation.target as FirAnonymousInitializer
    require(initializer.body is FirLazyBlock)

    val newInitializer = revive<FirAnonymousInitializer>(designation)
    initializer.replaceBody(newInitializer.body)
}

fun needCalculatingLazyBodyForConstructor(firConstructor: FirConstructor): Boolean {
    if (needCalculatingLazyBodyForFunction(firConstructor) || firConstructor.delegatedConstructor is FirLazyDelegatedConstructorCall) {
        return true
    }
    val delegatedConstructor = firConstructor.delegatedConstructor
    if (delegatedConstructor is FirMultiDelegatedConstructorCall) {
        for (delegated in delegatedConstructor.delegatedConstructorCalls) {
            if (delegated is FirLazyDelegatedConstructorCall) {
                return true
            }
        }
    }
    return false
}

fun calculateLazyBodiesForField(designation: FirDesignation) {
    val field = designation.target as FirField
    require(field.initializer is FirLazyExpression)

    // 'designation.path.last()' cannot be used here, as for dangling files designation target may be in a different file
    val psi = field.psi?.getStrictParentOfType<KtClassOrObject>()

    val newField = revive<FirField>(designation, psi)
    field.replaceInitializer(newField.initializer)
}

fun needCalculatingLazyBodyForFunction(firFunction: FirFunction): Boolean {
    return firFunction.body is FirLazyBlock || firFunction.valueParameters.any { it.defaultValue is FirLazyExpression }
}

fun needCalculatingLazyBodyForProperty(firProperty: FirProperty): Boolean =
    firProperty.getter?.let { needCalculatingLazyBodyForFunction(it) } == true
            || firProperty.setter?.let { needCalculatingLazyBodyForFunction(it) } == true
            || firProperty.initializer is FirLazyExpression
            || firProperty.delegate is FirLazyExpression
            || firProperty.getExplicitBackingField()?.initializer is FirLazyExpression

fun calculateLazyBodyForCodeFragment(designation: FirDesignation) {
    val codeFragment = designation.target as FirCodeFragment
    require(codeFragment.block is FirLazyBlock)

    val newCodeFragment = revive<FirCodeFragment>(designation)
    codeFragment.replaceBlock(newCodeFragment.block)
}

/**
 * This object is supposed to be used only for tests.
 *
 * @see LazyAnnotationCalculatorVisitor
 */
object RecursiveLazyAnnotationCalculatorVisitor : RecursiveNonLocalAnnotationVisitor<FirSession>() {
    override fun processAnnotation(annotation: FirAnnotation, data: FirSession) {
        calculateAnnotationCallIfNeeded(annotation, data)
    }
}

/**
 * Calculates all non-local lazy annotations on a provided declaration.
 */
object LazyAnnotationCalculatorVisitor : NonLocalAnnotationVisitor<FirSession>() {
    override fun processAnnotation(annotation: FirAnnotation, data: FirSession) {
        calculateAnnotationCallIfNeeded(annotation, data)
    }
}

fun calculateAnnotationCallIfNeeded(annotation: FirAnnotation, session: FirSession) {
    if (annotation !is FirAnnotationCall || !FirLazyBodiesCalculator.needCalculatingAnnotationCall(annotation)) return

    val newArgumentList = FirLazyBodiesCalculator.calculateLazyArgumentsForAnnotation(annotation, session)
    annotation.replaceArgumentList(newArgumentList)
}

object FirAllLazyBodiesCalculatorTransformer : FirLazyBodiesCalculatorTransformer() {
    override fun <E : FirElement> transformElement(element: E, data: PersistentList<FirDeclaration>): E {
        return recursiveTransformation(element, data)
    }
}

object FirTargetLazyBodiesCalculatorTransformer : FirLazyBodiesCalculatorTransformer()

sealed class FirLazyBodiesCalculatorTransformer : FirTransformer<PersistentList<FirDeclaration>>() {
    override fun <E : FirElement> transformElement(element: E, data: PersistentList<FirDeclaration>): E = element

    override fun transformField(field: FirField, data: PersistentList<FirDeclaration>): FirStatement {
        if (field.initializer is FirLazyExpression) {
            val designation = FirDesignation(data, field)
            calculateLazyBodiesForField(designation)
        }

        return field
    }

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: PersistentList<FirDeclaration>,
    ): FirSimpleFunction {
        if (needCalculatingLazyBodyForFunction(simpleFunction)) {
            val designation = FirDesignation(data, simpleFunction)
            calculateLazyBodiesForFunction(designation)
        }

        return simpleFunction
    }

    override fun transformConstructor(
        constructor: FirConstructor,
        data: PersistentList<FirDeclaration>,
    ): FirConstructor {
        if (needCalculatingLazyBodyForConstructor(constructor)) {
            val designation = FirDesignation(data, constructor)
            calculateLazyBodyForConstructor(designation)
        }

        return constructor
    }

    override fun transformErrorPrimaryConstructor(
        errorPrimaryConstructor: FirErrorPrimaryConstructor,
        data: PersistentList<FirDeclaration>,
    ) = transformConstructor(errorPrimaryConstructor, data)

    override fun transformProperty(property: FirProperty, data: PersistentList<FirDeclaration>): FirProperty {
        if (needCalculatingLazyBodyForProperty(property)) {
            val designation = FirDesignation(data, property)
            calculateLazyBodyForProperty(designation)
        }

        return property
    }

    override fun transformEnumEntry(enumEntry: FirEnumEntry, data: PersistentList<FirDeclaration>): FirStatement {
        if (enumEntry.initializer is FirLazyExpression) {
            val designation = FirDesignation(data, enumEntry)
            calculateLazyInitializerForEnumEntry(designation)
        }

        return enumEntry
    }

    override fun transformAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer,
        data: PersistentList<FirDeclaration>,
    ): FirAnonymousInitializer {
        if (anonymousInitializer.body is FirLazyBlock) {
            val designation = FirDesignation(data, anonymousInitializer)
            calculateLazyBodyForAnonymousInitializer(designation)
        }

        return anonymousInitializer
    }

    override fun transformCodeFragment(codeFragment: FirCodeFragment, data: PersistentList<FirDeclaration>): FirCodeFragment {
        if (codeFragment.block is FirLazyBlock) {
            val designation = FirDesignation(data, codeFragment)
            calculateLazyBodyForCodeFragment(designation)
        }

        return codeFragment
    }
}

fun <E : FirElement> FirTransformer<PersistentList<FirDeclaration>>.recursiveTransformation(
    element: E,
    data: PersistentList<FirDeclaration>,
): E {
    if (element is FirFile || element is FirScript || element is FirRegularClass) {
        val newList = data.add(element as FirDeclaration)
        element.forEachDeclaration {
            it.transformSingle(this, newList)
        }

        element.transformChildren(this, newList)
    }

    return element
}

@OptIn(ExperimentalContracts::class)
fun needCalculatingLazyContractsForFunction(function: FirFunction): Boolean {
    contract {
        returns(true) implies (function is FirContractDescriptionOwner)
    }

    if (function !is FirContractDescriptionOwner) return false

    val contractDescription = function.contractDescription
    return when (contractDescription) {
        is FirRawContractDescription -> contractDescription.rawEffects.any { it is FirLazyExpression }

        // Q: Why is it null?
        // A: There is an ambiguity between `null` and `FirLegacyRawContractDescription` as during PSI2FIR phase we cannot check the body
        // to set up the description properly.
        // So, potentially, all functions without `FirRawContractDescription` may have a contract.
        null, is FirLegacyRawContractDescription -> function.body is FirLazyBlock

        is FirErrorContractDescription, is FirResolvedContractDescription -> errorWithAttachment("Unexpected contract description type: ${contractDescription::class.simpleName}") {
            withFirEntry("function", function)
        }
    }
}

fun needCalculatingLazyContractsForProperty(property: FirProperty): Boolean {
    return property.getter?.let(::needCalculatingLazyContractsForFunction) == true ||
            property.setter?.let(::needCalculatingLazyContractsForFunction) == true
}

object FirAllLazyContractsCalculatorTransformer : FirLazyContractsCalculatorTransformer() {
    override fun <E : FirElement> transformElement(element: E, data: PersistentList<FirDeclaration>): E {
        return recursiveTransformation(element, data)
    }
}

fun calculateLazyContractsForFunction(designation: FirDesignation) {
    val function = designation.target as FirFunction
    require(needCalculatingLazyContractsForFunction(function))

    val newFunction = revive<FirFunction>(designation, function.originalPsi)
    requireWithAttachment(newFunction is FirContractDescriptionOwner, { "Unexpected function type: ${newFunction::class.simpleName}" }) {
        withFirEntry("originalFunction", function)
        withFirEntry("newFunction", newFunction)
    }

    replaceLazyContracts(target = function, copy = newFunction)
}

fun calculateLazyContractsForProperty(designation: FirDesignation) {
    val property = designation.target as FirProperty
    require(needCalculatingLazyContractsForProperty(property))

    val newProperty = revive<FirProperty>(designation, property.originalPsi)
    property.getter?.let { getter ->
        val newGetter = newProperty.getter!!
        replaceLazyContracts(target = getter, copy = newGetter)
    }

    property.setter?.let { setter ->
        val newSetter = newProperty.setter!!
        replaceLazyContracts(target = setter, copy = newSetter)
    }
}

fun <F> replaceLazyContracts(target: F, copy: F) where F : FirFunction, F : FirContractDescriptionOwner {
    val contractDescription = copy.contractDescription
    target.replaceContractDescription(contractDescription)

    if (contractDescription is FirLegacyRawContractDescription) {
        replaceLazyBody(target = target, copy = copy, isContractResolved = false)
    }
}

object FirTargetLazyContractsCalculatorTransformer : FirLazyContractsCalculatorTransformer()

sealed class FirLazyContractsCalculatorTransformer : FirTransformer<PersistentList<FirDeclaration>>() {
    override fun <E : FirElement> transformElement(element: E, data: PersistentList<FirDeclaration>): E = element

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: PersistentList<FirDeclaration>,
    ): FirSimpleFunction {
        if (needCalculatingLazyContractsForFunction(simpleFunction)) {
            val designation = FirDesignation(data, simpleFunction)
            calculateLazyContractsForFunction(designation)
        }

        return simpleFunction
    }

    override fun transformConstructor(
        constructor: FirConstructor,
        data: PersistentList<FirDeclaration>,
    ): FirConstructor {
        if (needCalculatingLazyContractsForFunction(constructor)) {
            val designation = FirDesignation(data, constructor)
            calculateLazyContractsForFunction(designation)
        }

        return constructor
    }

    override fun transformProperty(property: FirProperty, data: PersistentList<FirDeclaration>): FirProperty {
        if (needCalculatingLazyContractsForProperty(property)) {
            val designation = FirDesignation(data, property)
            calculateLazyContractsForProperty(designation)
        }

        return property
    }
}
