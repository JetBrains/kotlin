/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java

import org.jetbrains.kotlin.load.kotlin.SignatureBuildingComponents
import org.jetbrains.kotlin.load.kotlin.signatures
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType


open class SpecialGenericSignatures {
    enum class TypeSafeBarrierDescription(val defaultValue: Any?) {
        NULL(null), INDEX(-1), FALSE(false),

        MAP_GET_OR_DEFAULT(null) {
            override fun checkParameter(index: Int) = index != 1
        };

        open fun checkParameter(index: Int) = true
    }

    enum class SpecialSignatureInfo(val valueParametersSignature: String?, val isObjectReplacedWithTypeParameter: Boolean) {
        ONE_COLLECTION_PARAMETER("Ljava/util/Collection<+Ljava/lang/Object;>;", false),
        OBJECT_PARAMETER_NON_GENERIC(null, true),
        OBJECT_PARAMETER_GENERIC("Ljava/lang/Object;", true)
    }

    companion object {
        fun getSpecialSignatureInfo(builtinSignature: String): SpecialSignatureInfo {
            if (builtinSignature in ERASED_COLLECTION_PARAMETER_SIGNATURES) return SpecialSignatureInfo.ONE_COLLECTION_PARAMETER

            val defaultValue = SIGNATURE_TO_DEFAULT_VALUES_MAP.getValue(builtinSignature)

            return if (defaultValue == TypeSafeBarrierDescription.NULL) {
                // return type is some generic type as 'Map.get'
                SpecialSignatureInfo.OBJECT_PARAMETER_GENERIC
            } else
                SpecialSignatureInfo.OBJECT_PARAMETER_NON_GENERIC
        }

        data class NameAndSignature(val classInternalName: String, val name: Name, val parameters: String, val returnType: String) {
            val signature = SignatureBuildingComponents.signature(classInternalName, "$name($parameters)$returnType")
        }

        private fun String.method(name: String, parameters: String, returnType: String) =
            NameAndSignature(
                this@method,
                Name.identifier(name),
                parameters, returnType,
            )

        private val ERASED_COLLECTION_PARAMETER_NAME_AND_SIGNATURES = setOf(
            "containsAll", "removeAll", "retainAll"
        ).map { "java/util/Collection".method(it, "Ljava/util/Collection;", JvmPrimitiveType.BOOLEAN.desc) }

        val ERASED_COLLECTION_PARAMETER_SIGNATURES = ERASED_COLLECTION_PARAMETER_NAME_AND_SIGNATURES.map { it.signature }
        val ERASED_COLLECTION_PARAMETER_NAMES = ERASED_COLLECTION_PARAMETER_NAME_AND_SIGNATURES.map { it.name.asString() }

        private val GENERIC_PARAMETERS_METHODS_TO_DEFAULT_VALUES_MAP =
            signatures {
                mapOf(
                    javaUtil("Collection")
                        .method("contains", "Ljava/lang/Object;", JvmPrimitiveType.BOOLEAN.desc) to TypeSafeBarrierDescription.FALSE,
                    javaUtil("Collection")
                        .method("remove", "Ljava/lang/Object;", JvmPrimitiveType.BOOLEAN.desc) to TypeSafeBarrierDescription.FALSE,

                    javaUtil("Map")
                        .method("containsKey", "Ljava/lang/Object;", JvmPrimitiveType.BOOLEAN.desc) to TypeSafeBarrierDescription.FALSE,
                    javaUtil("Map")
                        .method("containsValue", "Ljava/lang/Object;", JvmPrimitiveType.BOOLEAN.desc) to TypeSafeBarrierDescription.FALSE,
                    javaUtil("Map")
                        .method(
                            "remove", "Ljava/lang/Object;Ljava/lang/Object;",
                            JvmPrimitiveType.BOOLEAN.desc
                        ) to TypeSafeBarrierDescription.FALSE,

                    javaUtil("Map")
                        .method(
                            "getOrDefault", "Ljava/lang/Object;Ljava/lang/Object;",
                            "Ljava/lang/Object;"
                        ) to TypeSafeBarrierDescription.MAP_GET_OR_DEFAULT,

                    javaUtil("Map")
                        .method("get", "Ljava/lang/Object;", "Ljava/lang/Object;") to TypeSafeBarrierDescription.NULL,
                    javaUtil("Map")
                        .method("remove", "Ljava/lang/Object;", "Ljava/lang/Object;") to TypeSafeBarrierDescription.NULL,

                    javaUtil("List")
                        .method("indexOf", "Ljava/lang/Object;", JvmPrimitiveType.INT.desc) to TypeSafeBarrierDescription.INDEX,
                    javaUtil("List")
                        .method("lastIndexOf", "Ljava/lang/Object;", JvmPrimitiveType.INT.desc) to TypeSafeBarrierDescription.INDEX
                )
            }

        val SIGNATURE_TO_DEFAULT_VALUES_MAP = GENERIC_PARAMETERS_METHODS_TO_DEFAULT_VALUES_MAP.mapKeys { it.key.signature }
        val ERASED_VALUE_PARAMETERS_SHORT_NAMES: Set<Name>
        val ERASED_VALUE_PARAMETERS_SIGNATURES: Set<String>

        init {
            val allMethods = GENERIC_PARAMETERS_METHODS_TO_DEFAULT_VALUES_MAP.keys + ERASED_COLLECTION_PARAMETER_NAME_AND_SIGNATURES
            ERASED_VALUE_PARAMETERS_SHORT_NAMES = allMethods.map { it.name }.toSet()
            ERASED_VALUE_PARAMETERS_SIGNATURES = allMethods.map { it.signature }.toSet()
        }

        // Note that signatures here are not real,
        // e.g. 'java/lang/CharSequence.get(I)C' does not actually exist in JDK
        // But it doesn't matter here, because signatures are only used to distinguish overloaded built-in definitions
        val REMOVE_AT_NAME_AND_SIGNATURE =
            "java/util/List".method("removeAt", JvmPrimitiveType.INT.desc, "Ljava/lang/Object;")

        private val NAME_AND_SIGNATURE_TO_JVM_REPRESENTATION_NAME_MAP: Map<NameAndSignature, Name> = signatures {
            mapOf(
                javaLang("Number").method("toByte", "", JvmPrimitiveType.BYTE.desc) to Name.identifier("byteValue"),
                javaLang("Number").method("toShort", "", JvmPrimitiveType.SHORT.desc) to Name.identifier("shortValue"),
                javaLang("Number").method("toInt", "", JvmPrimitiveType.INT.desc) to Name.identifier("intValue"),
                javaLang("Number").method("toLong", "", JvmPrimitiveType.LONG.desc) to Name.identifier("longValue"),
                javaLang("Number").method("toFloat", "", JvmPrimitiveType.FLOAT.desc) to Name.identifier("floatValue"),
                javaLang("Number").method("toDouble", "", JvmPrimitiveType.DOUBLE.desc) to Name.identifier("doubleValue"),
                REMOVE_AT_NAME_AND_SIGNATURE to Name.identifier("remove"),
                javaLang("CharSequence")
                    .method("get", JvmPrimitiveType.INT.desc, JvmPrimitiveType.CHAR.desc) to Name.identifier("charAt"),

                javaUtilConcurrentAtomic("AtomicInteger").method("load", "", "I") to Name.identifier("get"),
                javaUtilConcurrentAtomic("AtomicInteger").method("store", "I", "V") to Name.identifier("set"),
                javaUtilConcurrentAtomic("AtomicInteger").method("exchange", "I", "I") to Name.identifier("getAndSet"),
                javaUtilConcurrentAtomic("AtomicInteger").method("fetchAndAdd", "I", "I") to Name.identifier("getAndAdd"),
                javaUtilConcurrentAtomic("AtomicInteger").method("addAndFetch", "I", "I") to Name.identifier("addAndGet"),

                javaUtilConcurrentAtomic("AtomicLong").method("load", "", "J") to Name.identifier("get"),
                javaUtilConcurrentAtomic("AtomicLong").method("store", "J", "V") to Name.identifier("set"),
                javaUtilConcurrentAtomic("AtomicLong").method("exchange", "J", "J") to Name.identifier("getAndSet"),
                javaUtilConcurrentAtomic("AtomicLong").method("fetchAndAdd", "J", "J") to Name.identifier("getAndAdd"),
                javaUtilConcurrentAtomic("AtomicLong").method("addAndFetch", "J", "J") to Name.identifier("addAndGet"),

                javaUtilConcurrentAtomic("AtomicBoolean").method("load", "", "Z") to Name.identifier("get"),
                javaUtilConcurrentAtomic("AtomicBoolean").method("store", "Z", "V") to Name.identifier("set"),
                javaUtilConcurrentAtomic("AtomicBoolean").method("exchange", "Z", "Z") to Name.identifier("getAndSet"),

                javaUtilConcurrentAtomic("AtomicReference").method("load", "", "Ljava/lang/Object;") to Name.identifier("get"),
                javaUtilConcurrentAtomic("AtomicReference").method("store", "Ljava/lang/Object;", "V") to Name.identifier("set"),
                javaUtilConcurrentAtomic("AtomicReference").method("exchange", "Ljava/lang/Object;", "Ljava/lang/Object;") to Name.identifier("getAndSet"),

                javaUtilConcurrentAtomic("AtomicIntegerArray").method("loadAt", "I", "I") to Name.identifier("get"),
                javaUtilConcurrentAtomic("AtomicIntegerArray").method("storeAt", "II", "V") to Name.identifier("set"),
                javaUtilConcurrentAtomic("AtomicIntegerArray").method("exchangeAt", "II", "I") to Name.identifier("getAndSet"),
                javaUtilConcurrentAtomic("AtomicIntegerArray").method("compareAndSetAt", "III", "Z") to Name.identifier("compareAndSet"),
                javaUtilConcurrentAtomic("AtomicIntegerArray").method("fetchAndAddAt", "II", "I") to Name.identifier("getAndAdd"),
                javaUtilConcurrentAtomic("AtomicIntegerArray").method("addAndFetchAt", "II", "I") to Name.identifier("addAndGet"),

                javaUtilConcurrentAtomic("AtomicLongArray").method("loadAt", "I", "J") to Name.identifier("get"),
                javaUtilConcurrentAtomic("AtomicLongArray").method("storeAt", "IJ", "V") to Name.identifier("set"),
                javaUtilConcurrentAtomic("AtomicLongArray").method("exchangeAt", "IJ", "J") to Name.identifier("getAndSet"),
                javaUtilConcurrentAtomic("AtomicLongArray").method("compareAndSetAt", "IJJ", "Z") to Name.identifier("compareAndSet"),
                javaUtilConcurrentAtomic("AtomicLongArray").method("fetchAndAddAt", "IJ", "J") to Name.identifier("getAndAdd"),
                javaUtilConcurrentAtomic("AtomicLongArray").method("addAndFetchAt", "IJ", "J") to Name.identifier("addAndGet"),

                javaUtilConcurrentAtomic("AtomicReferenceArray").method("loadAt", "I", "Ljava/lang/Object;") to Name.identifier("get"),
                javaUtilConcurrentAtomic("AtomicReferenceArray").method("storeAt", "ILjava/lang/Object;", "V") to Name.identifier("set"),
                javaUtilConcurrentAtomic("AtomicReferenceArray").method("exchangeAt", "ILjava/lang/Object;", "Ljava/lang/Object;") to Name.identifier("getAndSet"),
                javaUtilConcurrentAtomic("AtomicReferenceArray").method("compareAndSetAt", "ILjava/lang/Object;Ljava/lang/Object;", "Z") to Name.identifier("compareAndSet"),
            )
        }

        val SIGNATURE_TO_JVM_REPRESENTATION_NAME: Map<String, Name> =
            NAME_AND_SIGNATURE_TO_JVM_REPRESENTATION_NAME_MAP.mapKeys { it.key.signature }

        // java/lang/Number.intValue()I, java/lang/ etc.
        val JVM_SIGNATURES_FOR_RENAMED_BUILT_INS: Set<String> =
            NAME_AND_SIGNATURE_TO_JVM_REPRESENTATION_NAME_MAP.mapTo(mutableSetOf()) { (signatureAndName, jdkName) ->
                signatureAndName.copy(name = jdkName).signature
            }

        val ORIGINAL_SHORT_NAMES: Set<Name> = NAME_AND_SIGNATURE_TO_JVM_REPRESENTATION_NAME_MAP.keys.mapTo(HashSet()) { it.name }

        val JVM_SHORT_NAME_TO_BUILTIN_SHORT_NAMES_MAP: Map<Name, Name> =
            NAME_AND_SIGNATURE_TO_JVM_REPRESENTATION_NAME_MAP.entries
                .map { Pair(it.key.name, it.value) }
                .associateBy({ it.second }, { it.first })

        fun getBuiltinFunctionNamesByJvmName(name: Name): Name? =
            JVM_SHORT_NAME_TO_BUILTIN_SHORT_NAMES_MAP[name]

        val Name.sameAsBuiltinMethodWithErasedValueParameters: Boolean
            get() = this in ERASED_VALUE_PARAMETERS_SHORT_NAMES

        val Name.sameAsRenamedInJvmBuiltin: Boolean
            get() = this in ORIGINAL_SHORT_NAMES

    }
}
