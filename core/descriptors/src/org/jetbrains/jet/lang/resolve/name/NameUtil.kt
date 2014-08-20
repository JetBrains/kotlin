package org.jetbrains.jet.lang.resolve.name

import java.util.regex.Pattern

public val COMPONENT_FUNCTION_PATTERN: Pattern = Pattern.compile("^component(\\d+)$")

public fun String.isComponentFunctionName(): Boolean = COMPONENT_FUNCTION_PATTERN.matcher(this).matches()
public fun Name.isComponentFunctionName(): Boolean = asString().isComponentFunctionName()