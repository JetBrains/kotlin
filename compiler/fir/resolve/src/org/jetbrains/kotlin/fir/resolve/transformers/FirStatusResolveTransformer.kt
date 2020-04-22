/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirEffectiveVisibility
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.firEffectiveVisibility
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.fir.visitors.transformSingle

@AdapterForResolvePhase
class FirStatusResolveTransformerAdapter : FirTransformer<Nothing?>() {
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        error("Should not be called for ${element::class}, only for files")
    }

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        val transformer = FirStatusResolveTransformer(file.session)
        return file.transform(transformer, null)
    }
}

fun <F : FirClass<F>> F.runStatusResolveForLocalClass(session: FirSession): F {
    val transformer = FirStatusResolveTransformer(session)

    return this.transform<F, Nothing?>(transformer, null).single
}

private class FirStatusResolveTransformer(
    override val session: FirSession
) : FirAbstractTreeTransformer<FirDeclarationStatus?>(phase = FirResolvePhase.STATUS) {
    private val classes = mutableListOf<FirClass<*>>()

    private val containingClass: FirClass<*>? get() = classes.lastOrNull()

    override fun transformDeclarationStatus(
        declarationStatus: FirDeclarationStatus,
        data: FirDeclarationStatus?
    ): CompositeTransformResult<FirDeclarationStatus> {
        return (data ?: declarationStatus).compose()
    }

    private inline fun storeClass(
        klass: FirClass<*>,
        computeResult: () -> CompositeTransformResult<FirDeclaration>
    ): CompositeTransformResult<FirDeclaration> {
        classes += klass
        val result = computeResult()
        classes.removeAt(classes.lastIndex)
        return result
    }

    override fun transformDeclaration(declaration: FirDeclaration, data: FirDeclarationStatus?): CompositeTransformResult<FirDeclaration> {
        declaration.replaceResolvePhase(transformerPhase)
        return if (declaration is FirCallableDeclaration<*>) {
            when (declaration) {
                is FirProperty -> {
                    declaration.getter?.let { transformPropertyAccessor(it, data) }
                    declaration.setter?.let { transformPropertyAccessor(it, data) }
                }
                is FirFunction<*> -> {
                    // Should we do it here?
                    for (valueParameter in declaration.valueParameters) {
                        transformValueParameter(valueParameter, data)
                    }
                }
            }
            declaration.compose()
        } else {
            transformElement(declaration, data)
        }
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: FirDeclarationStatus?): CompositeTransformResult<FirDeclaration> {
        typeAlias.typeParameters.forEach { transformDeclaration(it, data) }
        typeAlias.transformStatus(this, typeAlias.resolveStatus(typeAlias.status, containingClass, isLocal = false))
        return transformDeclaration(typeAlias, data)
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: FirDeclarationStatus?): CompositeTransformResult<FirStatement> {
        regularClass.transformStatus(this, regularClass.resolveStatus(regularClass.status, containingClass, isLocal = false))
        return storeClass(regularClass) {
            regularClass.typeParameters.forEach { it.transformSingle(this, data) }
            transformDeclaration(regularClass, data)
        } as CompositeTransformResult<FirStatement>
    }

    override fun transformAnonymousObject(
        anonymousObject: FirAnonymousObject,
        data: FirDeclarationStatus?
    ): CompositeTransformResult<FirStatement> {
        return storeClass(anonymousObject) {
            transformDeclaration(anonymousObject, data)
        } as CompositeTransformResult<FirStatement>
    }

    override fun transformPropertyAccessor(
        propertyAccessor: FirPropertyAccessor,
        data: FirDeclarationStatus?
    ): CompositeTransformResult<FirStatement> {
        propertyAccessor.transformStatus(this, propertyAccessor.resolveStatus(propertyAccessor.status, containingClass, isLocal = false))
        return transformDeclaration(propertyAccessor, data) as CompositeTransformResult<FirStatement>
    }

    override fun transformConstructor(
        constructor: FirConstructor,
        data: FirDeclarationStatus?
    ): CompositeTransformResult<FirDeclaration> {
        constructor.transformStatus(this, constructor.resolveStatus(constructor.status, containingClass, isLocal = false))
        return transformDeclaration(constructor, data)
    }

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: FirDeclarationStatus?
    ): CompositeTransformResult<FirDeclaration> {
        simpleFunction.transformStatus(this, simpleFunction.resolveStatus(simpleFunction.status, containingClass, isLocal = false))
        return transformDeclaration(simpleFunction, data)
    }

    override fun transformProperty(
        property: FirProperty,
        data: FirDeclarationStatus?
    ): CompositeTransformResult<FirDeclaration> {
        property.transformStatus(this, property.resolveStatus(property.status, containingClass, isLocal = false))
        return transformDeclaration(property, data)
    }

    override fun transformEnumEntry(enumEntry: FirEnumEntry, data: FirDeclarationStatus?): CompositeTransformResult<FirDeclaration> {
        return transformDeclaration(enumEntry, data)
    }

    override fun transformValueParameter(
        valueParameter: FirValueParameter,
        data: FirDeclarationStatus?
    ): CompositeTransformResult<FirStatement> {
        return transformDeclaration(valueParameter, data) as CompositeTransformResult<FirStatement>
    }

    override fun transformTypeParameter(
        typeParameter: FirTypeParameter,
        data: FirDeclarationStatus?
    ): CompositeTransformResult<FirDeclaration> {
        return transformDeclaration(typeParameter, data)
    }

    override fun transformBlock(block: FirBlock, data: FirDeclarationStatus?): CompositeTransformResult<FirStatement> {
        return block.compose()
    }
}

