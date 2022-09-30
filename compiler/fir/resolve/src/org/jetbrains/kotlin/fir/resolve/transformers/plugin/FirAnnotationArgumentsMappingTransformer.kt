/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.BodyResolveContext
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirExpressionsResolveTransformer

open class FirAnnotationArgumentsMappingTransformer(
    session: FirSession,
    scopeSession: ScopeSession,
    resolvePhase: FirResolvePhase,
    outerBodyResolveContext: BodyResolveContext? = null
) : FirBodyResolveTransformer(
    session,
    resolvePhase,
    implicitTypeOnly = true,
    scopeSession,
    outerBodyResolveContext = outerBodyResolveContext
) {
    override val expressionsTransformer: FirExpressionsResolveTransformer =
        FirExpressionsResolveTransformer(this)

    override val declarationsTransformer: FirDeclarationsResolveTransformer =
        FirDeclarationsResolveTransformerForAnnotationArgumentsMapping(this)
}

private class FirDeclarationsResolveTransformerForAnnotationArgumentsMapping(
    transformer: FirBodyResolveTransformer
) : FirDeclarationsResolveTransformer(transformer) {
    override fun transformRegularClass(regularClass: FirRegularClass, data: ResolutionMode): FirStatement {
        regularClass.transformAnnotations(this, data)
        doTransformTypeParameters(regularClass)

        context.withContainingClass(regularClass) {
            return doTransformRegularClass(regularClass, data)
        }
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
        if (simpleFunction.bodyResolved) {
            return simpleFunction
        }

        doTransformTypeParameters(simpleFunction)
        dataFlowAnalyzer.enterFunction(simpleFunction)

        context.withSimpleFunction(simpleFunction, session) {
            simpleFunction
                .transformReturnTypeRef(transformer, data)
                .transformReceiverTypeRef(transformer, data)
                .transformValueParameters(transformer, data)
                .transformAnnotations(transformer, data)
        }

        dataFlowAnalyzer.exitFunction(simpleFunction)
        return simpleFunction
    }

    override fun transformConstructor(constructor: FirConstructor, data: ResolutionMode): FirConstructor {
        val owningClass = context.containerIfAny as? FirRegularClass
        doTransformTypeParameters(constructor)
        dataFlowAnalyzer.enterFunction(constructor)

        context.withConstructor(constructor) {
            constructor
                .transformAnnotations(transformer, data)
                .transformReceiverTypeRef(transformer, data)
                .transformReturnTypeRef(transformer, data)

            context.forConstructorParameters(constructor, owningClass, components) {
                constructor.transformValueParameters(transformer, data)
            }

            constructor.transformDelegatedConstructor(transformer, data)
        }

        dataFlowAnalyzer.exitFunction(constructor)
        return constructor
    }

    override fun transformValueParameter(valueParameter: FirValueParameter, data: ResolutionMode): FirStatement {
        dataFlowAnalyzer.enterValueParameter(valueParameter)

        context.withValueParameter(valueParameter, session) {
            valueParameter
                .transformAnnotations(transformer, data)
                .transformReturnTypeRef(transformer, data)
        }

        dataFlowAnalyzer.exitValueParameter(valueParameter)
        return valueParameter
    }

    override fun transformProperty(property: FirProperty, data: ResolutionMode): FirProperty {
        property.transformReceiverTypeRef(transformer, ResolutionMode.ContextIndependent)
        doTransformTypeParameters(property)
        dataFlowAnalyzer.enterProperty(property)

        context.withProperty(property) {
            property
                .transformAnnotations(transformer, data)
                .transformReceiverTypeRef(transformer, data)
                .transformReturnTypeRef(transformer, data)
                .transformGetter(transformer, data)
                .transformSetter(transformer, data)
        }

        dataFlowAnalyzer.exitProperty(property)
        return property
    }

    override fun transformPropertyAccessor(
        propertyAccessor: FirPropertyAccessor,
        data: ResolutionMode
    ): FirPropertyAccessor {
        propertyAccessor
            .transformValueParameters(transformer, data)
            .transformReturnTypeRef(transformer, data)
            .transformReceiverTypeRef(transformer, data)
            .transformReturnTypeRef(transformer, data)
            .transformAnnotations(transformer, data)
        return propertyAccessor
    }

    override fun transformDeclarationStatus(declarationStatus: FirDeclarationStatus, data: ResolutionMode): FirDeclarationStatus {
        return declarationStatus
    }

    override fun transformEnumEntry(enumEntry: FirEnumEntry, data: ResolutionMode): FirEnumEntry {
        context.forEnumEntry {
            enumEntry
                .transformAnnotations(transformer, data)
                .transformReceiverTypeRef(transformer, data)
                .transformReturnTypeRef(transformer, data)
                .transformTypeParameters(transformer, data)
        }
        return enumEntry
    }

    override fun transformField(field: FirField, data: ResolutionMode): FirField {
        dataFlowAnalyzer.enterField(field)

        context.withField(field) {
            field.transformAnnotations(transformer, data)
        }

        dataFlowAnalyzer.exitField(field)
        return field
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: ResolutionMode): FirTypeAlias {
        doTransformTypeParameters(typeAlias)
        typeAlias.transformAnnotations(transformer, data)
        return typeAlias
    }
}
