/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    INT -> ArrayTypeImpl(INT, { "kotlin.IntArray" }, { "kotlin.IntArray($it)" })
    LONG -> ArrayTypeImpl(LONG, { "kotlin.LongArray" }, { "kotlin.LongArray($it)" })
    DOUBLE -> ArrayTypeImpl(DOUBLE, { "kotlin.DoubleArray" }, { "kotlin.DoubleArray($it)" })
    else -> ArrayTypeImpl(nullable { elementType }, { "kotlin.Array<$it>" }, { "kotlin.arrayOfNulls($it)" })
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

