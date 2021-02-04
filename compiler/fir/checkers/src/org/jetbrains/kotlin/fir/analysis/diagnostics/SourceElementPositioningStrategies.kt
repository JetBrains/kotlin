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

    val DECLARATION_RETURN_TYPE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DECLARATION_RETURN_TYPE,
        PositioningStrategies.DECLARATION_RETURN_TYPE
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

    val ABSTRACT_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.ABSTRACT_MODIFIER,
        PositioningStrategies.ABSTRACT_MODIFIER
    )

    val OPEN_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.OPEN_MODIFIER,
        PositioningStrategies.OPEN_MODIFIER
    )

    val OVERRIDE_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.OVERRIDE_MODIFIER,
        PositioningStrategies.OVERRIDE_MODIFIER
    )

    val PRIVATE_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.PRIVATE_MODIFIER,
        PositioningStrategies.PRIVATE_MODIFIER
    )

    val LATEINIT_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.LATEINIT_MODIFIER,
        PositioningStrategies.LATEINIT_MODIFIER
    )

    val VARIANCE_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.VARIANCE_MODIFIER,
        PositioningStrategies.VARIANCE_MODIFIER
    )

    val OPERATOR = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.OPERATOR,
        PositioningStrategies.OPERATOR
    )
    
    val PARAMETER_DEFAULT_VALUE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.PARAMETER_DEFAULT_VALUE,
        PositioningStrategies.PARAMETER_DEFAULT_VALUE
    )

    val PARAMETER_VARARG_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.PARAMETER_VARARG_MODIFIER,
        PositioningStrategies.PARAMETER_VARARG_MODIFIER
    )

    val DOT_BY_SELECTOR = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DOT_BY_SELECTOR,
        PositioningStrategies.DOT_BY_SELECTOR
    )

    val WHEN_EXPRESSION = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.WHEN_EXPRESSION,
        PositioningStrategies.WHEN_EXPRESSION
    )

    val IF_EXPRESSION = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.IF_EXPRESSION,
        PositioningStrategies.IF_EXPRESSION
    )
}
