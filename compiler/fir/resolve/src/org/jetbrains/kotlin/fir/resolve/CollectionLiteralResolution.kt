/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.declarations.processAllDeclaredCallables
import org.jetbrains.kotlin.fir.declarations.utils.isOperator
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.expressions.builder.buildResolvedQualifier
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.calls.ConeAtomWithCandidate
import org.jetbrains.kotlin.fir.resolve.calls.ConeCollectionLiteralAtom
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

abstract class CollectionLiteralResolutionStrategy(protected val context: ResolutionContext) {
    protected val components: BodyResolveComponents get() = context.bodyResolveComponents

    fun resolveCollectionLiteral(
        collectionLiteralAtom: ConeCollectionLiteralAtom,
        topLevelCandidate: Candidate,
        expectedType: FirRegularClassSymbol?,
    ): FirFunctionCall? {
        var call = prepareRawCall(collectionLiteralAtom.expression, expectedType) ?: return null
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

    internal abstract fun declaresOperatorOf(expectedType: FirRegularClassSymbol): Boolean

    protected abstract fun prepareRawCall(
        collectionLiteral: FirCollectionLiteral,
        expectedClass: FirRegularClassSymbol?
    ): FirFunctionCall?
}

private class CollectionLiteralResolutionStrategyThroughCompanion(context: ResolutionContext) :
    CollectionLiteralResolutionStrategy(context) {

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

    override fun declaresOperatorOf(expectedType: FirRegularClassSymbol): Boolean {
        return expectedType.companionObjectIfDefinedOperatorOf != null
    }

    override fun prepareRawCall(
        collectionLiteral: FirCollectionLiteral,
        expectedClass: FirRegularClassSymbol?
    ): FirFunctionCall? {
        val companion = expectedClass?.companionObjectIfDefinedOperatorOf ?: return null

        val functionCall = buildFunctionCall {
            explicitReceiver =
                companion.toImplicitResolvedQualifierReceiver(
                    components,
                    collectionLiteral.source?.fakeElement(KtFakeSourceElementKind.DesugaredReceiverForOperatorOfCall),
                )
            source = collectionLiteral.source
            calleeReference = buildSimpleNamedReference {
                source = collectionLiteral.source?.fakeElement(KtFakeSourceElementKind.CalleeReferenceForOperatorOfCall)
                name = OperatorNameConventions.OF
            }
            argumentList = collectionLiteral.argumentList
            origin = FirFunctionCallOrigin.Operator
        }

        return functionCall
    }
}

private class CollectionLiteralResolutionStrategyForStdlibType(context: ResolutionContext) : CollectionLiteralResolutionStrategy(context) {
    override fun declaresOperatorOf(expectedType: FirRegularClassSymbol): Boolean {
        return toCollectionOfFactoryPackageAndName(expectedType, context.session) != null
    }

    override fun prepareRawCall(
        collectionLiteral: FirCollectionLiteral,
        expectedClass: FirRegularClassSymbol?,
    ): FirFunctionCall? {
        if (expectedClass == null) return null
        val (packageName, functionName) = toCollectionOfFactoryPackageAndName(expectedClass, context.session) ?: return null

        return buildFunctionCall {
            explicitReceiver = buildResolvedQualifier {
                packageFqName = packageName
                source = collectionLiteral.source?.fakeElement(KtFakeSourceElementKind.DesugaredReceiverForOperatorOfCall)
                resolvedToCompanionObject = false
            }.apply {
                setTypeOfQualifier(components)
            }
            source = collectionLiteral.source
            calleeReference = buildSimpleNamedReference {
                source = collectionLiteral.source?.fakeElement(KtFakeSourceElementKind.CalleeReferenceForOperatorOfCall)
                name = functionName
            }
            argumentList = collectionLiteral.argumentList
        }
    }

}

class ErrorCollectionLiteralResolutionStrategy(context: ResolutionContext) : CollectionLiteralResolutionStrategy(context) {
    override fun declaresOperatorOf(expectedType: FirRegularClassSymbol): Boolean = false

    override fun prepareRawCall(
        collectionLiteral: FirCollectionLiteral,
        expectedClass: FirRegularClassSymbol?,
    ): FirFunctionCall {
        return buildFunctionCall {
            source = collectionLiteral.source
            argumentList = collectionLiteral.argumentList
            calleeReference = buildSimpleNamedReference {
                source = collectionLiteral.source?.fakeElement(KtFakeSourceElementKind.CalleeReferenceForOperatorOfCall)
                name = SpecialNames.ERROR_NAME_FOR_COLLECTION_LITERAL_CALL
            }
        }
    }
}

context(context: ResolutionContext)
fun <T : Any> tryAllCLResolutionStrategies(attempt: CollectionLiteralResolutionStrategy.() -> T?): T? {
    CollectionLiteralResolutionStrategyThroughCompanion(context).attempt()?.let { return it }
    return CollectionLiteralResolutionStrategyForStdlibType(context).attempt()
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

/**
 * For Kotlin class:
 *  There is a companion && in this companion at least one operator `of` is declared.
 *  If all the `of` operators are deprecated with `level=HIDDEN`, the class must be considered as not-having `of` operator
 *   ([KT-83165](https://youtrack.jetbrains.com/issue/KT-83165)).
 *  If the overloads of `of` are not visible from the call-site, the class must be considered as not-having `of` operator
 *   ([KT-84072](https://youtrack.jetbrains.com/issue/KT-84072)).
 *
 * For Java class:
 *  Static `of` with vararg is declared which fulfills all the restrictions on `of` (KT-80494).
 *
 * Additionally:
 *  `List`, `Set`, `MutableList`, `MutableSet`, `Sequence`, `Array`, primitive arrays, and unsigned arrays all declare operator `of`.
 *  ([KT-81722](https://youtrack.jetbrains.com/issue/KT-81722) for stdlib support).
 */
context(resolutionContext: ResolutionContext)
fun FirRegularClassSymbol.declaresOperatorOf(): Boolean {
    return tryAllCLResolutionStrategies {
        if (declaresOperatorOf(this@declaresOperatorOf)) true
        else null
    } ?: false
}

context(context: ResolutionContext)
fun Collection<FirRegularClassSymbol>.chooseSingleClassFromIntersectionComponents(): FirRegularClassSymbol? {
    return firstOrNull { candidate ->
        all { other ->
            candidate.fir.isSubclassOf(other.toLookupTag(), context.session, isStrict = false)
        }
    }
}
