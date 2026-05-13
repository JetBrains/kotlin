/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.util.inlinecodegen

import java.io.Serializable

data class LightIrType(
    val classifier: Classifier,
    val arguments: List<TypeArgument>,
    val nullable: Boolean,
    val asmTypeInternalName: String,
) : Serializable {
    val asmTypeDesc: String
        get() = when {
            asmTypeInternalName.startsWith("[") -> asmTypeInternalName
            else -> "L${asmTypeInternalName};"
        }

    sealed interface Classifier : Serializable {
        data class Clazz(
            val fqName: String,
            val inlineAbi: InlineAbi?,
            val typeOfSupportClassInstance: ClassInstance,
        ) : Classifier

        data class TypeParameter(
            val name: String,
            val index: Int,
            val variance: Char,
            val isReified: Boolean,
            val specialized: Boolean,
            val parent: Parent,
            val upperBounds: List<LightIrType>?, // null for reified type parameters
        ) : Classifier {
            sealed interface Parent : Serializable {
                data class ParentClass(val internalName: String) : Parent

                data class Function(
                    val arity: Int,
                    val owner: ClassInstance,
                    val declarationName: String,
                    val signatureString: String,
                    val topLevelFlag: Int,
                ) : Parent

                data class Property(
                    val implClassInternalName: String,
                    val owner: ClassInstance,
                    val declarationName: String,
                    val signatureString: String,
                    val topLevelFlag: Int,
                ) : Parent
            }
        }
    }

    data class InlineAbi(val unboxedDesc: String, val nullableIsBoxed: Boolean) : Serializable

    sealed interface TypeArgument : Serializable {
        class StarProjection : TypeArgument
        data class TypeProjection(val type: LightIrType, val variance: Char) : TypeArgument
        companion object {
            const val VARIANCE_INV = '-'
            const val VARIANCE_IN = 'I'
            const val VARIANCE_OUT = 'O'
        }
    }

    fun markNullable(): LightIrType = copy(nullable = true)

    fun reify(reificationArgument: ReificationArgument): LightIrType {
        var arrayWrapped = this
        repeat(reificationArgument.arrayDepth) {
            arrayWrapped = LightIrType(
                Classifier.Clazz(
                    "kotlin/Array",
                    null,
                    // TODO: is this actually correct?
                    ClassInstance.ConstClass("[" + arrayWrapped.asmTypeDesc),
                ),
                listOf(TypeArgument.TypeProjection(arrayWrapped.markNullable(), TypeArgument.VARIANCE_INV)),
                false,
                "[" + arrayWrapped.asmTypeDesc,
            )
        }
        return if (reificationArgument.nullable && !arrayWrapped.nullable) {
            arrayWrapped.markNullable()
        } else {
            arrayWrapped
        }
    }

    fun encode(): String {
        val bytes = java.io.ByteArrayOutputStream().use { bos ->
            java.io.ObjectOutputStream(bos).use { it.writeObject(this) }
            bos.toByteArray()
        }
        return java.util.Base64.getEncoder().encodeToString(bytes)
    }

    fun reify(mapping: Map<String, LightIrType>): LightIrType {
        (this.classifier as? Classifier.TypeParameter)?.name?.let { mapping[it] }?.let { parameterValue ->
            return if (nullable) parameterValue.markNullable() else parameterValue
        }

        val reifiedArgs = arguments.map {
            when (it) {
                is TypeArgument.StarProjection -> TypeArgument.StarProjection()
                is TypeArgument.TypeProjection -> TypeArgument.TypeProjection(it.type.reify(mapping), it.variance)
            }
        }

        return LightIrType(
            classifier,
            reifiedArgs,
            nullable,
            if ((classifier as? Classifier.Clazz)?.fqName == "kotlin.Array") {
                (reifiedArgs.single() as? TypeArgument.TypeProjection)
                    ?.let { "[" + it.type.asmTypeDesc }
                    ?: asmTypeInternalName
            } else {
                asmTypeInternalName
            },
        )
    }

    companion object {
        fun decode(s: String): LightIrType {
            val bytes = java.util.Base64.getDecoder().decode(s)
            return java.io.ObjectInputStream(java.io.ByteArrayInputStream(bytes)).use { it.readObject() as LightIrType }
        }

        fun decodeTypeParameters(str: String): Map<Int, LightIrType> {
            val map = HashMap<Int, LightIrType>()
            for (line in str.lines()) {
                if (line.isEmpty()) continue
                val eqIdx = line.indexOf('=')
                val key = line.substring(0, eqIdx).toInt()
                val value = line.substring(eqIdx + 1)
                map[key] = decode(value)
            }
            return map
        }

        fun encodeTypeParameters(map: Map<Int, LightIrType>): String {
            return map.entries.joinToString("\n") { (k, v) -> "$k=${v.encode()}" }
        }
    }
}

sealed interface ClassInstance : Serializable {
    data class ConstClass(val descriptor: String) : ClassInstance
    data class StaticOf(val internalName: String) : ClassInstance
}
