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
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.impl.FirVariableAssignmentImpl

@FirBuilderDsl
class FirVariableAssignmentBuilder : FirAnnotationContainerBuilder {
    override var source: KtSourceElement? = null
    override val annotations: MutableList<FirAnnotation> = mutableListOf()
    lateinit var lValue: FirExpression
    lateinit var rValue: FirExpression

    override fun build(): FirVariableAssignment {
        return FirVariableAssignmentImpl(
            source,
            annotations.toMutableOrEmpty(),
            lValue,
            rValue,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildVariableAssignment(init: FirVariableAssignmentBuilder.() -> Unit): FirVariableAssignment {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return FirVariableAssignmentBuilder().apply(init).build()
}
