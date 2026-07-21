/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.producers

import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.IrBuilderWithParent
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceData
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceReplacement
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression

internal class EmptySequenceStrategy() : ProducerStrategy() {
    override fun fuseConsumer(
        builderWithParent: IrBuilderWithParent,
        sequenceData: SequenceData,
        sequenceReplacement: SequenceReplacement,
    ): IrContainerExpression = builderWithParent.first.irBlock {
        +sequenceReplacement.initialDeclarations
        +sequenceReplacement.finalExpression
    }
}
