/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.declarations.processAllDeclaredCallables
import org.jetbrains.kotlin.fir.declarations.utils.isOperator
import org.jetbrains.kotlin.fir.expressions.FirCollectionLiteral
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirFunctionCallOrigin
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.expressions.builder.buildResolvedQualifier
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.calls.ConeAtomWithCandidate
import org.jetbrains.kotlin.fir.resolve.calls.ConeCollectionLiteralAtom
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousObjectSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeCapturedType
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeDefinitelyNotNullType
import org.jetbrains.kotlin.fir.types.ConeDynamicType
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.ConeIntegerLiteralType
import org.jetbrains.kotlin.fir.types.ConeIntersectionType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeLookupTagBasedType
import org.jetbrains.kotlin.fir.types.ConeStubType
import org.jetbrains.kotlin.fir.types.ConeTypeVariableType
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

abstract class CollectionLiteralResolver(protected val context: ResolutionContext) {
    protected val components: BodyResolveComponents get() = context.bodyResolveComponents

    fun resolveCollectionLiteral(
        collectionLiteralAtom: ConeCollectionLiteralAtom,
        topLevelCandidate: Candidate,
        expectedType: FirRegularClassSymbol?,
    ): FirFunctionCall? {
        var call = prepareRawCall(collectionLiteralAtom.expression, topLevelCandidate, expectedType) ?: return null
        call = components.callResolver.resolveCallAndSelectCandidate(call, ResolutionMode.ContextDependent, topLevelCandidate)
        call = context.bodyResolveComponents.callCompleter.completeCall(call, ResolutionMode.ContextDependent)

        return when (val calleeRef = call.calleeReference) {
            is FirNamedReferenceWithCandidate -> {
                topLevelCandidate.system.replaceContentWith(calleeRef.candidate.system.currentStorage())
                collectionLiteralAtom.subAtom = ConeAtomWithCandidate(collectionLiteralAtom.expression, calleeRef.candidate)
                call
            }
            else -> call
        }
    }

    protected abstract fun prepareRawCall(
        collectionLiteral: FirCollectionLiteral,
        topLevelCandidate: Candidate,
        expectedClass: FirRegularClassSymbol?
    ): FirFunctionCall?
}

class CollectionLiteralResolverThroughCompanion(context: ResolutionContext) : CollectionLiteralResolver(context) {

    private fun FirCallableSymbol<*>.canBeMainOperatorOfOverload(outerClass: FirRegularClassSymbol): Boolean {
        return when {
            this !is FirNamedFunctionSymbol -> false
            !isOperator || name != OperatorNameConventions.OF || valueParameterSymbols.none { it.isVararg } -> false
            else -> when (val returnType = context.returnTypeCalculator.tryCalculateReturnType(this).coneType) {
                is ConeClassLikeType if returnType.fullyExpandedType(context.session).lookupTag == outerClass.toLookupTag() -> true
                is ConeErrorType -> true
                else -> false
            }
        }
    }

    /**
     * @return if there is a suitable operator `of` overload, companion object where it is defined
     */
    private val FirRegularClassSymbol.companionObjectIfDefinedOperatorOf: FirRegularClassSymbol?
        get() {
            val companionObjectSymbol = resolvedCompanionObjectSymbol ?: return null
            var overloadFound = false
            companionObjectSymbol.processAllDeclaredCallables(context.session) { declaration ->
                if (declaration.canBeMainOperatorOfOverload(this))
                    overloadFound = true
            }
            return overloadFound.ifTrue { companionObjectSymbol }
        }

    override fun prepareRawCall(
        collectionLiteral: FirCollectionLiteral,
        topLevelCandidate: Candidate,
        expectedClass: FirRegularClassSymbol?
    ): FirFunctionCall? {
        val companion = expectedClass?.companionObjectIfDefinedOperatorOf ?: return null

        val functionCall = buildFunctionCall {
            explicitReceiver =
                companion.toImplicitResolvedQualifierReceiver(
                    components,
                    collectionLiteral.source?.fakeElement(KtFakeSourceElementKind.CompanionObjectForOperatorOfCall),
                )
            source = collectionLiteral.source?.fakeElement(KtFakeSourceElementKind.OperatorOfCall)
            calleeReference = buildSimpleNamedReference {
                source = collectionLiteral.source
                name = OperatorNameConventions.OF
            }
            argumentList = collectionLiteral.argumentList
            origin = FirFunctionCallOrigin.Operator
        }

        return functionCall
    }
}

class CollectionLiteralResolverForStdlibType(context: ResolutionContext) : CollectionLiteralResolver(context) {
    override fun prepareRawCall(
        collectionLiteral: FirCollectionLiteral,
        topLevelCandidate: Candidate,
        expectedClass: FirRegularClassSymbol?,
    ): FirFunctionCall? {
        if (expectedClass == null) return null
        val (packageName, functionName) = toCollectionOfFactoryPackageAndName(expectedClass, context.session) ?: return null

        return buildFunctionCall {
            explicitReceiver = buildResolvedQualifier {
                packageFqName = packageName
                source = collectionLiteral.source
                resolvedToCompanionObject = false
            }.apply {
                setTypeOfQualifier(components)
            }
            source = collectionLiteral.source?.fakeElement(KtFakeSourceElementKind.OperatorOfCall)
            calleeReference = buildSimpleNamedReference {
                source = collectionLiteral.source
                name = functionName
            }
            argumentList = collectionLiteral.argumentList
        }
    }

}

fun ResolutionContext.runResolutionForDanglingCollectionLiteral(collectionLiteral: FirCollectionLiteral) {
    // If there are any diagnostics on the call that we miss, even better: we report `UNSUPPORTED_COLLECTION_LITERAL_TYPE` anyway.
    val fakeCall = bodyResolveComponents.syntheticCallGenerator
        .generateFakeCallForDanglingCollectionLiteral(collectionLiteral, this)
    val completedCall = bodyResolveComponents.callCompleter.completeCall(fakeCall, ResolutionMode.ContextIndependent)

    val newArgumentList = buildArgumentList {
        for (argument in completedCall.arguments) {
            check(argument is FirVarargArgumentsExpression) { "Arguments should me mapped to vararg" }
            arguments += argument.arguments
        }
    }

    collectionLiteral.replaceArgumentList(newArgumentList)
}

context(resolutionContext: ResolutionContext)
fun ConeKotlinType.getClassRepresentativeForCollectionLiteralResolution(): FirRegularClassSymbol? {
    return when (this) {
        is ConeFlexibleType -> lowerBound.getClassRepresentativeForCollectionLiteralResolution()
        is ConeCapturedType -> constructor.lowerType?.getClassRepresentativeForCollectionLiteralResolution()
        is ConeDefinitelyNotNullType -> {
            // very rarely, but still needed, because there might be an expected type of form `Captured(in SomeCollection?) & Any`
            original.getClassRepresentativeForCollectionLiteralResolution()
        }
        is ConeDynamicType,
        is ConeIntersectionType,
        is ConeStubType,
        is ConeTypeVariableType,
        is ConeIntegerLiteralType,
            -> null
        is ConeLookupTagBasedType ->
            when (val symbol = lookupTag.toSymbol()) {
                is FirTypeParameterSymbol, is FirAnonymousObjectSymbol, null -> null
                is FirRegularClassSymbol -> symbol
                is FirTypeAliasSymbol -> fullyExpandedType().getClassRepresentativeForCollectionLiteralResolution()
            }
    }
}
