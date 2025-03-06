/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.extensions.FirStatusTransformerExtension
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.statusTransformerExtensions
import kotlin.reflect.KCallable

internal fun javaOrigin(isFromSource: Boolean): FirDeclarationOrigin.Java {
    return if (isFromSource) FirDeclarationOrigin.Java.Source else FirDeclarationOrigin.Java.Library
}

internal inline fun applyStatusTransformerExtensions(
    declaration: FirMemberDeclaration,
    originalStatus: FirResolvedDeclarationStatusImpl,
    operation: FirStatusTransformerExtension.(FirDeclarationStatus) -> FirDeclarationStatus,
): FirDeclarationStatus {
    val statusExtensions = declaration.moduleData.session.extensionService.statusTransformerExtensions
    if (statusExtensions.isEmpty()) return originalStatus

    val newStatus = statusExtensions.fold<FirStatusTransformerExtension, FirDeclarationStatus>(originalStatus) { acc, it ->
        if (it.needTransformStatus(declaration)) {
            it.operation(acc)
        } else {
            acc
        }
    } as FirDeclarationStatusImpl

    if (newStatus === originalStatus) return originalStatus
    return newStatus.resolved(
        newStatus.visibility,
        newStatus.modality ?: originalStatus.modality,
        originalStatus.effectiveVisibility
    )
}

internal fun FirDeclaration.shouldNotBeCalled(mutator: KCallable<*>, reader: KCallable<*>): Nothing {
    error("${mutator.name} should not be called for ${this::class.simpleName}, ${reader.name} is lazily calculated")
}