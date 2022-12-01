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
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.*

open class FirAnnotationArgumentsMappingTransformer(
    session: FirSession,
    scopeSession: ScopeSession,
    resolvePhase: FirResolvePhase,
    outerBodyResolveContext: BodyResolveContext? = null
) : FirAbstractBodyResolveTransformerDispatcher(
    session,
    resolvePhase,
    implicitTypeOnly = false,
    scopeSession,
    outerBodyResolveContext = outerBodyResolveContext
) {
    final override val expressionsTransformer: FirExpressionsResolveTransformer =
        FirExpressionsResolveTransformer(this)

    final override val declarationsTransformer: FirDeclarationsResolveTransformer =
        FirDeclarationsResolveTransformerForAnnotationArgumentsMapping(this)
}

private class FirDeclarationsResolveTransformerForAnnotationArgumentsMapping(
    transformer: FirAbstractBodyResolveTransformerDispatcher
) : FirDeclarationsResolveTransformer(transformer) {
    override fun transformRegularClass(regularClass: FirRegularClass, data: ResolutionMode): FirStatement {
        regularClass.transformAnnotations(this, data)
        doTransformTypeParameters(regularClass)

        context.withContainingClass(regularClass) {
            context.withRegularClass(regularClass, components) {
                transformDeclarationContent(regularClass, data) as FirRegularClass
            }
        }

        return regularClass
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

        context.withSimpleFunction(simpleFunction, session) {
            simpleFunction
                .transformReturnTypeRef(transformer, data)
                .transformReceiverParameter(transformer, data)
                .transformValueParameters(transformer, data)
                .transformAnnotations(transformer, data)
        }

        return simpleFunction
    }

    override fun transformConstructor(constructor: FirConstructor, data: ResolutionMode): FirConstructor {
        val containingClass = context.containerIfAny as? FirRegularClass
        doTransformTypeParameters(constructor)

        context.withConstructor(constructor) {
            constructor
                .transformAnnotations(transformer, data)
                .transformReceiverParameter(transformer, data)
                .transformReturnTypeRef(transformer, data)

            context.forConstructorParameters(constructor, containingClass, components) {
                constructor.transformValueParameters(transformer, data)
            }
        }

        return constructor
    }

    override fun transformValueParameter(valueParameter: FirValueParameter, data: ResolutionMode): FirStatement {
        context.withValueParameter(valueParameter, session) {
            valueParameter
                .transformAnnotations(transformer, data)
                .transformReturnTypeRef(transformer, data)
        }

        return valueParameter
    }

    override fun transformProperty(property: FirProperty, data: ResolutionMode): FirProperty {
        property.transformReceiverParameter(transformer, ResolutionMode.ContextIndependent)
        doTransformTypeParameters(property)

        context.withProperty(property) {
            property
                .transformAnnotations(transformer, data)
                .transformReceiverParameter(transformer, data)
                .transformReturnTypeRef(transformer, data)
                .transformGetter(transformer, data)
                .transformSetter(transformer, data)
        }

        return property
    }

    override fun transformPropertyAccessor(
        propertyAccessor: FirPropertyAccessor,
        data: ResolutionMode
    ): FirPropertyAccessor {
        propertyAccessor
            .transformValueParameters(transformer, data)
            .transformReturnTypeRef(transformer, data)
            .transformReceiverParameter(transformer, data)
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
                .transformReceiverParameter(transformer, data)
                .transformReturnTypeRef(transformer, data)
                .transformTypeParameters(transformer, data)
        }
        return enumEntry
    }

    override fun transformField(field: FirField, data: ResolutionMode): FirField {
        context.withField(field) {
            field.transformAnnotations(transformer, data)
        }

        return field
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: ResolutionMode): FirTypeAlias {
        doTransformTypeParameters(typeAlias)
        typeAlias.transformAnnotations(transformer, data)
        return typeAlias
    }

    override fun transformScript(script: FirScript, data: ResolutionMode): FirScript {
        return script
    }
}
