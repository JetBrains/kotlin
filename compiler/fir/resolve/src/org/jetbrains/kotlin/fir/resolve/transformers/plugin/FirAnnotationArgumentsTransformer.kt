/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.BodyResolveContext
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformerDispatcher
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirExpressionsResolveTransformer
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.util.PrivateForInline

open class FirAnnotationArgumentsTransformer(
    session: FirSession,
    scopeSession: ScopeSession,
    resolvePhase: FirResolvePhase,
    outerBodyResolveContext: BodyResolveContext? = null,
    returnTypeCalculator: ReturnTypeCalculator = ReturnTypeCalculatorForFullBodyResolve.Default,
) : FirAbstractBodyResolveTransformerDispatcher(
    session,
    resolvePhase,
    implicitTypeOnly = false,
    scopeSession,
    outerBodyResolveContext = outerBodyResolveContext,
    returnTypeCalculator = returnTypeCalculator,
    expandTypeAliases = true,
) {
    final override val expressionsTransformer: FirExpressionsResolveTransformer = FirExpressionTransformerForAnnotationArguments(this)

    private val declarationsResolveTransformerForAnnotationArguments = FirDeclarationsResolveTransformerForAnnotationArguments(this)

    private val usualDeclarationTransformer = FirDeclarationsResolveTransformer(this)

    @PrivateForInline
    var isInsideAnnotationArgument: Boolean = false

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
            else declarationsResolveTransformerForAnnotationArguments
        }
}

private class FirExpressionTransformerForAnnotationArguments(
    private val annotationArgumentsTransformer: FirAnnotationArgumentsTransformer,
) : FirExpressionsResolveTransformer(annotationArgumentsTransformer) {

    override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: ResolutionMode): FirStatement {
        annotationArgumentsTransformer.insideAnnotationArgument {
            return super.transformAnnotationCall(annotationCall, data)
        }
    }

    override fun transformErrorAnnotationCall(errorAnnotationCall: FirErrorAnnotationCall, data: ResolutionMode): FirStatement {
        annotationArgumentsTransformer.insideAnnotationArgument {
            return super.transformErrorAnnotationCall(errorAnnotationCall, data)
        }
    }
}

private class FirDeclarationsResolveTransformerForAnnotationArguments(
    transformer: FirAbstractBodyResolveTransformerDispatcher
) : FirDeclarationsResolveTransformer(transformer) {
    override fun withFile(file: FirFile, action: () -> FirFile): FirFile {
        return context.withFile(file) {
            action()
        }
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: ResolutionMode): FirRegularClass {
        context.withClassHeader(regularClass) {
            regularClass.transformAnnotations(this, ResolutionMode.ContextIndependent)
            regularClass.transformTypeParameters(this, ResolutionMode.ContextIndependent)
            regularClass.transformSuperTypeRefs(this, ResolutionMode.ContextIndependent)
            regularClass.contextParameters.forEach {
                it.transformSingle(this, ResolutionMode.ContextIndependent)
            }
        }

        doTransformRegularClassContent(regularClass, data)
        return regularClass
    }

    override fun forRegularClassBody(regularClass: FirRegularClass, action: () -> FirRegularClass): FirRegularClass {
        return context.withContainingClass(regularClass) {
            context.forRegularClassBody(regularClass) {
                action()
            }
        }
    }

    override fun withScript(script: FirScript, action: () -> FirScript): FirScript {
        return context.withScript(script) {
            action()
        }
    }

    override fun transformAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer,
        data: ResolutionMode
    ): FirAnonymousInitializer {
        @OptIn(PrivateForInline::class)
        context.withContainer(anonymousInitializer) {
            anonymousInitializer.transformAnnotations(this, ResolutionMode.ContextIndependent)
        }

        return anonymousInitializer
    }

    override fun transformNamedFunction(
        namedFunction: FirNamedFunction,
        data: ResolutionMode
    ): FirNamedFunction {
        context.withNamedFunction(namedFunction, session) {
            namedFunction
                .transformTypeParameters(transformer, data)
                .transformReturnTypeRef(transformer, data)
                .transformReceiverParameter(transformer, data)
                .transformValueParameters(transformer, data)
                .transformAnnotations(transformer, data)
                .contextParameters.forEach {
                    it.transformSingle(transformer, data)
                }
        }

        return namedFunction
    }

    override fun transformConstructor(constructor: FirConstructor, data: ResolutionMode): FirConstructor {
        val containingClass = context.containerIfAny as? FirRegularClass
        context.forConstructor(constructor) {
            constructor
                .transformTypeParameters(transformer, data)
                .transformAnnotations(transformer, data)
                .transformReceiverParameter(transformer, data)
                .transformReturnTypeRef(transformer, data)
                .contextParameters.forEach {
                    it.transformSingle(transformer, data)
                }

            context.forConstructorParameters(constructor, containingClass) {
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
        context.withProperty(property) {
            property
                .transformTypeParameters(transformer, data)
                .transformAnnotations(transformer, data)
                .transformReceiverParameter(transformer, data)
                .transformReturnTypeRef(transformer, data)
                .transformGetter(transformer, data)
                .transformSetter(transformer, data)
                .transformBackingField(transformer, data)
                .transformContextParameters(transformer, data)
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
        context.withEnumEntry(enumEntry) {
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
                .transformReturnTypeRef(transformer, data)
                .transformBackingField(transformer, data)
        }

        return field
    }

    override fun transformBackingField(backingField: FirBackingField, data: ResolutionMode): FirBackingField {
        backingField
            .transformAnnotations(transformer, data)
            .transformReturnTypeRef(transformer, data)

        return backingField
    }

    override fun transformScript(script: FirScript, data: ResolutionMode): FirScript {
        return script
    }

    override fun transformReplSnippet(replSnippet: FirReplSnippet, data: ResolutionMode): FirReplSnippet {
        return replSnippet
    }
}
