/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.builder.FirAnnotationContainerBuilder
import org.jetbrains.kotlin.fir.builder.FirBuilderDsl
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.builder.FirQualifiedAccessBuilder
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirVariableAssignmentImpl
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.visitors.*

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@FirBuilderDsl
class FirVariableAssignmentBuilder : FirQualifiedAccessBuilder, FirAnnotationContainerBuilder {
    override var source: FirSourceElement? = null
    lateinit var calleeReference: FirReference
    override val annotations: MutableList<FirAnnotationCall> = mutableListOf()
    override val typeArguments: MutableList<FirTypeProjection> = mutableListOf()
    override var explicitReceiver: FirExpression? = null
    override var dispatchReceiver: FirExpression = FirNoReceiverExpression
    override var extensionReceiver: FirExpression = FirNoReceiverExpression
    lateinit var rValue: FirExpression

    override fun build(): FirVariableAssignment {
        return FirVariableAssignmentImpl(
            source,
            calleeReference,
            annotations,
            typeArguments,
            explicitReceiver,
            dispatchReceiver,
            extensionReceiver,
            rValue,
        )
    }

}

@OptIn(ExperimentalContracts::class)
inline fun buildVariableAssignment(init: FirVariableAssignmentBuilder.() -> Unit): FirVariableAssignment {
    contract {
        callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
    }
    return FirVariableAssignmentBuilder().apply(init).build()
}
