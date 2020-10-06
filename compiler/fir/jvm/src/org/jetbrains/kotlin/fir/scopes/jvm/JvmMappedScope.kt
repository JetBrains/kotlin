/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.jvm

import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsSignatures
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.Name

class JvmMappedScope(
    private val declaredMemberScope: FirScope,
    private val javaMappedClassUseSiteScope: FirScope,
    private val signatures: Signatures
) : FirTypeScope() {

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> Unit) {
        val whiteListSignatures = signatures.whiteListSignaturesByName[name]
            ?: return declaredMemberScope.processFunctionsByName(name, processor)
        javaMappedClassUseSiteScope.processFunctionsByName(name) { symbol ->
            val jvmSignature = symbol.fir.computeJvmDescriptor()
                .replace("kotlin/Any", "java/lang/Object")
                .replace("kotlin/String", "java/lang/String")
                .replace("kotlin/Throwable", "java/lang/Throwable")
            if (jvmSignature in whiteListSignatures) {
                processor(symbol)
            }
        }

        declaredMemberScope.processFunctionsByName(name, processor)
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: FirFunctionSymbol<*>,
        processor: (FirFunctionSymbol<*>, FirTypeScope) -> ProcessorAction
    ) = ProcessorAction.NONE

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        val constructorBlackList = signatures.constructorBlackList
        if (constructorBlackList.isNotEmpty()) {
            javaMappedClassUseSiteScope.processDeclaredConstructors { symbol ->
                val jvmSignature = symbol.fir.computeJvmDescriptor()
                    .replace("kotlin/Any", "java/lang/Object")
                    .replace("kotlin/String", "java/lang/String")
                    .replace("kotlin/Throwable", "java/lang/Throwable")
                if (jvmSignature !in constructorBlackList) {
                    processor(symbol)
                }
            }
        } else {
            javaMappedClassUseSiteScope.processDeclaredConstructors(processor)
        }

        declaredMemberScope.processDeclaredConstructors(processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        declaredMemberScope.processPropertiesByName(name, processor)
    }

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction = ProcessorAction.NONE

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        declaredMemberScope.processClassifiersByNameWithSubstitution(name, processor)
    }

    override fun getCallableNames(): Set<Name> {
        return declaredMemberScope.getContainingCallableNamesIfPresent()
    }

    override fun getClassifierNames(): Set<Name> {
        return declaredMemberScope.getContainingClassifierNamesIfPresent()
    }

    companion object {
        data class Signatures(val whiteListSignaturesByName: Map<Name, Set<String>>, val constructorBlackList: Set<String>) {
            fun isEmpty() = whiteListSignaturesByName.isEmpty() && constructorBlackList.isEmpty()
            fun isNotEmpty() = !isEmpty()
        }

        // NOTE: No-arg constructors
        @OptIn(ExperimentalStdlibApi::class)
        private val additionalConstructorBlackList = buildSet<String> {
            // kotlin.text.String pseudo-constructors should be used instead of java.lang.String constructors
            listOf(
                "",
                "Lkotlin/ByteArray;IILjava/nio/charset/Charset;",
                "Lkotlin/ByteArray;Ljava/nio/charset/Charset;",
                "Lkotlin/ByteArray;II",
                "Lkotlin/ByteArray;",
                "Lkotlin/CharArray;",
                "Lkotlin/CharArray;II",
                "Lkotlin/IntArray;II",
                "Ljava/lang/StringBuffer;",
                "Ljava/lang/StringBuilder;",
            ).mapTo(this) { arguments -> "java/lang/String.<init>($arguments)V" }

            listOf(
                "",
                "Ljava/lang/String;Ljava/lang/Throwable;",
                "Ljava/lang/Throwable;",
                "Ljava/lang/String;"
            ).mapTo(this) { arguments -> "java/lang/Throwable.<init>($arguments)V" }
        }

        fun prepareSignatures(klass: FirRegularClass): Signatures {

            val signaturePrefix = klass.symbol.classId.toString()
            val whiteListSignaturesByName = mutableMapOf<Name, MutableSet<String>>()
            JvmBuiltInsSignatures.WHITE_LIST_METHOD_SIGNATURES.filter { signature ->
                signature.startsWith(signaturePrefix)
            }.map { signature ->
                // +1 to delete dot before function name
                signature.substring(signaturePrefix.length + 1)
            }.forEach {
                whiteListSignaturesByName.getOrPut(Name.identifier(it.substringBefore("("))) { mutableSetOf() }.add(it)
            }

            val constructorBlackList =
                (JvmBuiltInsSignatures.BLACK_LIST_CONSTRUCTOR_SIGNATURES + additionalConstructorBlackList)
                    .filter { it.startsWith(signaturePrefix) }
                    .mapTo(mutableSetOf()) { it.substring(signaturePrefix.length + 1) }

            return Signatures(whiteListSignaturesByName, constructorBlackList)
        }
    }
}
