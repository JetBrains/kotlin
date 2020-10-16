/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.fir.visitors.transformSingle

@OptIn(AdapterForResolveProcessor::class)
class FirStatusResolveProcessor(session: FirSession, scopeSession: ScopeSession) :
    FirTransformerBasedResolveProcessor(session, scopeSession) {
    override val transformer = FirStatusResolveTransformer(session)
}

fun <F : FirClass<F>> F.runStatusResolveForLocalClass(session: FirSession): F {
    val transformer = FirStatusResolveTransformer(session)

    return this.transform<F, Nothing?>(transformer, null).single
}

class FirStatusResolveTransformer(
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
        return when (declaration) {
            is FirCallableDeclaration<*> -> {
                when (declaration) {
                    is FirProperty -> {
                        declaration.getter?.let { transformPropertyAccessor(it, data) }
                        declaration.setter?.let { transformPropertyAccessor(it, data) }
                    }
                    is FirFunction<*> -> {
                        for (valueParameter in declaration.valueParameters) {
                            transformValueParameter(valueParameter, data)
                        }
                    }
                }
                declaration.compose()
            }
            is FirPropertyAccessor -> {
                declaration.compose()
            }
            else -> {
                transformElement(declaration, data)
            }
        }
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: FirDeclarationStatus?): CompositeTransformResult<FirDeclaration> {
        typeAlias.typeParameters.forEach { transformDeclaration(it, data) }
        typeAlias.transformStatus(this, typeAlias.resolveStatus(typeAlias.status, containingClass, isLocal = false))
        return transformDeclaration(typeAlias, data)
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: FirDeclarationStatus?): CompositeTransformResult<FirStatement> {
        regularClass.transformStatus(this, regularClass.resolveStatus(regularClass.status, containingClass, isLocal = false))
        @Suppress("UNCHECKED_CAST")
        return storeClass(regularClass) {
            regularClass.typeParameters.forEach { it.transformSingle(this, data) }
            transformDeclaration(regularClass, data)
        } as CompositeTransformResult<FirStatement>
    }

    override fun transformAnonymousObject(
        anonymousObject: FirAnonymousObject,
        data: FirDeclarationStatus?
    ): CompositeTransformResult<FirStatement> {
        @Suppress("UNCHECKED_CAST")
        return storeClass(anonymousObject) {
            transformDeclaration(anonymousObject, data)
        } as CompositeTransformResult<FirStatement>
    }

    override fun transformPropertyAccessor(
        propertyAccessor: FirPropertyAccessor,
        data: FirDeclarationStatus?
    ): CompositeTransformResult<FirDeclaration> {
        propertyAccessor.transformStatus(this, propertyAccessor.resolveStatus(propertyAccessor.status, containingClass, isLocal = false))
        @Suppress("UNCHECKED_CAST")
        return transformDeclaration(propertyAccessor, data)
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

    override fun transformField(
        field: FirField,
        data: FirDeclarationStatus?
    ): CompositeTransformResult<FirDeclaration> {
        field.transformStatus(this, field.resolveStatus(field.status, containingClass, isLocal = false))
        return transformDeclaration(field, data)
    }

    override fun transformEnumEntry(enumEntry: FirEnumEntry, data: FirDeclarationStatus?): CompositeTransformResult<FirDeclaration> {
        enumEntry.transformStatus(this, enumEntry.resolveStatus(enumEntry.status, containingClass, isLocal = false))
        return transformDeclaration(enumEntry, data)
    }

    override fun transformValueParameter(
        valueParameter: FirValueParameter,
        data: FirDeclarationStatus?
    ): CompositeTransformResult<FirStatement> {
        @Suppress("UNCHECKED_CAST")
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
    if (status.visibility == Visibilities.Unknown || status.modality == null || status.modality == Modality.OPEN) {
        val visibility = when (status.visibility) {
            Visibilities.Unknown -> when {
                isLocal -> Visibilities.Local
                this is FirConstructor && containingClass is FirAnonymousObject -> Visibilities.Private
                else -> resolveVisibility(containingClass)
            }
            else -> status.visibility
        }
        val modality = status.modality?.let {
            if (it == Modality.OPEN && containingClass?.classKind == ClassKind.INTERFACE && !hasOwnBodyOrAccessorBody()) {
                Modality.ABSTRACT
            } else {
                it
            }
        } ?: resolveModality(containingClass)
        return (status as FirDeclarationStatusImpl).resolved(visibility, modality)
    }
    return status
}

private fun FirDeclaration.hasOwnBodyOrAccessorBody(): Boolean {
    return when (this) {
        is FirSimpleFunction -> this.body != null
        is FirProperty -> this.initializer != null || this.getter?.body != null || this.setter?.body != null
        else -> true
    }
}

private fun FirDeclaration.resolveVisibility(containingClass: FirClass<*>?): Visibility {
    // See DescriptorUtils#getDefaultConstructorVisibility in core.descriptors
    if (this is FirConstructor) {
        if (containingClass != null &&
            (containingClass.classKind == ClassKind.ENUM_CLASS || containingClass.classKind == ClassKind.ENUM_ENTRY ||
                    containingClass.modality == Modality.SEALED)
        ) {
            return Visibilities.Private
        }
    }
    return Visibilities.Public // TODO (overrides)
}

private fun FirDeclaration.resolveModality(containingClass: FirClass<*>?): Modality {
    return when (this) {
        is FirRegularClass -> if (classKind == ClassKind.INTERFACE) Modality.ABSTRACT else Modality.FINAL
        is FirCallableMemberDeclaration<*> -> {
            when {
                containingClass == null -> Modality.FINAL
                containingClass.classKind == ClassKind.INTERFACE -> {
                    when {
                        visibility == Visibilities.Private ->
                            Modality.FINAL
                        !this.hasOwnBodyOrAccessorBody() ->
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
