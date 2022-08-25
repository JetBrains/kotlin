/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirResolvedDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.builder.buildTypeParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef

inline fun FirFunctionCall.copyAsImplicitInvokeCall(
    setupCopy: FirImplicitInvokeCallBuilder.() -> Unit
): FirImplicitInvokeCall {
    val original = this

    return buildImplicitInvokeCall {
        source = original.source
        annotations.addAll(original.annotations)
        typeArguments.addAll(original.typeArguments)
        explicitReceiver = original.explicitReceiver
        dispatchReceiver = original.dispatchReceiver
        extensionReceiver = original.extensionReceiver
        argumentList = original.argumentList
        calleeReference = original.calleeReference

        setupCopy()
    }
}

fun FirTypeRef.resolvedTypeFromPrototype(
    type: ConeKotlinType
): FirResolvedTypeRef {
    return if (type is ConeErrorType) {
        buildErrorTypeRef {
            source = this@resolvedTypeFromPrototype.source
            this.type = type
            diagnostic = type.diagnostic
        }
    } else {
        buildResolvedTypeRef {
            source = this@resolvedTypeFromPrototype.source
            this.type = type
            annotations += this@resolvedTypeFromPrototype.annotations
        }
    }
}

fun FirTypeRef.errorTypeFromPrototype(
    diagnostic: ConeDiagnostic
): FirErrorTypeRef {
    return buildErrorTypeRef {
        source = this@errorTypeFromPrototype.source
        this.diagnostic = diagnostic
    }
}
