/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose

class FirStatusResolveTransformer : FirAbstractTreeTransformer<FirDeclarationStatus?>(phase = FirResolvePhase.STATUS) {
    private val classes = mutableListOf<FirRegularClass>()

    private val containingClass: FirRegularClass? get() = classes.lastOrNull()

    override lateinit var session: FirSession

    override fun transformFile(file: FirFile, data: FirDeclarationStatus?): CompositeTransformResult<FirFile> {
        session = file.session
        return super.transformFile(file, data)
    }

    override fun transformDeclarationStatus(
        declarationStatus: FirDeclarationStatus,
        data: FirDeclarationStatus?
    ): CompositeTransformResult<FirDeclarationStatus> {
        return (data ?: declarationStatus).compose()
    }

    private inline fun storeClass(
        klass: FirRegularClass,
        computeResult: () -> CompositeTransformResult<FirDeclaration>
    ): CompositeTransformResult<FirDeclaration> {
        classes += klass
        val result = computeResult()
        classes.removeAt(classes.lastIndex)
        return result
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: FirDeclarationStatus?): CompositeTransformResult<FirDeclaration> {
        typeAlias.typeParameters.forEach { transformDeclaration(it, data) }
        typeAlias.transformStatus(this, typeAlias.resolveStatus(typeAlias.status, containingClass, isLocal = false))
        return transformDeclaration(typeAlias, data)
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: FirDeclarationStatus?): CompositeTransformResult<FirStatement> {
        regularClass.transformStatus(this, regularClass.resolveStatus(regularClass.status, containingClass, isLocal = false))
        return storeClass(regularClass) {
            regularClass.typeParameters.forEach { transformDeclaration(it, data) }
            transformDeclaration(regularClass, data)
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

    override fun transformValueParameter(
        valueParameter: FirValueParameter,
        data: FirDeclarationStatus?
    ): CompositeTransformResult<FirStatement> {
        return transformDeclaration(valueParameter, data) as CompositeTransformResult<FirStatement>
    }

    override fun transformBlock(block: FirBlock, data: FirDeclarationStatus?): CompositeTransformResult<FirStatement> {
        return block.compose()
    }

    companion object {
        fun FirDeclaration.resolveStatus(
            status: FirDeclarationStatus,
            containingClass: FirRegularClass?,
            isLocal: Boolean
        ): FirDeclarationStatus {
            if (status.visibility == Visibilities.UNKNOWN || status.modality == null) {
                val visibility = when (status.visibility) {
                    Visibilities.UNKNOWN -> if (isLocal) Visibilities.LOCAL else resolveVisibility(containingClass)
                    else -> status.visibility
                }
                val modality = status.modality ?: resolveModality(containingClass)
                return (status as FirDeclarationStatusImpl).resolved(visibility, modality)
            }
            return status
        }

        private fun FirDeclaration.resolveVisibility(containingClass: FirRegularClass?): Visibility {
            if (this is FirConstructor) {
                if (containingClass != null &&
                    (containingClass.classKind == ClassKind.ENUM_CLASS || containingClass.modality == Modality.SEALED)
                ) {
                    return Visibilities.PRIVATE
                }
            }
            return Visibilities.PUBLIC // TODO (overrides)
        }

        private fun FirDeclaration.resolveModality(containingClass: FirRegularClass?): Modality {
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
    }
}