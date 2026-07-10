/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.util.inlinecodegen

import org.jetbrains.org.objectweb.asm.Opcodes
import java.io.Serializable
import kotlin.io.encoding.Base64

@JvmInline
value class LightIrTypeArguments(val arguments: Map<Int, LightIrType>) : Serializable {
    fun reify(otherArguments: LightIrTypeArguments): LightIrTypeArguments {
        return LightIrTypeArguments(arguments.mapValues { it.value.reify(otherArguments) })
    }

    fun encode(): String {
        val bytes = java.io.ByteArrayOutputStream().use { bos ->
            java.io.ObjectOutputStream(bos).use { it.writeObject(this.arguments) }
            bos.toByteArray()
        }
        return Base64.encode(bytes)
    }

    companion object {
        fun decode(s: String): LightIrTypeArguments {
            val bytes = Base64.decode(s)
            val arguments = java.io.ObjectInputStream(java.io.ByteArrayInputStream(bytes))
                .use { @Suppress("UNCHECKED_CAST") (it.readObject() as Map<Int, LightIrType>) }
            return LightIrTypeArguments(arguments)
        }
    }
}

data class LightIrType(
    val nullable: Boolean,
    val asmTypeInternalName: String,
    val unreified: LightIrType?,
) : Serializable {
    lateinit var classifier: Classifier
    lateinit var arguments: List<TypeArgument>

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

    data class InlineAbi(
        val unboxedDesc: String,
        val nullableIsBoxed: Boolean,
        val replacements: List<Replacement>,
    ) : Serializable {
        data class Replacement(
            val isInterface: Boolean,
            val repName: String,
            val repDesc: String,
            val origName: String,
            val origDesc: String,
            val changedParameters: List<Pair<Int, LightIrType>>,
            val changedReturnType: LightIrType?,
        ) : Serializable
    }

    sealed interface TypeArgument : Serializable {
        class StarProjection : TypeArgument
        data class TypeProjection(val type: LightIrType, val variance: Char) : TypeArgument
        companion object {
            const val VARIANCE_INV = '-'
            const val VARIANCE_IN = 'I'
            const val VARIANCE_OUT = 'O'
        }
    }

    fun markNullable(): LightIrType =
        copy(nullable = true).also { it.classifier = classifier; it.arguments = arguments }

    private fun markUnreified(unreified: LightIrType): LightIrType =
        copy(unreified = unreified).also { it.classifier = classifier; it.arguments = arguments }

    fun reify(reificationArgument: ReificationArgument): LightIrType {
        var arrayWrapped = this
        repeat(reificationArgument.arrayDepth) {
            arrayWrapped = LightIrType(
                false,
                "[" + arrayWrapped.asmTypeDesc,
                null,
            ).apply {
                classifier = Classifier.Clazz(
                    "kotlin/Array",
                    null,
                    // TODO: is this actually correct?
                    ClassInstance.ConstClass("[" + arrayWrapped.asmTypeDesc),
                )
                arguments = listOf(TypeArgument.TypeProjection(arrayWrapped.markNullable(), TypeArgument.VARIANCE_INV))
            }
        }
        return if (reificationArgument.nullable && !arrayWrapped.nullable) {
            arrayWrapped.markNullable()
        } else {
            arrayWrapped
        }
    }

    fun reify(mapping: LightIrTypeArguments): LightIrType {
        when (val classifier = this.classifier) {
            is Classifier.TypeParameter -> {
                mapping.arguments[classifier.index]
                    ?.let { if (nullable && !it.nullable) it.markNullable() else it }
                    ?.let { if (!classifier.isReified) it.markUnreified(this) else it }
                    ?.also { return it }
                return this
            }
            is Classifier.Clazz -> {
                val reifiedArgs = arguments.map {
                    when (it) {
                        is TypeArgument.StarProjection -> TypeArgument.StarProjection()
                        is TypeArgument.TypeProjection -> TypeArgument.TypeProjection(it.type.reify(mapping), it.variance)
                    }
                }

                val reifiedAsmTypeInternalName = if (classifier.fqName == "kotlin.Array") {
                    (reifiedArgs.single() as? TypeArgument.TypeProjection)
                        ?.let { "[" + it.type.asmTypeDesc }
                } else {
                    null
                }

                return copy(
                    asmTypeInternalName = reifiedAsmTypeInternalName ?: asmTypeInternalName,
                ).also {
                    it.classifier = classifier
                    it.arguments = reifiedArgs
                }
            }
        }
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
}

sealed interface ClassInstance : Serializable {
    data class ConstClass(val descriptor: String) : ClassInstance
    data class StaticOf(val internalName: String) : ClassInstance
}
