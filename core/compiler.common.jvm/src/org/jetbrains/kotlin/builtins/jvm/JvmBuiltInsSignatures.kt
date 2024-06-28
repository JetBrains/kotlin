/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.builtins.jvm

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.load.kotlin.SignatureBuildingComponents
import org.jetbrains.kotlin.load.kotlin.signatures
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import java.io.Serializable

object JvmBuiltInsSignatures {
    val DROP_LIST_METHOD_SIGNATURES: Set<String> =
        SignatureBuildingComponents.inJavaUtil(
            "Collection",
            "toArray()[Ljava/lang/Object;", "toArray([Ljava/lang/Object;)[Ljava/lang/Object;"
        ) + "java/lang/annotation/Annotation.annotationType()Ljava/lang/Class;"

    val HIDDEN_METHOD_SIGNATURES: Set<String> =
        signatures {
            buildPrimitiveValueMethodsSet() +

                    inJavaUtil(
                        "List",
                        "sort(Ljava/util/Comparator;)V",
                        // From JDK 21
                        "reversed()Ljava/util/List;",
                    ) +

                    inJavaLang(
                        "String",
                        "codePointAt(I)I", "codePointBefore(I)I", "codePointCount(II)I", "compareToIgnoreCase(Ljava/lang/String;)I",
                        "concat(Ljava/lang/String;)Ljava/lang/String;", "contains(Ljava/lang/CharSequence;)Z",
                        "contentEquals(Ljava/lang/CharSequence;)Z", "contentEquals(Ljava/lang/StringBuffer;)Z",
                        "endsWith(Ljava/lang/String;)Z", "equalsIgnoreCase(Ljava/lang/String;)Z", "getBytes()[B", "getBytes(II[BI)V",
                        "getBytes(Ljava/lang/String;)[B", "getBytes(Ljava/nio/charset/Charset;)[B", "getChars(II[CI)V",
                        "indexOf(I)I", "indexOf(II)I", "indexOf(Ljava/lang/String;)I", "indexOf(Ljava/lang/String;I)I",
                        "intern()Ljava/lang/String;", "isEmpty()Z", "lastIndexOf(I)I", "lastIndexOf(II)I",
                        "lastIndexOf(Ljava/lang/String;)I", "lastIndexOf(Ljava/lang/String;I)I", "matches(Ljava/lang/String;)Z",
                        "offsetByCodePoints(II)I", "regionMatches(ILjava/lang/String;II)Z", "regionMatches(ZILjava/lang/String;II)Z",
                        "replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", "replace(CC)Ljava/lang/String;",
                        "replaceFirst(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                        "replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;",
                        "split(Ljava/lang/String;I)[Ljava/lang/String;", "split(Ljava/lang/String;)[Ljava/lang/String;",
                        "startsWith(Ljava/lang/String;I)Z", "startsWith(Ljava/lang/String;)Z", "substring(II)Ljava/lang/String;",
                        "substring(I)Ljava/lang/String;", "toCharArray()[C", "toLowerCase()Ljava/lang/String;",
                        "toLowerCase(Ljava/util/Locale;)Ljava/lang/String;", "toUpperCase()Ljava/lang/String;",
                        "toUpperCase(Ljava/util/Locale;)Ljava/lang/String;", "trim()Ljava/lang/String;",
                        "isBlank()Z", "lines()Ljava/util/stream/Stream;", "repeat(I)Ljava/lang/String;"
                    ) +

                    inJavaLang("Double", "isInfinite()Z", "isNaN()Z") +
                    inJavaLang("Float", "isInfinite()Z", "isNaN()Z") +

                    inJavaLang("Enum", "getDeclaringClass()Ljava/lang/Class;", "finalize()V") +
                    inJavaLang("CharSequence", "isEmpty()Z")

        }

    private fun buildPrimitiveValueMethodsSet(): Set<String> =
        signatures {
            listOf(JvmPrimitiveType.BOOLEAN, JvmPrimitiveType.CHAR).flatMapTo(LinkedHashSet()) {
                inJavaLang(it.wrapperFqName.shortName().asString(), "${it.javaKeywordName}Value()${it.desc}")
            }
        }

    val DEPRECATED_LIST_METHODS: Set<String> =
        signatures {
            inJavaUtil(
                "List",
                "getFirst()Ljava/lang/Object;",
                "getLast()Ljava/lang/Object;",
            )
        }

