/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose

class FirStatusResolveTransformer : FirTransformer<Nothing?>() {
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        @Suppress("UNCHECKED_CAST")
        return (element.transformChildren(this, data) as E).compose()
    }

    private val declarationsWithStatuses = mutableListOf<FirDeclaration>()

    private val classes = mutableListOf<FirRegularClass>()

    private fun FirDeclaration.resolveVisibility(): Visibility {
        if (this is FirConstructor) {
            val klass = classes.lastOrNull()
            if (klass != null && (klass.classKind == ClassKind.ENUM_CLASS || klass.modality == Modality.SEALED)) {
                return Visibilities.PRIVATE
            }
        }
        return Visibilities.PUBLIC // TODO (overrides)
    }

    private fun FirDeclaration.resolveModality(): Modality {
        return when (this) {
            is FirEnumEntry -> Modality.FINAL
            is FirRegularClass -> if (classKind == ClassKind.INTERFACE) Modality.ABSTRACT else Modality.FINAL
            is FirCallableMember -> {
                val containingClass = classes.lastOrNull()
                when {
                    containingClass == null -> Modality.FINAL
                    containingClass.classKind == ClassKind.INTERFACE -> {
                        when {
                            visibility == Visibilities.PRIVATE ->
                                Modality.FINAL
                            this is FirNamedFunction && body == null ->
                                Modality.ABSTRACT
                            this is FirProperty && initializer == null && getter.body == null && setter.body == null ->
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

    override fun transformDeclarationStatus(
        declarationStatus: FirDeclarationStatus,
        data: Nothing?
    ): CompositeTransformResult<FirDeclarationStatus> {
        if (declarationStatus.visibility == Visibilities.UNKNOWN || declarationStatus.modality == null) {
            val declaration = declarationsWithStatuses.last()
            val visibility = when (declarationStatus.visibility) {
                Visibilities.UNKNOWN -> declaration.resolveVisibility()
                else -> declarationStatus.visibility
            }
            val modality = declarationStatus.modality ?: declaration.resolveModality()
            val resolvedStatus = (declarationStatus as FirDeclarationStatusImpl).resolved(visibility, modality)
            return resolvedStatus.compose()
        }

        return super.transformDeclarationStatus(declarationStatus, data)
    }

    private inline fun storeDeclaration(
        declaration: FirDeclaration,
        computeResult: () -> CompositeTransformResult<FirDeclaration>
    ): CompositeTransformResult<FirDeclaration> {
        declarationsWithStatuses += declaration
        val result = computeResult()
        declarationsWithStatuses.removeAt(declarationsWithStatuses.lastIndex)
        return result
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

    override fun transformRegularClass(regularClass: FirRegularClass, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return storeClass(regularClass) {
            super.transformRegularClass(regularClass, data)
        }
    }

    override fun transformMemberDeclaration(
        memberDeclaration: FirMemberDeclaration,
        data: Nothing?
    ): CompositeTransformResult<FirDeclaration> {
        return storeDeclaration(memberDeclaration) {
            super.transformMemberDeclaration(memberDeclaration, data)
        }
    }

    override fun transformPropertyAccessor(
        propertyAccessor: FirPropertyAccessor,
        data: Nothing?
    ): CompositeTransformResult<FirDeclaration> {
        return storeDeclaration(propertyAccessor) {
            super.transformPropertyAccessor(propertyAccessor, data)
        }
    }

    override fun transformConstructor(
        constructor: FirConstructor,
        data: Nothing?
    ): CompositeTransformResult<FirDeclaration> {
        return storeDeclaration(constructor) {
            super.transformConstructor(constructor, data)
        }
    }

    override fun transformNamedFunction(
        namedFunction: FirNamedFunction,
        data: Nothing?
    ): CompositeTransformResult<FirDeclaration> {
        return storeDeclaration(namedFunction) {
            super.transformNamedFunction(namedFunction, data)
        }
    }

    override fun transformProperty(
        property: FirProperty,
        data: Nothing?
    ): CompositeTransformResult<FirDeclaration> {
        return storeDeclaration(property) {
            super.transformProperty(property, data)
        }
    }
}