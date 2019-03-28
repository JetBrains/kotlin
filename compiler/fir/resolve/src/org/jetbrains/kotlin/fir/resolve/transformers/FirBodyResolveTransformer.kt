/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import com.google.common.collect.LinkedHashMultimap
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReferenceImpl
import org.jetbrains.kotlin.fir.references.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.CallInfo
import org.jetbrains.kotlin.fir.resolve.calls.CallResolver
import org.jetbrains.kotlin.fir.resolve.calls.createFunctionConsumer
import org.jetbrains.kotlin.fir.resolve.calls.createVariableConsumer
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.addImportingScopes
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope
import org.jetbrains.kotlin.fir.scopes.impl.FirTopLevelDeclaredMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.withReplacedConeType
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirComputingImplicitTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirErrorTypeRefImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addIfNotNull

open class FirBodyResolveTransformer(val session: FirSession, val implicitTypeOnly: Boolean) : FirTransformer<Any?>() {

    val symbolProvider = session.service<FirSymbolProvider>()

    override fun <E : FirElement> transformElement(element: E, data: Any?): CompositeTransformResult<E> {
        @Suppress("UNCHECKED_CAST")
        return (element.transformChildren(this, data) as E).compose()
    }

    private var packageFqName = FqName.ROOT

    override fun transformFile(file: FirFile, data: Any?): CompositeTransformResult<FirFile> {
        packageFqName = file.packageFqName
        return withScopeCleanup(scopes) {
            scopes.addImportingScopes(file, session)
            scopes += FirTopLevelDeclaredMemberScope(file, session)
            super.transformFile(file, data)
        }
    }