    val VISIBLE_METHOD_SIGNATURES: Set<String> =
        signatures {
            inJavaLang(
                "CharSequence",
                "codePoints()Ljava/util/stream/IntStream;", "chars()Ljava/util/stream/IntStream;"
            ) +

                    inJavaUtil(
                        "Iterator",
                        "forEachRemaining(Ljava/util/function/Consumer;)V"
                    ) +

                    inJavaLang(
                        "Iterable",
                        "forEach(Ljava/util/function/Consumer;)V", "spliterator()Ljava/util/Spliterator;"
                    ) +

                    inJavaLang(
                        "Throwable",
                        "setStackTrace([Ljava/lang/StackTraceElement;)V", "fillInStackTrace()Ljava/lang/Throwable;",
                        "getLocalizedMessage()Ljava/lang/String;", "printStackTrace()V", "printStackTrace(Ljava/io/PrintStream;)V",
                        "printStackTrace(Ljava/io/PrintWriter;)V", "getStackTrace()[Ljava/lang/StackTraceElement;",
                        "initCause(Ljava/lang/Throwable;)Ljava/lang/Throwable;", "getSuppressed()[Ljava/lang/Throwable;",
                        "addSuppressed(Ljava/lang/Throwable;)V"
                    ) +

                    inJavaUtil(
                        "Collection",
                        "spliterator()Ljava/util/Spliterator;", "parallelStream()Ljava/util/stream/Stream;",
                        "stream()Ljava/util/stream/Stream;", "removeIf(Ljava/util/function/Predicate;)Z"
                    ) +

                    inJavaUtil(
                        "List",
                        "replaceAll(Ljava/util/function/UnaryOperator;)V",
                        // From JDK 21
                        "addFirst(Ljava/lang/Object;)V",
                        "addLast(Ljava/lang/Object;)V",
                        "removeFirst()Ljava/lang/Object;",
                        "removeLast()Ljava/lang/Object;",
                    ) +

                    inJavaUtil(
                        "Map",
                        "getOrDefault(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                        "forEach(Ljava/util/function/BiConsumer;)V", "replaceAll(Ljava/util/function/BiFunction;)V",
                        "merge(Ljava/lang/Object;Ljava/lang/Object;Ljava/util/function/BiFunction;)Ljava/lang/Object;",
                        "computeIfPresent(Ljava/lang/Object;Ljava/util/function/BiFunction;)Ljava/lang/Object;",
                        "putIfAbsent(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                        "replace(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z",
                        "replace(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                        "computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;",
                        "compute(Ljava/lang/Object;Ljava/util/function/BiFunction;)Ljava/lang/Object;"
                    )
        }

    val MUTABLE_METHOD_SIGNATURES: Set<String> =
        signatures {
            inJavaUtil("Collection", "removeIf(Ljava/util/function/Predicate;)Z") +

                    inJavaUtil(
                        "List",
                        "replaceAll(Ljava/util/function/UnaryOperator;)V",
                        "sort(Ljava/util/Comparator;)V",
                        "addFirst(Ljava/lang/Object;)V",
                        "addLast(Ljava/lang/Object;)V",
                        "removeFirst()Ljava/lang/Object;",
                        "removeLast()Ljava/lang/Object;",
                    ) +

                    inJavaUtil(
                        "Map",
                        "computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;",
                        "computeIfPresent(Ljava/lang/Object;Ljava/util/function/BiFunction;)Ljava/lang/Object;",
                        "compute(Ljava/lang/Object;Ljava/util/function/BiFunction;)Ljava/lang/Object;",
                        "merge(Ljava/lang/Object;Ljava/lang/Object;Ljava/util/function/BiFunction;)Ljava/lang/Object;",
                        "putIfAbsent(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                        "remove(Ljava/lang/Object;Ljava/lang/Object;)Z", "replaceAll(Ljava/util/function/BiFunction;)V",
                        "replace(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                        "replace(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z"
                    )
        }

    val HIDDEN_CONSTRUCTOR_SIGNATURES: Set<String> =
        signatures {
            buildPrimitiveStringConstructorsSet() +
                    inJavaLang("Float", *constructors("D")) +
                    inJavaLang(
                        "String", *constructors(
                            "[C", "[CII", "[III", "[BIILjava/lang/String;",
                            "[BIILjava/nio/charset/Charset;",
                            "[BLjava/lang/String;",
                            "[BLjava/nio/charset/Charset;",
                            "[BII", "[B",
                            "Ljava/lang/StringBuffer;",
                            "Ljava/lang/StringBuilder;"
                        )
                    )
        }

    val VISIBLE_CONSTRUCTOR_SIGNATURES: Set<String> =
        signatures {
            inJavaLang("Throwable", *constructors("Ljava/lang/String;Ljava/lang/Throwable;ZZ"))
        }

    private fun buildPrimitiveStringConstructorsSet(): Set<String> =
        signatures {
            listOf(
                JvmPrimitiveType.BOOLEAN, JvmPrimitiveType.BYTE, JvmPrimitiveType.DOUBLE, JvmPrimitiveType.FLOAT,
                JvmPrimitiveType.BYTE, JvmPrimitiveType.INT, JvmPrimitiveType.LONG, JvmPrimitiveType.SHORT
            ).flatMapTo(LinkedHashSet()) {
                // java/lang/<Wrapper>.<init>(Ljava/lang/String;)V
                inJavaLang(it.wrapperFqName.shortName().asString(), *constructors("Ljava/lang/String;"))
            }
        }

    fun isSerializableInJava(fqName: FqNameUnsafe): Boolean {
        if (isArrayOrPrimitiveArray(fqName)) {
            return true
        }
        val javaClassId = JavaToKotlinClassMap.mapKotlinToJava(fqName) ?: return false
        val classViaReflection = try {
            Class.forName(javaClassId.asSingleFqName().asString())
        } catch (e: ClassNotFoundException) {
            return false
        }
        return Serializable::class.java.isAssignableFrom(classViaReflection)
    }

    fun isArrayOrPrimitiveArray(fqName: FqNameUnsafe): Boolean {
        return fqName == StandardNames.FqNames.array || StandardNames.isPrimitiveArray(fqName)
    }
}
