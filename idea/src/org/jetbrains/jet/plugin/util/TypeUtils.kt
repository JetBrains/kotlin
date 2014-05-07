package org.jetbrains.jet.plugin.util

import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.types.TypeUtils

fun JetType.makeNullable() = TypeUtils.makeNullable(this)
fun JetType.makeNotNullable() = TypeUtils.makeNotNullable(this)