    override fun transformImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: Any?): CompositeTransformResult<FirTypeRef> {
        if (data == null)
            return implicitTypeRef.compose()
        require(data is FirTypeRef)
        return data.compose()
    }

    override fun transformFunction(function: FirFunction, data: Any?): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup(localScopes) {
            localScopes += FirLocalScope()
            super.transformFunction(function, data)
        }
    }


    override fun transformValueParameter(valueParameter: FirValueParameter, data: Any?): CompositeTransformResult<FirDeclaration> {
        localScopes.lastOrNull()?.storeDeclaration(valueParameter)
        if (valueParameter.returnTypeRef is FirImplicitTypeRef) return valueParameter.compose() // TODO
        return super.transformValueParameter(valueParameter, valueParameter.returnTypeRef)
    }


    inline fun <T> withLabel(labelName: Name, type: ConeKotlinType, block: () -> T): T {
        labels.put(labelName, type)
        val result = block()
        labels.remove(labelName, type)
        return result
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Any?): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup(scopes) {
            val type = regularClass.defaultType()
            scopes.addIfNotNull(type.scope(session))
            withLabel(regularClass.name, type) {
                super.transformRegularClass(regularClass, data)
            }
        }
    }

    override fun transformTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: Any?): CompositeTransformResult<FirStatement> {
        val symbolProvider = session.service<FirSymbolProvider>()
        val resolved = super.transformTypeOperatorCall(typeOperatorCall, data).single
        when ((resolved as FirTypeOperatorCall).operation) {
            FirOperation.IS, FirOperation.NOT_IS -> {
                resolved.resultType = FirResolvedTypeRefImpl(
                    session,
                    null,
                    StandardClassIds.Boolean(symbolProvider).constructType(emptyArray(), isNullable = false),
                    false,
                    emptyList()
                )
            }
            FirOperation.AS -> {
                resolved.resultType = resolved.conversionTypeRef
            }
            FirOperation.SAFE_AS -> {
                resolved.resultType =
                    resolved.conversionTypeRef.withReplacedConeType(
                        session,
                        resolved.conversionTypeRef.coneTypeUnsafe().withNullability(ConeNullability.NULLABLE)
                    )
            }
            else -> error("Unknown type operator")
        }
        return resolved.compose()
    }

    protected inline fun <T> withScopeCleanup(scopes: MutableList<*>, crossinline l: () -> T): T {
        val sizeBefore = scopes.size
        val result = l()
        val size = scopes.size
        assert(size >= sizeBefore)
        repeat(size - sizeBefore) {
            scopes.let { it.removeAt(it.size - 1) }
        }
        return result
    }

    val scopes = mutableListOf<FirScope>()
    val localScopes = mutableListOf<FirLocalScope>()

    val labels = LinkedHashMultimap.create<Name, ConeKotlinType>()


    val jump = ReturnTypeCalculatorWithJump(session)

    private fun <T> storeTypeFromCallee(access: T) where T : FirQualifiedAccess, T : FirExpression {
        access.resultType =
            when (val newCallee = access.calleeReference) {
                is FirErrorNamedReference ->
                    FirErrorTypeRefImpl(session, access.psi, newCallee.errorReason)
                is FirResolvedCallableReference ->
                    jump.tryCalculateReturnType(newCallee.callableSymbol.firUnsafe())
                else -> return
            }
    }

    private fun <T : FirQualifiedAccess> transformCallee(qualifiedAccess: T): T {
        val callee = qualifiedAccess.calleeReference as? FirSimpleNamedReference ?: return qualifiedAccess

        qualifiedAccess.explicitReceiver?.visitNoTransform(this, null)

        val receiver = qualifiedAccess.explicitReceiver

        //val checkers = listOf(VariableApplicabilityChecker(callee.name))

        val info = CallInfo(true, receiver, 0)
        val resolver = CallResolver(jump, session)
        resolver.callInfo = info
        resolver.scopes = (scopes + localScopes).asReversed()

        val consumer = createVariableConsumer(
            session, callee.name, qualifiedAccess.explicitReceiver, qualifiedAccess.explicitReceiver?.resultType
        )
        val result = resolver.runTowerResolver(consumer)
        val successCandidates = result.successCandidates()
        val resultExpression = qualifiedAccess.transformCalleeReference(this, successCandidates) as T
        if (resultExpression is FirExpression) storeTypeFromCallee(resultExpression)
        return resultExpression
    }

    override fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: Any?
    ): CompositeTransformResult<FirStatement> {

        when (val callee = qualifiedAccessExpression.calleeReference) {
            is FirThisReference -> {

                val labelName = callee.labelName
                val types = if (labelName == null) labels.values() else labels[Name.identifier(labelName)]
                val type = types.lastOrNull() ?: ConeKotlinErrorType("Unresolved this@$labelName")
                qualifiedAccessExpression.resultType = FirResolvedTypeRefImpl(session, null, type, false, emptyList())
            }
            is FirSuperReference -> {
                qualifiedAccessExpression.resultType =
                    FirErrorTypeRefImpl(session, qualifiedAccessExpression.psi, "Unsupported: super type") //TODO

            }
            is FirResolvedCallableReference -> {
                if (qualifiedAccessExpression.typeRef !is FirResolvedTypeRef) {
                    qualifiedAccessExpression.resultType =
                        jump.tryCalculateReturnType(callee.callableSymbol.firUnsafe<FirCallableDeclaration>())
                }
            }
        }
        return transformCallee(qualifiedAccessExpression).compose()
    }

    override fun transformVariableAssignment(
        variableAssignment: FirVariableAssignment,
        data: Any?
    ): CompositeTransformResult<FirStatement> {
        variableAssignment.rValue.visitNoTransform(this, null)
        return transformCallee(variableAssignment).compose()
    }

    override fun transformFunctionCall(functionCall: FirFunctionCall, data: Any?): CompositeTransformResult<FirStatement> {

        val functionCall = functionCall.transformChildren(this, null) as FirFunctionCall

        if (functionCall.calleeReference !is FirSimpleNamedReference) return functionCall.compose()

        val name = functionCall.calleeReference.name

        val expectedTypeRef = data as FirTypeRef?

        val receiver = functionCall.explicitReceiver
        val arguments = functionCall.arguments


        val info = CallInfo(false, receiver, arguments.size)
        val resolver = CallResolver(jump, session)
        resolver.callInfo = info
        resolver.scopes = (scopes + localScopes).asReversed()



        val consumer = createFunctionConsumer(session, name, receiver, receiver?.resultType)
        val result = resolver.runTowerResolver(consumer)
        val successCandidates = result.successCandidates()

//        fun isInvoke()
//
//        val resultExpression =
//
//        when {
//            successCandidates.singleOrNull() as? ConeCallableSymbol -> {
//                FirFunctionCallImpl(functionCall.session, functionCall.psi, safe = functionCall.safe).apply {
//                    calleeReference =
//                        functionCall.calleeReference.transformSingle(this@FirBodyResolveTransformer, result.successCandidates())
//                    explicitReceiver =
//                        FirQualifiedAccessExpressionImpl(
//                            functionCall.session,
//                            functionCall.calleeReference.psi,
//                            functionCall.safe
//                        ).apply {
//                            calleeReference = createResolvedNamedReference(
//                                functionCall.calleeReference,
//                                result.variableChecker.successCandidates() as List<ConeCallableSymbol>
//                            )
//                            explicitReceiver = functionCall.explicitReceiver
//                        }
//                }
//            }
//            is ApplicabilityChecker -> {
//                functionCall.transformCalleeReference(this, result.successCandidates())
//            }
//            else -> functionCall
//        }
        val resultExpression = functionCall.transformCalleeReference(this, successCandidates)

        storeTypeFromCallee(resultExpression as FirFunctionCall)
        return resultExpression.compose()
    }

    private fun createResolvedNamedReference(namedReference: FirNamedReference, candidates: List<ConeCallableSymbol>): FirNamedReference {
        val name = namedReference.name
        return when (candidates.size) {
            0 -> FirErrorNamedReference(
                namedReference.session, namedReference.psi, "Unresolved name: $name"
            )
            1 -> FirResolvedCallableReferenceImpl(
                namedReference.session, namedReference.psi,
                name, candidates.single()
            )
            else -> FirErrorNamedReference(
                namedReference.session, namedReference.psi, "Ambiguity: $name, ${candidates.map { it.callableId }}"
            )
        }
    }

    override fun transformNamedReference(namedReference: FirNamedReference, data: Any?): CompositeTransformResult<FirNamedReference> {
        if (namedReference is FirErrorNamedReference || namedReference is FirResolvedCallableReference) return namedReference.compose()
        val referents = data as? List<ConeCallableSymbol> ?: return namedReference.compose()
        return createResolvedNamedReference(namedReference, referents).compose()
    }


    override fun transformBlock(block: FirBlock, data: Any?): CompositeTransformResult<FirStatement> {
        val block = super.transformBlock(block, data).single as FirBlock
        val statement = block.statements.lastOrNull()

        val resultExpression = when (statement) {
            is FirReturnExpression -> statement.result
            is FirExpression -> statement
            else -> null
        }
        (resultExpression?.resultType as? FirResolvedTypeRef)?.let { block.resultType = it }
        return block.compose()
    }

    private fun commonSuperType(types: List<FirTypeRef>): FirTypeRef? {
        return types.firstOrNull()
    }

    override fun transformWhenExpression(whenExpression: FirWhenExpression, data: Any?): CompositeTransformResult<FirStatement> {
        whenExpression.transformChildren(this, data)
        if (whenExpression.resultType !is FirResolvedTypeRef) {
            val type = commonSuperType(whenExpression.branches.mapNotNull {
                it.result.resultType
            })
            if (type != null) whenExpression.resultType = type
        }
        return whenExpression.compose()
    }

    override fun <T> transformConstExpression(constExpression: FirConstExpression<T>, data: Any?): CompositeTransformResult<FirStatement> {
        val expectedType = data as? FirTypeRef

        val kind = constExpression.kind
        if (expectedType is FirImplicitTypeRef || expectedType == null ||
            kind == IrConstKind.Null || kind == IrConstKind.Boolean || kind == IrConstKind.Char
        ) {
            val symbol = when (kind) {
                IrConstKind.Null -> StandardClassIds.Nothing(symbolProvider)
                IrConstKind.Boolean -> StandardClassIds.Boolean(symbolProvider)
                IrConstKind.Char -> StandardClassIds.Char(symbolProvider)
                IrConstKind.Byte -> StandardClassIds.Byte(symbolProvider)
                IrConstKind.Short -> StandardClassIds.Short(symbolProvider)
                IrConstKind.Int -> StandardClassIds.Int(symbolProvider)
                IrConstKind.Long -> StandardClassIds.Long(symbolProvider)
                IrConstKind.String -> StandardClassIds.String(symbolProvider)
                IrConstKind.Float -> StandardClassIds.Float(symbolProvider)
                IrConstKind.Double -> StandardClassIds.Double(symbolProvider)
            }

            val type = ConeClassTypeImpl(symbol.toLookupTag(), emptyArray(), isNullable = kind == IrConstKind.Null)

            constExpression.resultType = FirResolvedTypeRefImpl(session, null, type, false, emptyList())
        } else {
            constExpression.resultType = expectedType
        }


        return super.transformConstExpression(constExpression, data)
    }

    private var FirExpression.resultType: FirTypeRef
        get() = typeRef
        set(type) {
            replaceTypeRef(type)
        }


    override fun transformNamedFunction(namedFunction: FirNamedFunction, data: Any?): CompositeTransformResult<FirDeclaration> {
        if (namedFunction.returnTypeRef !is FirImplicitTypeRef && implicitTypeOnly) return namedFunction.compose()

        val receiverTypeRef = namedFunction.receiverTypeRef
        fun transform(): CompositeTransformResult<FirDeclaration> {
            localScopes.lastOrNull()?.storeDeclaration(namedFunction)
            return withScopeCleanup(scopes) {
                scopes.addIfNotNull(receiverTypeRef?.coneTypeSafe()?.scope(session))


                val result = super.transformNamedFunction(namedFunction, namedFunction.returnTypeRef).single as FirNamedFunction
                val body = result.body
                if (result.returnTypeRef is FirImplicitTypeRef && body != null) {
                    result.transformReturnTypeRef(this, body.resultType)
                    result
                } else {
                    result
                }.compose()
            }
        }

        return if (receiverTypeRef != null) {
            withLabel(namedFunction.name, receiverTypeRef.coneTypeUnsafe()) { transform() }
        } else {
            transform()
        }
    }

    override fun transformVariable(variable: FirVariable, data: Any?): CompositeTransformResult<FirDeclaration> {
        val variable = super.transformVariable(variable, variable.returnTypeRef).single as FirVariable
        val initializer = variable.initializer
        if (variable.returnTypeRef is FirImplicitTypeRef) {
            when {
                initializer != null -> {
                    variable.transformReturnTypeRef(
                        this,
                        when (val resultType = initializer.resultType) {
                            is FirImplicitTypeRef -> FirErrorTypeRefImpl(
                                session,
                                null,
                                "No result type for initializer"
                            )
                            else -> resultType
                        }
                    )
                }
                variable.delegate != null -> {
                    // TODO: type from delegate
                    variable.transformReturnTypeRef(
                        this,
                        FirErrorTypeRefImpl(
                            session,
                            null,
                            "Not supported: type from delegate"
                        )
                    )
                }
            }
        }
        if (variable !is FirProperty) {
            localScopes.lastOrNull()?.storeDeclaration(variable)
        }
        return variable.compose()
    }

    override fun transformProperty(property: FirProperty, data: Any?): CompositeTransformResult<FirDeclaration> {
        if (property.returnTypeRef !is FirImplicitTypeRef && implicitTypeOnly) return property.compose()
        return transformVariable(property, data)
    }

    override fun transformExpression(expression: FirExpression, data: Any?): CompositeTransformResult<FirStatement> {
        if (expression.resultType is FirImplicitTypeRef) {
            val type = FirErrorTypeRefImpl(session, expression.psi, "Type calculating for ${expression.render()} is not supported")
            expression.resultType = type
        }
        return super.transformExpression(expression, data)
    }

    fun <D> FirElement.visitNoTransform(transformer: FirTransformer<D>, data: D) {
        val result = this.transform(transformer, data)
        require(result.single === this) { "become ${result.single}: `${result.single.render()}`, was ${this}: `${this.render()}`" }
    }

}


