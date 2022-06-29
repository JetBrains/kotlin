/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.mapping

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.allOverridden
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodGenericSignature
import org.jetbrains.org.objectweb.asm.commons.Method

object GenericSignatureMapper {
    private enum class SignatureKind {
        NON_GENERIC, FIXED_COLLECTION_PARAMETER, FIRST_PARAMETER_ERASED
    }

    private class SignatureProcessor(
        private val builtinFqName: FqName,
        private val descriptorPrefix: String,
        private val kind: SignatureKind,
        private val checker: ((Method) -> Boolean)? = null
    ) {
        fun match(irFunction: IrSimpleFunction, method: Method): Boolean =
            method.descriptor.startsWith(descriptorPrefix) &&
                    checker?.invoke(method) != false &&
                    irFunction.allOverridden(false).any {
                        it.parentClassOrNull?.fqNameWhenAvailable == builtinFqName
                    }

        fun process(signature: String): String? = when (kind) {
            SignatureKind.NON_GENERIC -> null
            SignatureKind.FIXED_COLLECTION_PARAMETER -> "(Ljava/util/Collection<+Ljava/lang/Object;>;)Z"
            SignatureKind.FIRST_PARAMETER_ERASED -> "(Ljava/lang/Object;${signature.substringAfter(';')}"
        }
    }

    // Change generic signatures of remapped Kotlin builtins for Java interoperability.
    //
    // There are a few Kotlin builtins which are mapped to methods with different generic signatures in Java.
    // For example, `MutableMap<K, V>` contains the following method.
    //
    //   remove(key: K): V
    //
    // This is mapped to a method with the same name in `java.util.Map` but with the following signature.
    //
    //   V remove(Object key)
    //
    // If we instantiate `K` with some type that erases to `Object` then the Java compiler will see the
    // `remove` method as an override of the corresponding method in `java.util.Map`. This leads to a check
    // that it is a valid override according to the Java language rules, i.e., that the parameter types
    // are the same and the return type of the override is a subtype of the return type of the declaration.
    //
    // This check will fail if we generate a generic signature for the Kotlin declaration, since `K` is not
    // the same type as `Object`. Similarly, we cannot just omit the generic signature, since otherwise the
    // check would fail as `Object` is not a subtype of `V`.
    //
    // Instead, we have to produce a generic signature matching what Java expects. This is not a problem for
    // Kotlin code, since we record the correct types in the Kotlin metadata.
    fun mapSignature(irFunction: IrSimpleFunction, genericSignature: JvmMethodGenericSignature): JvmMethodGenericSignature {
        val signature = genericSignature.genericsSignature
            ?: return genericSignature

        val method = genericSignature.asmMethod
        val processors = methodNameToProcessors[method.name]
            ?: return genericSignature

        for (processor in processors) {
            if (processor.match(irFunction, method)) {
                return JvmMethodGenericSignature(
                    method,
                    genericSignature.valueParameters,
                    processor.process(signature)
                )
            }
        }

        return genericSignature
    }

    private val methodNameToProcessors = mapOf(
        "contains" to listOf(
            // boolean contains(Object o)
            SignatureProcessor(
                StandardNames.FqNames.collection,
                "(Ljava/lang/Object;)Z",
                SignatureKind.NON_GENERIC
            )
        ),
        "containsAll" to listOf(
            // boolean containsAll(Collection<?> c)
            SignatureProcessor(
                StandardNames.FqNames.collection,
                "(Ljava/util/Collection;)Z",
                SignatureKind.FIXED_COLLECTION_PARAMETER
            ),
        ),
        "containsKey" to listOf(
            // boolean containsKey(Object key)
            SignatureProcessor(
                StandardNames.FqNames.map,
                "(Ljava/lang/Object;)Z",
                SignatureKind.NON_GENERIC
            ),
        ),
        "containsValue" to listOf(
            // boolean containsValue(Object value)
            SignatureProcessor(
                StandardNames.FqNames.map,
                "(Ljava/lang/Object;)Z",
                SignatureKind.NON_GENERIC
            ),
        ),
        "get" to listOf(
            // V get(Object key)
            // Match the first parameter and ensure that there is only a single parameter.
            // The return type is generic in both Kotlin and Java.
            SignatureProcessor(
                StandardNames.FqNames.map,
                "(Ljava/lang/Object;)",
                SignatureKind.FIRST_PARAMETER_ERASED
            )
        ),
        "getOrDefault" to listOf(
            // V getOrDefault(Object key, V defaultValue)
            // Match the first parameter and ensure that there are two parameters. This check
            // could be more precise, but it's unnecessary since we will check afterwards that
            // any matching function overrides a `getOrDefault` method in `Map`.
            // Note that this check is required anyway, since `getOrDefault` is platform specific
            // and only present when compiling against JDK 1.8+.
            SignatureProcessor(
                StandardNames.FqNames.map,
                "(Ljava/lang/Object;",
                SignatureKind.FIRST_PARAMETER_ERASED
            ) { method ->
                method.argumentTypes.size == 2
            },
        ),
        "indexOf" to listOf(
            // int indexOf(Object o)
            SignatureProcessor(
                StandardNames.FqNames.list,
                "(Ljava/lang/Object;)I",
                SignatureKind.NON_GENERIC
            ),
        ),
        "lastIndexOf" to listOf(
            // int lastIndexOf(Object o)
            SignatureProcessor(
                StandardNames.FqNames.list,
                "(Ljava/lang/Object;)I",
                SignatureKind.NON_GENERIC
            ),
        ),
        "remove" to listOf(
            // boolean remove(Object o)
            SignatureProcessor(
                StandardNames.FqNames.mutableCollection,
                "(Ljava/lang/Object;)Z",
                SignatureKind.NON_GENERIC
            ),
            // V remove(Object key)
            // Match the first parameter and ensure that there is only a single parameter.
            // The return type is generic in both Kotlin and Java.
            SignatureProcessor(
                StandardNames.FqNames.mutableMap,
                "(Ljava/lang/Object;)",
                SignatureKind.FIRST_PARAMETER_ERASED
            ),
            // boolean remove(Object key, Object value)
            // This declaration is platform specific and only present on JDK 1.8+.
            // We check that the function overrides `remove` in `MutableMap` to avoid
            // erasing generic signatures when compiling against older JDK versions.
            SignatureProcessor(
                StandardNames.FqNames.mutableMap,
                "(Ljava/lang/Object;Ljava/lang/Object;)Z",
                SignatureKind.NON_GENERIC
            ),
        ),
        "removeAll" to listOf(
            // boolean removeAll(Collection<?> c)
            SignatureProcessor(
                StandardNames.FqNames.mutableCollection,
                "(Ljava/util/Collection;)Z",
                SignatureKind.FIXED_COLLECTION_PARAMETER
            ),
        ),
        "retainAll" to listOf(
            // boolean retainAll(Collection<?> c)
            SignatureProcessor(
                StandardNames.FqNames.mutableCollection,
                "(Ljava/util/Collection;)Z",
                SignatureKind.FIXED_COLLECTION_PARAMETER
            ),
        ),
    )
}
