/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirAnnotatedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import kotlin.reflect.KClass

abstract class FirStatusTransformerExtension(session: FirSession) : FirPredicateBasedExtension(session) {
    companion object {
        val NAME = FirExtensionPointName("StatusTransformer")
    }

    final override val name: FirExtensionPointName
        get() = NAME

    final override val extensionType: KClass<out FirExtension> = FirStatusTransformerExtension::class

    abstract fun transformStatus(
        declaration: FirDeclaration<*>,
        owners: List<FirAnnotatedDeclaration<*>>,
        status: FirDeclarationStatus
    ): FirDeclarationStatus

    fun interface Factory : FirExtension.Factory<FirStatusTransformerExtension>
}

val FirExtensionService.statusTransformerExtensions: List<FirStatusTransformerExtension> by FirExtensionService.registeredExtensions()

inline fun FirDeclarationStatus.transform(
    visibility: Visibility = this.visibility,
    modality: Modality? = this.modality,
    init: FirDeclarationStatusImpl.() -> Unit = {}
): FirDeclarationStatus {
    @Suppress("DuplicatedCode")
    return FirDeclarationStatusImpl(visibility, modality).apply {
        isExpect = this@transform.isExpect
        isActual = this@transform.isActual
        isOverride = this@transform.isOverride
        isOperator = this@transform.isOperator
        isInfix = this@transform.isInfix
        isInline = this@transform.isInline
        isTailRec = this@transform.isTailRec
        isExternal = this@transform.isExternal
        isConst = this@transform.isConst
        isLateInit = this@transform.isLateInit
        isInner = this@transform.isInner
        isCompanion = this@transform.isCompanion
        isData = this@transform.isData
        isSuspend = this@transform.isSuspend
        isStatic = this@transform.isStatic
        isFromSealedClass = this@transform.isFromSealedClass
        isFromEnumClass = this@transform.isFromEnumClass
    }.apply(init)
}
