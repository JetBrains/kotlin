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

class FirStatusResolveTransformer : FirAbstractTreeTransformer(phase = FirResolvePhase.STATUS) {
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
            is FirCallableMemberDeclaration<*> -> {
                val containingClass = classes.lastOrNull()
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

    override lateinit var session: FirSession

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        session = file.session
        return transformElement(file, data)
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

        return transformElement(declarationStatus, data)
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

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return transformMemberDeclaration(typeAlias, data)
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Nothing?): CompositeTransformResult<FirStatement> {
        return storeClass(regularClass) {
            transformMemberDeclaration(regularClass, data)
        } as CompositeTransformResult<FirStatement>
    }

    override fun transformMemberDeclaration(
        memberDeclaration: FirMemberDeclaration,
        data: Nothing?
    ): CompositeTransformResult<FirDeclaration> {
        return storeDeclaration(memberDeclaration) {
            transformDeclaration(memberDeclaration, data)
        }
    }

    override fun transformPropertyAccessor(
        propertyAccessor: FirPropertyAccessor,
        data: Nothing?
    ): CompositeTransformResult<FirStatement> {
        return storeDeclaration(propertyAccessor) {
            transformDeclaration(propertyAccessor, data)
        } as CompositeTransformResult<FirStatement>
    }

    override fun transformConstructor(
        constructor: FirConstructor,
        data: Nothing?
    ): CompositeTransformResult<FirDeclaration> {
        return storeDeclaration(constructor) {
            transformDeclaration(constructor, data)
        }
    }

    override fun transformSimpleFunction(simpleFunction: FirSimpleFunction, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return storeDeclaration(simpleFunction) {
            transformDeclaration(simpleFunction, data)
        }
    }

    override fun transformProperty(
        property: FirProperty,
        data: Nothing?
    ): CompositeTransformResult<FirDeclaration> {
        return storeDeclaration(property) {
            transformDeclaration(property, data)
        }
    }

    override fun transformValueParameter(valueParameter: FirValueParameter, data: Nothing?): CompositeTransformResult<FirStatement> {
        return (transformDeclaration(valueParameter, data).single as FirStatement).compose()
    }

    override fun transformBlock(block: FirBlock, data: Nothing?): CompositeTransformResult<FirStatement> {
        return block.compose()
    }
}