// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.trace.dsl

import com.intellij.debugger.streams.trace.dsl.Types
import com.intellij.debugger.streams.trace.impl.handler.type.*

/**
 * @author Vitaliy.Bibaev
 */
object KotlinTypes : Types {
  override val ANY: GenericType = ClassTypeImpl("kotlin.Any", "kotlin.Any()")
  override val INT: GenericType = ClassTypeImpl("kotlin.Int", "0")
  override val LONG: GenericType = ClassTypeImpl("kotlin.Long", "0L")
  override val BOOLEAN: GenericType = ClassTypeImpl("kotlin.Boolean", "false")
  override val DOUBLE: GenericType = ClassTypeImpl("kotlin.Double", "0.")
  override val STRING: GenericType = ClassTypeImpl("kotlin.String", "\"\"")
  override val EXCEPTION: GenericType = ClassTypeImpl("kotlin.Throwable", "kotlin.Throwable()")
  override val VOID: GenericType = ClassTypeImpl("kotlin.Unit", "Unit")

  override val TIME: GenericType = ClassTypeImpl("java.util.concurrent.atomic.AtomicInteger",
      "java.util.concurrent.atomic.AtomicInteger()")

  override fun list(elementsType: GenericType): ListType =
      ListTypeImpl(elementsType, { "kotlin.collections.MutableList<$it>" }, "kotlin.collections.mutableListOf()")

  override fun array(elementType: GenericType): ArrayType = when (elementType) {
    BOOLEAN -> ArrayTypeImpl(BOOLEAN, { "kotlin.BooleanArray" }, { "kotlin.BooleanArray($it)" })
    INT -> ArrayTypeImpl(INT, { "kotlin.IntArray" }, { "kotlin.IntArray($it)" })
    LONG -> ArrayTypeImpl(LONG, { "kotlin.LongArray" }, { "kotlin.LongArray($it)" })
    DOUBLE -> ArrayTypeImpl(DOUBLE, { "kotlin.DoubleArray" }, { "kotlin.DoubleArray($it)" })
    else -> ArrayTypeImpl(nullable { elementType }, { "kotlin.Array<$it>" },
        { "kotlin.arrayOfNulls<${elementType.genericTypeName}>($it)" })
  }

  override fun map(keyType: GenericType, valueType: GenericType): MapType =
      MapTypeImpl(keyType, valueType,
          { keys, values -> "kotlin.collections.MutableMap<$keys, $values>" },
          "kotlin.collections.mutableMapOf()")

  override fun linkedMap(keyType: GenericType, valueType: GenericType): MapType =
      MapTypeImpl(keyType, valueType,
          { keys, values -> "kotlin.collections.MutableMap<$keys, $values>" },
          "kotlin.collections.linkedMapOf()")

  override fun nullable(typeSelector: Types.() -> GenericType): GenericType {
    val type = this.typeSelector()
    if (type.genericTypeName.last() == '?') return type
    return when (type) {
      is ArrayType -> ArrayTypeImpl(type.elementType, { "kotlin.Array<$it>?" }, { type.sizedDeclaration(it) })
      is ListType -> ListTypeImpl(type.elementType, { "kotlin.collections.MutableList<$it>?" }, type.defaultValue)
      is MapType -> MapTypeImpl(type.keyType, type.valueType, { keys, values -> "kotlin.collections.MutableMap<$keys, $values>?" },
          type.defaultValue)
      else -> ClassTypeImpl(type.genericTypeName + '?', type.defaultValue)
    }
  }
}

