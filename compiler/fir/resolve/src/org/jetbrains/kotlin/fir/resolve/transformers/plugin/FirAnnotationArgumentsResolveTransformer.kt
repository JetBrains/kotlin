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
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose

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
    ): CompositeTransformResult<FirStatement> {
        return wrappedDelegateExpression.compose()
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        return regularClass.transformAnnotations(this, data).transformDeclarations(this, data).compose()
    }

    override fun transformAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer,
        data: ResolutionMode
    ): CompositeTransformResult<FirDeclaration> {
        return anonymousInitializer.compose()
    }

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: ResolutionMode
    ): CompositeTransformResult<FirSimpleFunction> {
        return simpleFunction.transformAnnotations(this, data).compose()
    }

    override fun transformConstructor(constructor: FirConstructor, data: ResolutionMode): CompositeTransformResult<FirDeclaration> {
        return constructor.transformAnnotations(this, data).compose()
    }

    override fun transformValueParameter(valueParameter: FirValueParameter, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        return valueParameter.transformAnnotations(this, data).compose()
    }

    override fun transformProperty(property: FirProperty, data: ResolutionMode): CompositeTransformResult<FirProperty> {
        property.transformAnnotations(this, data)
        property.transformGetter(this, data)
        property.transformSetter(this, data)
        return property.compose()
    }

    override fun transformPropertyAccessor(
        propertyAccessor: FirPropertyAccessor,
        data: ResolutionMode
    ): CompositeTransformResult<FirDeclaration> {
        propertyAccessor.transformAnnotations(this, data)
        return propertyAccessor.compose()
    }
}

private class FirExpressionsResolveTransformerForSpecificAnnotations(
    transformer: FirBodyResolveTransformer,
    private val annotations: Set<AnnotationFqn>
) : FirExpressionsResolveTransformer(transformer) {
    private var annotationArgumentsMode: Boolean = false

    override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        if (annotationArgumentsMode) {
            return resolveAnnotationCall(annotationCall, FirAnnotationResolveStatus.PartiallyResolved)
        }

        annotationCall.transformAnnotationTypeRef(transformer, data)
        val classId = annotationCall.annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.lookupTag?.classId
            ?: return annotationCall.compose()
        if (classId.asSingleFqName() !in annotations) {
            return annotationCall.compose()
        }
        annotationArgumentsMode = true
        return resolveAnnotationCall(annotationCall, FirAnnotationResolveStatus.PartiallyResolved).also {
            annotationArgumentsMode = false
        }
    }

    override fun transformExpression(expression: FirExpression, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        return expression.compose()
    }

    override fun FirQualifiedAccessExpression.isAcceptableResolvedQualifiedAccess(): Boolean {
        return calleeReference !is FirErrorNamedReference
    }

    override fun transformFunctionCall(functionCall: FirFunctionCall, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        return functionCall.compose()
    }

    override fun transformBlock(block: FirBlock, data: ResolutionMode): CompositeTransformResult<FirStatement> {
        return block.compose()
    }

    override fun transformThisReceiverExpression(
        thisReceiverExpression: FirThisReceiverExpression,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        return thisReceiverExpression.compose()
    }

    override fun transformComparisonExpression(
        comparisonExpression: FirComparisonExpression,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        return comparisonExpression.compose()
    }

    override fun transformTypeOperatorCall(
        typeOperatorCall: FirTypeOperatorCall,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        return typeOperatorCall.compose()
    }

    override fun transformCheckNotNullCall(
        checkNotNullCall: FirCheckNotNullCall,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        return checkNotNullCall.compose()
    }

    override fun transformBinaryLogicExpression(
        binaryLogicExpression: FirBinaryLogicExpression,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        return binaryLogicExpression.compose()
    }

    override fun transformVariableAssignment(
        variableAssignment: FirVariableAssignment,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        return variableAssignment.compose()
    }

    override fun transformCallableReferenceAccess(
        callableReferenceAccess: FirCallableReferenceAccess,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        return callableReferenceAccess.compose()
    }

    override fun transformDelegatedConstructorCall(
        delegatedConstructorCall: FirDelegatedConstructorCall,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        return delegatedConstructorCall.compose()
    }

    override fun transformAugmentedArraySetCall(
        augmentedArraySetCall: FirAugmentedArraySetCall,
        data: ResolutionMode
    ): CompositeTransformResult<FirStatement> {
        return augmentedArraySetCall.compose()
    }
}
