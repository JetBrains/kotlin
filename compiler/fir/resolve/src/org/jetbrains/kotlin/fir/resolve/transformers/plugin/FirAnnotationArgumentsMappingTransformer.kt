/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirErrorAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.*
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.util.PrivateForInline

open class FirAnnotationArgumentsMappingTransformer(
    session: FirSession,
    scopeSession: ScopeSession,
    resolvePhase: FirResolvePhase,
    outerBodyResolveContext: BodyResolveContext? = null,
    returnTypeCalculator: ReturnTypeCalculator = ReturnTypeCalculatorForFullBodyResolve.Default,
    firResolveContextCollector: FirResolveContextCollector? = null,
) : FirAbstractBodyResolveTransformerDispatcher(
    session,
    resolvePhase,
    implicitTypeOnly = false,
    scopeSession,
    outerBodyResolveContext = outerBodyResolveContext,
    returnTypeCalculator = returnTypeCalculator,
    firResolveContextCollector = firResolveContextCollector,
) {
    final override val expressionsTransformer: FirExpressionsResolveTransformer =
        FirExpressionTransformerForAnnotationArgumentsMapping(this)

    private val declarationsResolveTransformerForAnnotationArgumentsMapping =
        FirDeclarationsResolveTransformerForAnnotationArgumentsMapping(this)

    private val usualDeclarationTransformer = FirDeclarationsResolveTransformer(this)

    @PrivateForInline
    var isInsideAnnotationArgument = false

    @OptIn(PrivateForInline::class)
    inline fun <R> insideAnnotationArgument(action: () -> R): R {
        val oldValue = this.isInsideAnnotationArgument
        isInsideAnnotationArgument = true
        try {
            return action()
        } finally {
            isInsideAnnotationArgument = oldValue
        }
    }

    @OptIn(PrivateForInline::class)
    final override val declarationsTransformer: FirDeclarationsResolveTransformer
        get() {
            return if (isInsideAnnotationArgument) usualDeclarationTransformer
            else declarationsResolveTransformerForAnnotationArgumentsMapping
        }
}

private class FirExpressionTransformerForAnnotationArgumentsMapping(
    private val annotationArgumentsMappingTransformer: FirAnnotationArgumentsMappingTransformer,
) : FirExpressionsResolveTransformer(annotationArgumentsMappingTransformer) {

    override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: ResolutionMode): FirStatement {
        annotationArgumentsMappingTransformer.insideAnnotationArgument {
            return super.transformAnnotationCall(annotationCall, data)
        }
    }

    override fun transformErrorAnnotationCall(errorAnnotationCall: FirErrorAnnotationCall, data: ResolutionMode): FirStatement {
        annotationArgumentsMappingTransformer.insideAnnotationArgument {
            return super.transformErrorAnnotationCall(errorAnnotationCall, data)
        }
    }

}

private class FirDeclarationsResolveTransformerForAnnotationArgumentsMapping(
    transformer: FirAbstractBodyResolveTransformerDispatcher
) : FirDeclarationsResolveTransformer(transformer) {
    override fun withFile(file: FirFile, action: () -> FirFile): FirFile {
        return context.withFile(file, components) {
            action()
        }
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: ResolutionMode): FirRegularClass {
        regularClass.transformAnnotations(this, data)
        doTransformTypeParameters(regularClass)
        regularClass.transformSuperTypeRefs(this, data)

        doTransformRegularClass(regularClass, data)
        return regularClass
    }

    override fun withRegularClass(regularClass: FirRegularClass, action: () -> FirRegularClass): FirRegularClass {
        return context.withContainingClass(regularClass) {
            context.withRegularClass(regularClass, components) {
                action()
            }
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

    override fun transformErrorPrimaryConstructor(
        errorPrimaryConstructor: FirErrorPrimaryConstructor,
        data: ResolutionMode,
    ): FirErrorPrimaryConstructor = transformConstructor(errorPrimaryConstructor, data) as FirErrorPrimaryConstructor

    override fun transformValueParameter(valueParameter: FirValueParameter, data: ResolutionMode): FirValueParameter {
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
                .transformBackingField(transformer, data)
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

    override fun transformBackingField(backingField: FirBackingField, data: ResolutionMode): FirBackingField {
        backingField.transformAnnotations(transformer, data)
        return backingField
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: ResolutionMode): FirTypeAlias {
        doTransformTypeParameters(typeAlias)
        typeAlias.transformAnnotations(transformer, data)
        transformer.firResolveContextCollector?.addDeclarationContext(typeAlias, context)
        typeAlias.expandedTypeRef.transformSingle(transformer, data)
        return typeAlias
    }

    override fun transformScript(script: FirScript, data: ResolutionMode): FirScript {
        return script
    }
}
