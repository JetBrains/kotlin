/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.fir.expressions.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.builder.toMutableOrEmpty
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenSubjectExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirWhenSubjectExpressionImpl
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeProjection

@FirBuilderDsl
class FirWhenSubjectExpressionBuilder : FirQualifiedAccessExpressionBuilder, FirAnnotationContainerBuilder, FirExpressionBuilder {
    override var coneTypeOrNull: ConeKotlinType? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    override var source: KtSourceElement? = null
    override val nonFatalDiagnostics: MutableList<ConeDiagnostic> = mutableListOf()
    var contextSensitiveAlternative: FirPropertyAccessExpression? = null
    lateinit var calleeReference: FirNamedReference

    override fun build(): FirWhenSubjectExpression {
        return FirWhenSubjectExpressionImpl(
            coneTypeOrNull,
            annotations.toMutableOrEmpty(),
            source,
            nonFatalDiagnostics.toMutableOrEmpty(),
            contextSensitiveAlternative,
            calleeReference,
        )
    }


    @Deprecated("Modification of 'contextArguments' has no impact for FirWhenSubjectExpressionBuilder", level = DeprecationLevel.HIDDEN)
    override val contextArguments: MutableList<FirExpression> = mutableListOf()

    @Deprecated("Modification of 'typeArguments' has no impact for FirWhenSubjectExpressionBuilder", level = DeprecationLevel.HIDDEN)
    override val typeArguments: MutableList<FirTypeProjection> = mutableListOf()

    @Deprecated("Modification of 'explicitReceiver' has no impact for FirWhenSubjectExpressionBuilder", level = DeprecationLevel.HIDDEN)
    override var explicitReceiver: FirExpression?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'dispatchReceiver' has no impact for FirWhenSubjectExpressionBuilder", level = DeprecationLevel.HIDDEN)
    override var dispatchReceiver: FirExpression?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }

    @Deprecated("Modification of 'extensionReceiver' has no impact for FirWhenSubjectExpressionBuilder", level = DeprecationLevel.HIDDEN)
    override var extensionReceiver: FirExpression?
        get() = throw IllegalStateException()
        set(_) {
            throw IllegalStateException()
        }
}

@OptIn(ExperimentalContracts::class)
inline fun buildWhenSubjectExpression(init: FirWhenSubjectExpressionBuilder.() -> Unit): FirWhenSubjectExpression {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirWhenSubjectExpressionBuilder().apply(init).build()
}
