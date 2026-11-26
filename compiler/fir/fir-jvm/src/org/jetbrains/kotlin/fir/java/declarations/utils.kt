/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.extensions.FirStatusTransformerExtension
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.statusTransformerExtensions
import kotlin.reflect.KCallable

internal fun javaOrigin(isFromSource: Boolean, key: GeneratedDeclarationKey? = null): FirDeclarationOrigin.Java {
    return when {
        key != null -> FirDeclarationOrigin.Java.Plugin(key)
        isFromSource -> FirDeclarationOrigin.Java.Source
        else -> FirDeclarationOrigin.Java.Library
    }
}

internal inline fun <T : FirDeclarationStatus, R : FirResolvedDeclarationStatus> applyStatusTransformerExtensions(
    declaration: FirMemberDeclaration,
    originalStatus: R,
    operation: FirStatusTransformerExtension.(T) -> T,
): T {
    val statusExtensions = declaration.moduleData.session.extensionService.statusTransformerExtensions

    @Suppress("UNCHECKED_CAST")
    if (statusExtensions.isEmpty()) return originalStatus as T

    val newStatus = statusExtensions.fold<FirStatusTransformerExtension, FirDeclarationStatus>(originalStatus) { acc, it ->
        if (it.needTransformStatus(declaration)) {
            @Suppress("UNCHECKED_CAST")
            it.operation(acc as T)
        } else {
            acc
        }
    } as FirDeclarationStatusImpl

    @Suppress("UNCHECKED_CAST")
    if (newStatus === originalStatus) return originalStatus as T
    require(newStatus.visibility == originalStatus.visibility || declaration !is FirClassLikeDeclaration) {
        "Attempt to change visibility of a class-like: ${(declaration as? FirClassLikeDeclaration)?.symbol?.classId}, " +
                "original visibility: ${originalStatus.visibility}, new visibility: ${newStatus.visibility}"
    }

    @Suppress("UNCHECKED_CAST")
    return newStatus.resolved(
        newStatus.visibility,
        newStatus.modality ?: originalStatus.modality,
        originalStatus.effectiveVisibility
    ) as T
}

internal fun FirDeclaration.shouldNotBeCalled(mutator: KCallable<*>, reader: KCallable<*>): Nothing {
    error("${mutator.name} should not be called for ${this::class.simpleName}, ${reader.name} is lazily calculated")
}
