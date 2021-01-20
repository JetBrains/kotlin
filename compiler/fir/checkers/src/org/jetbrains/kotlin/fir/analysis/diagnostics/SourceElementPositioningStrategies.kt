/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.diagnostics.PositioningStrategies

object SourceElementPositioningStrategies {
    val DEFAULT = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DEFAULT,
        PositioningStrategies.DEFAULT
    )

    val VAL_OR_VAR_NODE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.VAL_OR_VAR_NODE,
        PositioningStrategies.VAL_OR_VAR_NODE
    )

    val SECONDARY_CONSTRUCTOR_DELEGATION_CALL = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.SECONDARY_CONSTRUCTOR_DELEGATION_CALL,
        PositioningStrategies.SECONDARY_CONSTRUCTOR_DELEGATION_CALL
    )

    val DECLARATION_NAME = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DECLARATION_NAME,
        PositioningStrategies.DECLARATION_NAME
    )

    val DECLARATION_SIGNATURE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DECLARATION_SIGNATURE,
        PositioningStrategies.DECLARATION_SIGNATURE
    )

    val DECLARATION_SIGNATURE_OR_DEFAULT = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT,
        PositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT
    )

    val VISIBILITY_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.VISIBILITY_MODIFIER,
        PositioningStrategies.VISIBILITY_MODIFIER
    )

    val MODALITY_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.MODALITY_MODIFIER,
        PositioningStrategies.MODALITY_MODIFIER
    )

    val OPERATOR = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.OPERATOR,
        PositioningStrategies.OPERATOR
    )
}