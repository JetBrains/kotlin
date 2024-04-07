/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.plugin

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirErrorAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildErrorNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedBaseSymbol
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeAmbiguouslyResolvedAnnotationArgument
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.*
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds
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
) {
    final override val expressionsTransformer: FirExpressionsResolveTransformer = FirExpressionTransformerForAnnotationArguments(this)

    private val declarationsResolveTransformerForAnnotationArguments = FirDeclarationsResolveTransformerForAnnotationArguments(this)

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
            else declarationsResolveTransformerForAnnotationArguments
        }
}

/**
 *  Set of enum class IDs that are resolved in COMPILER_REQUIRED_ANNOTATIONS phase that need to be rechecked here.
 */
private val classIdsToCheck: Set<ClassId> = setOf(StandardClassIds.DeprecationLevel, StandardClassIds.AnnotationTarget)

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

    override fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: ResolutionMode
    ): FirStatement {
        if (qualifiedAccessExpression is FirPropertyAccessExpression) {
            val calleeReference = qualifiedAccessExpression.calleeReference
            if (calleeReference is FirResolvedNamedReference) {
                val resolvedSymbol = calleeReference.resolvedSymbol
                if (resolvedSymbol is FirEnumEntrySymbol && resolvedSymbol.containingClassLookupTag()?.classId in classIdsToCheck) {
                    return resolveSpecialPropertyAccess(qualifiedAccessExpression, calleeReference, resolvedSymbol, data)
                }
            }
        }

        return super.transformQualifiedAccessExpression(qualifiedAccessExpression, data)
    }

    private fun resolveSpecialPropertyAccess(
        originalAccess: FirPropertyAccessExpression,
        originalCalleeReference: FirResolvedNamedReference,
        originalResolvedSymbol: FirEnumEntrySymbol,
        data: ResolutionMode,
    ): FirStatement {
        val accessCopyForResolution = buildPropertyAccessExpression {
            source = originalAccess.source
            typeArguments.addAll(originalAccess.typeArguments)

            val originalResolvedQualifier = originalAccess.explicitReceiver
            if (originalResolvedQualifier is FirResolvedQualifier) {
                val fqName = originalResolvedQualifier.classId
                    ?.let { if (originalResolvedQualifier.isFullyQualified) it.asSingleFqName() else it.relativeClassName }
                    ?: originalResolvedQualifier.packageFqName
                explicitReceiver = generatePropertyAccessExpression(fqName, originalResolvedQualifier.source)
            }

            calleeReference = buildSimpleNamedReference {
                source = originalCalleeReference.source
                name = originalCalleeReference.name
            }
        }

        val resolved = super.transformQualifiedAccessExpression(accessCopyForResolution, data)

        if (resolved is FirQualifiedAccessExpression) {
            // The initial resolution must have been to an enum entry. Report ambiguity if symbolFromArgumentsPhase is different to
            // original symbol including null (meaning we would resolve to something other than an enum entry).
            val symbolFromArgumentsPhase = resolved.calleeReference.toResolvedBaseSymbol()
            if (originalResolvedSymbol != symbolFromArgumentsPhase) {
                resolved.replaceCalleeReference(buildErrorNamedReference {
                    source = resolved.calleeReference.source
                    diagnostic = ConeAmbiguouslyResolvedAnnotationArgument(originalResolvedSymbol, symbolFromArgumentsPhase)
                })
            }
        }

        return resolved
    }

    private fun generatePropertyAccessExpression(fqName: FqName, accessSource: KtSourceElement?): FirPropertyAccessExpression {
        var result: FirPropertyAccessExpression? = null

        val pathSegments = fqName.pathSegments()
        for ((index, pathSegment) in pathSegments.withIndex()) {
            result = buildPropertyAccessExpression {
                calleeReference = buildSimpleNamedReference { name = pathSegment }
                explicitReceiver = result

                if (index == pathSegments.lastIndex) {
                    source = accessSource
                }
            }
        }

        return result ?: error("Got an empty ClassId")
    }
}

private class FirDeclarationsResolveTransformerForAnnotationArguments(
    transformer: FirAbstractBodyResolveTransformerDispatcher
) : FirDeclarationsResolveTransformer(transformer) {
    override fun withFile(file: FirFile, action: () -> FirFile): FirFile {
        return context.withFile(file, components) {
            action()
        }
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: ResolutionMode): FirRegularClass {
        context.withClassHeader(regularClass) {
            regularClass.transformAnnotations(this, ResolutionMode.ContextIndependent)
            regularClass.transformTypeParameters(this, ResolutionMode.ContextIndependent)
            regularClass.transformSuperTypeRefs(this, ResolutionMode.ContextIndependent)
        }

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

    override fun withScript(script: FirScript, action: () -> FirScript): FirScript {
        return context.withScript(script, components) {
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

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: ResolutionMode
    ): FirSimpleFunction {
        context.withSimpleFunction(simpleFunction, session) {
            simpleFunction
                .transformTypeParameters(transformer, data)
                .transformReturnTypeRef(transformer, data)
                .transformReceiverParameter(transformer, data)
                .transformValueParameters(transformer, data)
                .transformAnnotations(transformer, data)
        }

        return simpleFunction
    }

    override fun transformConstructor(constructor: FirConstructor, data: ResolutionMode): FirConstructor {
        val containingClass = context.containerIfAny as? FirRegularClass
        context.withConstructor(constructor) {
            constructor
                .transformTypeParameters(transformer, data)
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
        context.withProperty(property) {
            property
                .transformTypeParameters(transformer, data)
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
        backingField.transformAnnotations(transformer, data)
        return backingField
    }

    override fun transformScript(script: FirScript, data: ResolutionMode): FirScript {
        return script
    }
}
