/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.extensions.AnnotationFqn
import org.jetbrains.kotlin.fir.extensions.registeredPluginAnnotations
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.BodyResolveContext
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirExpressionsResolveTransformer
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneTypeSafe

class FirAnnotationArgumentsResolveTransformer(
    session: FirSession,
    scopeSession: ScopeSession,
    outerBodyResolveContext: BodyResolveContext? = null
) : FirBodyResolveTransformer(
    session,
    FirResolvePhase.ARGUMENTS_OF_PLUGIN_ANNOTATIONS,
    implicitTypeOnly = false,
    scopeSession,
    outerBodyResolveContext = outerBodyResolveContext
) {
    override val expressionsTransformer: FirExpressionsResolveTransformer = FirExpressionsResolveTransformerForSpecificAnnotations(
        this,
        session.registeredPluginAnnotations.annotations
    )

    override val declarationsTransformer: FirDeclarationsResolveTransformer = FirDeclarationsResolveTransformerForArgumentAnnotations(this)
}

private class FirDeclarationsResolveTransformerForArgumentAnnotations(
    transformer: FirBodyResolveTransformer
) : FirDeclarationsResolveTransformer(transformer) {
    override fun transformWrappedDelegateExpression(
        wrappedDelegateExpression: FirWrappedDelegateExpression,
        data: ResolutionMode
    ): FirStatement {
        return wrappedDelegateExpression
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: ResolutionMode): FirStatement {
        return regularClass.transformAnnotations(this, data).transformDeclarations(this, data)
    }

    override fun transformAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer,
        data: ResolutionMode
    ): FirAnonymousInitializer {
        return anonymousInitializer
    }

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: ResolutionMode
    ): FirSimpleFunction {
        return simpleFunction.transformAnnotations(this, data)
    }

    override fun transformConstructor(constructor: FirConstructor, data: ResolutionMode): FirConstructor {
        return constructor.transformAnnotations(this, data)
    }

    override fun transformValueParameter(valueParameter: FirValueParameter, data: ResolutionMode): FirStatement {
        return valueParameter.transformAnnotations(this, data)
    }

    override fun transformProperty(property: FirProperty, data: ResolutionMode): FirProperty {
        property.transformAnnotations(this, data)
        property.transformGetter(this, data)
        property.transformSetter(this, data)
        return property
    }

    override fun transformPropertyAccessor(
        propertyAccessor: FirPropertyAccessor,
        data: ResolutionMode
    ): FirPropertyAccessor {
        propertyAccessor.transformAnnotations(this, data)
        return propertyAccessor
    }
}

private class FirExpressionsResolveTransformerForSpecificAnnotations(
    transformer: FirBodyResolveTransformer,
    private val annotations: Set<AnnotationFqn>
) : FirExpressionsResolveTransformer(transformer) {
    private var annotationArgumentsMode: Boolean = false

    override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: ResolutionMode): FirStatement {
        if (annotationArgumentsMode) {
            return resolveAnnotationCall(annotationCall, FirAnnotationResolveStatus.PartiallyResolved)
        }

        annotationCall.transformAnnotationTypeRef(transformer, data)
        val classId = annotationCall.annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.lookupTag?.classId
            ?: return annotationCall
        if (classId.asSingleFqName() !in annotations) {
            return annotationCall
        }
        annotationArgumentsMode = true
        return resolveAnnotationCall(annotationCall, FirAnnotationResolveStatus.PartiallyResolved).also {
            annotationArgumentsMode = false
        }
    }

    override fun transformExpression(expression: FirExpression, data: ResolutionMode): FirStatement {
        return expression
    }

    override fun FirQualifiedAccessExpression.isAcceptableResolvedQualifiedAccess(): Boolean {
        return calleeReference !is FirErrorNamedReference
    }

    override fun transformFunctionCall(functionCall: FirFunctionCall, data: ResolutionMode): FirStatement {
        return functionCall
    }

    override fun transformBlock(block: FirBlock, data: ResolutionMode): FirStatement {
        return block
    }

    override fun transformThisReceiverExpression(
        thisReceiverExpression: FirThisReceiverExpression,
        data: ResolutionMode
    ): FirStatement {
        return thisReceiverExpression
    }

    override fun transformComparisonExpression(
        comparisonExpression: FirComparisonExpression,
        data: ResolutionMode
    ): FirStatement {
        return comparisonExpression
    }

    override fun transformTypeOperatorCall(
        typeOperatorCall: FirTypeOperatorCall,
        data: ResolutionMode
    ): FirStatement {
        return typeOperatorCall
    }

    override fun transformCheckNotNullCall(
        checkNotNullCall: FirCheckNotNullCall,
        data: ResolutionMode
    ): FirStatement {
        return checkNotNullCall
    }

    override fun transformBinaryLogicExpression(
        binaryLogicExpression: FirBinaryLogicExpression,
        data: ResolutionMode
    ): FirStatement {
        return binaryLogicExpression
    }

    override fun transformVariableAssignment(
        variableAssignment: FirVariableAssignment,
        data: ResolutionMode
    ): FirStatement {
        return variableAssignment
    }

    override fun transformCallableReferenceAccess(
        callableReferenceAccess: FirCallableReferenceAccess,
        data: ResolutionMode
    ): FirStatement {
        return callableReferenceAccess
    }

    override fun transformDelegatedConstructorCall(
        delegatedConstructorCall: FirDelegatedConstructorCall,
        data: ResolutionMode
    ): FirStatement {
        return delegatedConstructorCall
    }

    override fun transformAugmentedArraySetCall(
        augmentedArraySetCall: FirAugmentedArraySetCall,
        data: ResolutionMode
    ): FirStatement {
        return augmentedArraySetCall
    }
}
