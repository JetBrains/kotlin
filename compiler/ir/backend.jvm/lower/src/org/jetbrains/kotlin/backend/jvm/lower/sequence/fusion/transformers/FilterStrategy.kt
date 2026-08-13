/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.transformers

import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.FilterVersion
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.IrBuilderWithParent
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceReplacement
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.callPredicate
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.evaluateFunctionArgumentOnce
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irEquals
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfThenElse
import org.jetbrains.kotlin.ir.builders.irImplicitCast
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irTrue
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.types.makeNotNull

internal class FilterStrategy(val filter: SequenceTransformer.Filter, builderWithParent: IrBuilderWithParent) :
    TransformerStrategy(builderWithParent) {
    override fun addTransformerToBodyBuilder(
        sequenceReplacement: SequenceReplacement,
    ): SequenceReplacement {
        val [builder, parent] = builderWithParent
        val [functionVariable, function] = filter.function?.let { builderWithParent.evaluateFunctionArgumentOnce(it) } ?: (null to null)
        val mainBodyBuilder = { sequenceVariable: IrValueDeclaration ->
            with(builder) {
                irBlock {
                    val condition = when (filter.version) {
                        FilterVersion.Filter -> callPredicate(function!!, parent, irGet(sequenceVariable))
                        FilterVersion.FilterNot -> irNot(callPredicate(function!!, parent, irGet(sequenceVariable)))
                        FilterVersion.FilterNotNull -> irNot(irEquals(irGet(sequenceVariable), irNull()))
                    }
                    val consumed = if (filter.version == FilterVersion.FilterNotNull) {
                        irBlock {
                            val notNullElement = irTemporary(
                                irImplicitCast(irGet(sequenceVariable), sequenceVariable.type.makeNotNull()),
                                nameHint = "notNullElement",
                            )
                            +sequenceReplacement.mainBodyBuilder(notNullElement)
                        }
                    } else {
                        sequenceReplacement.mainBodyBuilder(sequenceVariable)
                    }
                    +irIfThenElse(
                        context.irBuiltIns.booleanType,
                        condition,
                        consumed,
                        irTrue()
                    )
                }
            }
        }
        val initialDeclarations = listOfNotNull(functionVariable) + sequenceReplacement.initialDeclarations
        val finalExpression = sequenceReplacement.finalExpression
        return SequenceReplacement(initialDeclarations, mainBodyBuilder, finalExpression)
    }
}
