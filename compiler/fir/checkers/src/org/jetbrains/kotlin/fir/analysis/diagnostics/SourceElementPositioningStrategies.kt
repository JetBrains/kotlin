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

    val FUN_INTERFACE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.FUN_INTERFACE,
        PositioningStrategies.FUN_INTERFACE
    )

    val COMPANION_OBJECT = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.COMPANION_OBJECT,
        PositioningStrategies.COMPANION_OBJECT
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

    val CONST_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.CONST_MODIFIER,
        PositioningStrategies.CONST_MODIFIER
    )

    val INLINE_OR_VALUE_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.INLINE_OR_VALUE_MODIFIER,
        PositioningStrategies.INLINE_OR_VALUE_MODIFIER
    )

    val INNER_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.INNER_MODIFIER,
        PositioningStrategies.INNER_MODIFIER
    )

    val FUN_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.FUN_MODIFIER,
        PositioningStrategies.FUN_MODIFIER
    )

    val SUSPEND_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.SUSPEND_MODIFIER,
        PositioningStrategies.SUSPEND_MODIFIER
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

    val NAME_OF_NAMED_ARGUMENT = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.NAME_OF_NAMED_ARGUMENT,
        PositioningStrategies.NAME_OF_NAMED_ARGUMENT
    )

    val VALUE_ARGUMENTS = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.VALUE_ARGUMENTS,
        PositioningStrategies.VALUE_ARGUMENTS
    )

    val SUPERTYPES_LIST = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.SUPERTYPES_LIST,
        PositioningStrategies.SUPERTYPES_LIST
    )

    val DOT_BY_QUALIFIED = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DOT_BY_QUALIFIED,
        PositioningStrategies.DOT_BY_QUALIFIED
    )

    val SELECTOR_BY_QUALIFIED = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.SELECTOR_BY_QUALIFIED,
        PositioningStrategies.SELECTOR_BY_QUALIFIED
    )

    val REFERENCE_BY_QUALIFIED = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.REFERENCE_BY_QUALIFIED,
        PositioningStrategies.REFERENCE_BY_QUALIFIED
    )

    val REFERENCED_NAME_BY_QUALIFIED = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.REFERENCED_NAME_BY_QUALIFIED,
        PositioningStrategies.REFERENCED_NAME_BY_QUALIFIED
    )

    val WHEN_EXPRESSION = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.WHEN_EXPRESSION,
        PositioningStrategies.WHEN_EXPRESSION
    )

    val IF_EXPRESSION = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.IF_EXPRESSION,
        PositioningStrategies.IF_EXPRESSION
    )

    val ELSE_ENTRY = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.ELSE_ENTRY,
        PositioningStrategies.ELSE_ENTRY
    )

    val ARRAY_ACCESS = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.ARRAY_ACCESS,
        PositioningStrategies.ARRAY_ACCESS
    )

    val SAFE_ACCESS = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.SAFE_ACCESS,
        PositioningStrategies.SAFE_ACCESS
    )

    val AS_TYPE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.AS_TYPE,
        PositioningStrategies.AS_TYPE
    )

    val USELESS_ELVIS = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.USELESS_ELVIS,
        PositioningStrategies.USELESS_ELVIS
    )

    val RETURN_WITH_LABEL = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.RETURN_WITH_LABEL,
        PositioningStrategies.RETURN_WITH_LABEL
    )

    val ASSIGNMENT_VALUE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.LAST_CHILD,
        PositioningStrategies.ASSIGNMENT_VALUE
    )

    val WHOLE_ELEMENT = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.WHOLE_ELEMENT,
        PositioningStrategies.WHOLE_ELEMENT
    )

    val LONG_LITERAL_SUFFIX = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.LONG_LITERAL_SUFFIX,
        PositioningStrategies.LONG_LITERAL_SUFFIX
    )

    val REIFIED_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.REIFIED_MODIFIER,
        PositioningStrategies.REIFIED_MODIFIER
    )

    val TYPE_PARAMETERS_LIST = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.TYPE_PARAMETERS_LIST,
        PositioningStrategies.TYPE_PARAMETERS_LIST
    )

    val RESERVED_UNDERSCORE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.RESERVED_UNDERSCORE,
        PositioningStrategies.RESERVED_UNDERSCORE
    )
}