class ReturnTypeCalculatorWithJump(val session: FirSession) : ReturnTypeCalculator {


    val storeType = object : FirTransformer<FirTypeRef>() {
        override fun <E : FirElement> transformElement(element: E, data: FirTypeRef): CompositeTransformResult<E> {
            return element.compose()
        }

        override fun transformImplicitTypeRef(
            implicitTypeRef: FirImplicitTypeRef,
            data: FirTypeRef
        ): CompositeTransformResult<FirTypeRef> {
            return data.compose()
        }
    }

    private fun cycleErrorType(declaration: FirTypedDeclaration): FirResolvedTypeRef? {
        if (declaration.returnTypeRef is FirComputingImplicitTypeRef) {
            declaration.transformReturnTypeRef(storeType, FirErrorTypeRefImpl(session, null, "cycle"))
            return declaration.returnTypeRef as FirResolvedTypeRef
        }
        return null
    }

    override fun tryCalculateReturnType(declaration: FirTypedDeclaration): FirResolvedTypeRef {

        if (declaration is FirValueParameter && declaration.returnTypeRef is FirImplicitTypeRef) {
            // TODO?
            declaration.transformReturnTypeRef(storeType, FirErrorTypeRefImpl(session, null, "Unsupported: implicit VP type"))
        }
        val returnTypeRef = declaration.returnTypeRef
        if (returnTypeRef is FirResolvedTypeRef) return returnTypeRef
        cycleErrorType(declaration)?.let { return it }
        require(declaration is FirCallableMemberDeclaration) { "${declaration::class}: ${declaration.render()}" }


        val symbol = declaration.symbol as ConeCallableSymbol
        val id = symbol.callableId

        val provider = session.service<FirProvider>()

        val file = provider.getFirCallableContainerFile(symbol)

        val outerClasses = generateSequence(id.classId) { classId ->
            classId.outerClassId
        }.mapTo(mutableListOf()) { provider.getFirClassifierByFqName(it) }

        if (file == null || outerClasses.any { it == null }) return FirErrorTypeRefImpl(
            session,
            null,
            "I don't know what todo"
        )

        declaration.transformReturnTypeRef(storeType, FirComputingImplicitTypeRef)

        val transformer = FirDesignatedBodyResolveTransformer(
            (listOf(file) + outerClasses.filterNotNull().asReversed() + listOf(declaration)).iterator(),
            file.session
        )

        file.transform(transformer, null)


        val newReturnTypeRef = declaration.returnTypeRef
        cycleErrorType(declaration)?.let { return it }
        require(newReturnTypeRef is FirResolvedTypeRef) { declaration.render() }
        return newReturnTypeRef
    }
}


