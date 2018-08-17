// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl

import com.intellij.debugger.streams.trace.dsl.Types
import com.intellij.debugger.streams.trace.impl.handler.type.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.FQ_NAMES

object KotlinSequenceTypes : Types {
    override val ANY: GenericType = ClassTypeImpl(FQ_NAMES.any.asString(), "kotlin.Any()")

    override val BOOLEAN: GenericType = ClassTypeImpl(FQ_NAMES._boolean.asString(), "false")
    val BYTE: GenericType = ClassTypeImpl(FQ_NAMES._byte.asString(), "0")
    val SHORT: GenericType = ClassTypeImpl(FQ_NAMES._short.asString(), "0")
    val CHAR: GenericType = ClassTypeImpl(FQ_NAMES._char.asString(), "0.toChar()")
    override val INT: GenericType = ClassTypeImpl(FQ_NAMES._int.asString(), "0")
    override val LONG: GenericType = ClassTypeImpl(FQ_NAMES._long.asString(), "0L")
    val FLOAT: GenericType = ClassTypeImpl(FQ_NAMES._float.asString(), "0.0f")
    override val DOUBLE: GenericType = ClassTypeImpl(FQ_NAMES._double.asString(), "0.0")
    override val STRING: GenericType = ClassTypeImpl(FQ_NAMES.string.asString(), "\"\"")
    override val EXCEPTION: GenericType = ClassTypeImpl(FQ_NAMES.throwable.asString(), "kotlin.Throwable()")
    override val VOID: GenericType = ClassTypeImpl(FQ_NAMES.unit.asString(), "Unit")

    val NULLABLE_ANY: GenericType = nullable { ANY }

    override val TIME: GenericType = ClassTypeImpl(
        "java.util.concurrent.atomic.AtomicInteger",
        "java.util.concurrent.atomic.AtomicInteger()"
    )

    override fun list(elementsType: GenericType): ListType =
        ListTypeImpl(elementsType, { "kotlin.collections.MutableList<$it>" }, "kotlin.collections.mutableListOf()")

    override fun array(elementType: GenericType): ArrayType = when (elementType) {
        BOOLEAN -> ArrayTypeImpl(BOOLEAN, { "kotlin.BooleanArray" }, { "kotlin.BooleanArray($it)" })
        BYTE -> ArrayTypeImpl(BYTE, { "kotlin.ByteArray" }, { "kotlin.ByteArray($it)" })
        SHORT -> ArrayTypeImpl(SHORT, { "kotlin.ShortArray" }, { "kotlin.ShortArray($it)" })
        CHAR -> ArrayTypeImpl(CHAR, { "kotlin.CharArray" }, { "kotlin.CharArray($it)" })
        INT -> ArrayTypeImpl(INT, { "kotlin.IntArray" }, { "kotlin.IntArray($it)" })
        LONG -> ArrayTypeImpl(LONG, { "kotlin.LongArray" }, { "kotlin.LongArray($it)" })
        FLOAT -> ArrayTypeImpl(FLOAT, { "kotlin.FloatArray" }, { "kotlin.FloatArray($it)" })
        DOUBLE -> ArrayTypeImpl(DOUBLE, { "kotlin.DoubleArray" }, { "kotlin.DoubleArray($it)" })
        else -> ArrayTypeImpl(nullable { elementType }, { "kotlin.Array<$it>" },
                              { "kotlin.arrayOfNulls<${elementType.genericTypeName}>($it)" })
    }

    override fun map(keyType: GenericType, valueType: GenericType): MapType =
        MapTypeImpl(
            keyType, valueType,
            { keys, values -> "kotlin.collections.MutableMap<$keys, $values>" },
            "kotlin.collections.mutableMapOf()"
        )

    override fun linkedMap(keyType: GenericType, valueType: GenericType): MapType =
        MapTypeImpl(
            keyType, valueType,
            { keys, values -> "kotlin.collections.MutableMap<$keys, $values>" },
            "kotlin.collections.linkedMapOf()"
        )

    override fun nullable(typeSelector: Types.() -> GenericType): GenericType {
        val type = this.typeSelector()
        if (type.genericTypeName.last() == '?') return type
        return when (type) {
            is ArrayType -> ArrayTypeImpl(type.elementType, { "kotlin.Array<$it>?" }, { type.sizedDeclaration(it) })
            is ListType -> ListTypeImpl(type.elementType, { "kotlin.collections.MutableList<$it>?" }, type.defaultValue)
            is MapType -> MapTypeImpl(
                type.keyType, type.valueType, { keys, values -> "kotlin.collections.MutableMap<$keys, $values>?" },
                type.defaultValue
            )
            else -> ClassTypeImpl(type.genericTypeName + '?', type.defaultValue)
        }
    }

    private val primitiveTypesIndex: Map<String, GenericType> =
        listOf(
            BOOLEAN, BYTE, INT, SHORT,
            CHAR, LONG, FLOAT, DOUBLE
        )
            .associate { it.genericTypeName to it }

    private val primitiveArraysIndex: Map<String, ArrayType> = primitiveTypesIndex.asSequence()
        .map { array(it.value) }
        .associate { it.genericTypeName to it }

    fun primitiveTypeByName(typeName: String): GenericType? = primitiveTypesIndex[typeName]

    fun primitiveArrayByName(typeName: String): ArrayType? = primitiveArraysIndex[typeName]
}