private val <F : FirClass<F>> FirClass<F>.effectiveVisibility: FirEffectiveVisibility
    get() = when (this) {
        is FirRegularClass -> status.effectiveVisibility
        is FirAnonymousObject -> FirEffectiveVisibility.Local
        else -> error("Unknown kind of class: ${this::class}")
    }

private val <F : FirClass<F>> FirClass<F>.modality: Modality?
    get() = when (this) {
        is FirRegularClass -> status.modality
        is FirAnonymousObject -> Modality.FINAL
        else -> error("Unknown kind of class: ${this::class}")
    }

fun FirDeclaration.resolveStatus(
    status: FirDeclarationStatus,
    containingClass: FirClass<*>?,
    isLocal: Boolean
): FirDeclarationStatus {
    if (status.visibility == Visibilities.UNKNOWN || status.modality == null) {
        val visibility = when (status.visibility) {
            Visibilities.UNKNOWN -> when {
                isLocal -> Visibilities.LOCAL
                this is FirConstructor && containingClass is FirAnonymousObject -> Visibilities.PRIVATE
                else -> resolveVisibility(containingClass)
            }
            else -> status.visibility
        }
        val modality = status.modality ?: resolveModality(containingClass)
        val containerEffectiveVisibility = containingClass?.effectiveVisibility ?: FirEffectiveVisibility.Public
        val effectiveVisibility =
            visibility.firEffectiveVisibility(session, this as? FirMemberDeclaration).lowerBound(containerEffectiveVisibility)
        return (status as FirDeclarationStatusImpl).resolved(visibility, effectiveVisibility, modality)
    }
    return status
}

private fun FirDeclaration.resolveVisibility(containingClass: FirClass<*>?): Visibility {
    if (this is FirConstructor) {
        if (containingClass != null &&
            (containingClass.classKind == ClassKind.ENUM_CLASS || containingClass.modality == Modality.SEALED)
        ) {
            return Visibilities.PRIVATE
        }
    }
    return Visibilities.PUBLIC // TODO (overrides)
}

private fun FirDeclaration.resolveModality(containingClass: FirClass<*>?): Modality {
    return when (this) {
        is FirRegularClass -> if (classKind == ClassKind.INTERFACE) Modality.ABSTRACT else Modality.FINAL
        is FirCallableMemberDeclaration<*> -> {
            when {
                containingClass == null -> Modality.FINAL
                containingClass.classKind == ClassKind.INTERFACE -> {
                    when {
                        visibility == Visibilities.PRIVATE ->
                            Modality.FINAL
                        this is FirSimpleFunction && body == null ->
                            Modality.ABSTRACT
                        this is FirProperty && initializer == null && getter?.body == null && setter?.body == null ->
                            Modality.ABSTRACT
                        else ->
                            Modality.OPEN
                    }
                }
                else -> {
                    if (isOverride && containingClass.modality != Modality.FINAL) Modality.OPEN else Modality.FINAL
                }
            }
        }
        else -> Modality.FINAL
    }
}