class FirDesignatedBodyResolveTransformer(val designation: Iterator<FirElement>, session: FirSession) :
    FirBodyResolveTransformer(session, implicitTypeOnly = true) {

    override fun <E : FirElement> transformElement(element: E, data: Any?): CompositeTransformResult<E> {
        if (designation.hasNext()) {
            designation.next().visitNoTransform(this, data)
            return element.compose()
        }
        return super.transformElement(element, data)
    }
}


@Deprecated("It is temp", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("TODO(\"что-то нормальное\")"))
class FirImplicitTypeBodyResolveTransformerAdapter : FirTransformer<Nothing?>() {
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        val transformer = FirBodyResolveTransformer(file.session, implicitTypeOnly = true)
        return file.transform(transformer, null)
    }
}


@Deprecated("It is temp", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("TODO(\"что-то нормальное\")"))
class FirBodyResolveTransformerAdapter : FirTransformer<Nothing?>() {
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        val transformer = FirBodyResolveTransformer(file.session, implicitTypeOnly = false)
        return file.transform(transformer, null)
    }
}


inline fun <reified T : FirElement> ConeSymbol.firUnsafe(): T {
    require(this is FirBasedSymbol<*>) {
        "Not a fir based symbol: ${this}"
    }
    val fir = this.fir
    require(fir is T) {
        "Not an expected fir element type = ${T::class}, symbol = ${this}, fir = ${fir.renderWithType()}"
    }
    return fir
}

inline fun <reified T : FirElement> ConeSymbol.firSafeNullable(): T? {
    if (this !is FirBasedSymbol<*>) return null
    return fir as? T
}


interface ReturnTypeCalculator {
    fun tryCalculateReturnType(declaration: FirTypedDeclaration): FirResolvedTypeRef
}
