/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.util.inlinecodegen

import org.jetbrains.org.objectweb.asm.Opcodes
import java.io.Serializable
import kotlin.io.encoding.Base64

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
        return Base64.encode(bytes)
    }

    fun reify(mapping: Map<Int, LightIrType>): LightIrType {
        (this.classifier as? Classifier.TypeParameter)?.index?.let { mapping[it] }?.let { parameterValue ->
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

    fun specializedAbi(): SpecializedTypeAbi? {
        val classifier = classifier as? Classifier.Clazz ?: return null

        // Simple non-null primitive casse
        if (!nullable) when (classifier.fqName) {
            "kotlin.Boolean" -> return Primitive("Z", "boolean", "java/lang/Boolean", 0, Opcodes.ICONST_0)
            "kotlin.Char" -> return Primitive("C", "char", "java/lang/Character", 0, Opcodes.ICONST_0)
            "kotlin.Byte" -> return Primitive("B", "byte", "java/lang/Byte", 0, Opcodes.ICONST_0)
            "kotlin.Short" -> return Primitive("S", "short", "java/lang/Short", 0, Opcodes.ICONST_0)
            "kotlin.Int" -> return Primitive("I", "int", "java/lang/Integer", 0, Opcodes.ICONST_0)
            "kotlin.Float" -> return Primitive("F", "float", "java/lang/Float", 2, Opcodes.FCONST_0)
            "kotlin.Long" -> return Primitive("J", "long", "java/lang/Long", 1, Opcodes.LCONST_0)
            "kotlin.Double" -> return Primitive("D", "double", "java/lang/Double", 3, Opcodes.DCONST_0)
        }

        // Inline value class
        if (classifier.inlineAbi != null) {
            return InlineClass(
                classifier.fqName.replace('.', '/'),
                nullable,
                classifier.inlineAbi,
            )
        }

        return null
    }

    companion object {
        fun decode(s: String): LightIrType {
            val bytes = Base64.decode(s)
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
