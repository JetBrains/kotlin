/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.gen.jvm.KotlinPlatform
import org.jetbrains.kotlin.native.interop.indexer.NativeLibrary
import org.jetbrains.kotlin.native.interop.indexer.mapFragmentIsCompilable

class SimpleBridgeGeneratorImpl(
        private val platform: KotlinPlatform,
        private val pkgName: String,
        private val jvmFileClassName: String,
        private val libraryForCStubs: NativeLibrary
) : SimpleBridgeGenerator {

    private var nextUniqueId = 0

    private val BridgedType.nativeType: String get() = when (platform) {
        KotlinPlatform.JVM -> when (this) {
            BridgedType.BYTE -> "jbyte"
            BridgedType.SHORT -> "jshort"
            BridgedType.INT -> "jint"
            BridgedType.LONG -> "jlong"
            BridgedType.FLOAT -> "jfloat"
            BridgedType.DOUBLE -> "jdouble"
            BridgedType.NATIVE_PTR -> "jlong"
            BridgedType.OBJC_POINTER -> TODO()
            BridgedType.VOID -> "void"
        }
        KotlinPlatform.NATIVE -> when (this) {
            BridgedType.BYTE -> "int8_t"
            BridgedType.SHORT -> "int16_t"
            BridgedType.INT -> "int32_t"
            BridgedType.LONG -> "int64_t"
            BridgedType.FLOAT -> "float"
            BridgedType.DOUBLE -> "double"
            BridgedType.NATIVE_PTR -> "void*"
            BridgedType.OBJC_POINTER -> "id"
            BridgedType.VOID -> "void"
        }
    }

    private inner class NativeBridge(val kotlinLines: List<String>, val nativeLines: List<String>)

    override fun kotlinToNative(
            nativeBacked: NativeBacked,
            returnType: BridgedType,
            kotlinValues: List<BridgeTypedKotlinValue>,
            block: NativeCodeBuilder.(arguments: List<NativeExpression>) -> NativeExpression
    ): KotlinExpression {

        val kotlinLines = mutableListOf<String>()
        val nativeLines = mutableListOf<String>()

        val kotlinFunctionName = "kniBridge${nextUniqueId++}"
        val kotlinParameters = kotlinValues.withIndex().joinToString {
            "p${it.index}: ${it.value.type.kotlinType}"
        }

        val callExpr = "$kotlinFunctionName(${kotlinValues.joinToString { it.value }})"

        val cFunctionParameters = when (platform) {
            KotlinPlatform.JVM -> mutableListOf(
                    "jniEnv" to "JNIEnv*",
                    "jclss" to "jclass"
            )
            KotlinPlatform.NATIVE -> mutableListOf()
        }

        kotlinValues.withIndex().mapTo(cFunctionParameters) {
            "p${it.index}" to it.value.type.nativeType
        }

        val joinedCParameters = cFunctionParameters.joinToString { (name, type) -> "$type $name" }
        val cReturnType = returnType.nativeType

        val cFunctionHeader = when (platform) {
            KotlinPlatform.JVM -> {
                val funcFullName = buildString {
                    if (pkgName.isNotEmpty()) {
                        append(pkgName)
                        append('.')
                    }
                    append(jvmFileClassName)
                    append('.')
                    append(kotlinFunctionName)
                }

                val functionName = "Java_" + funcFullName.replace("_", "_1").replace('.', '_').replace("$", "_00024")
                "JNIEXPORT $cReturnType JNICALL $functionName ($joinedCParameters)"
            }
            KotlinPlatform.NATIVE -> {
                val functionName = pkgName.replace('.', '_') + "_$kotlinFunctionName"
                kotlinLines.add("@SymbolName(${functionName.quoteAsKotlinLiteral()})")
                "$cReturnType $functionName ($joinedCParameters)"
            }
        }
        nativeLines.add(cFunctionHeader + " {")

        buildNativeCodeLines {
            val cExpr = block(cFunctionParameters.takeLast(kotlinValues.size).map { (name, _) -> name })
            if (returnType != BridgedType.VOID) {
                out("return ($cReturnType)$cExpr;")
            }
        }.forEach {
            nativeLines.add("    $it")
        }

        nativeLines.add("}")
        kotlinLines.add("private external fun $kotlinFunctionName($kotlinParameters): ${returnType.kotlinType}")

        val nativeBridge = NativeBridge(kotlinLines, nativeLines)
        nativeBridges.add(nativeBacked to nativeBridge)

        return callExpr
    }

    override fun nativeToKotlin(
            nativeBacked: NativeBacked,
            returnType: BridgedType,
            nativeValues: List<BridgeTypedNativeValue>,
            block: KotlinCodeBuilder.(arguments: List<KotlinExpression>) -> KotlinExpression
    ): NativeExpression {

        if (platform != KotlinPlatform.NATIVE) TODO()

        val kotlinLines = mutableListOf<String>()
        val nativeLines = mutableListOf<String>()

        val kotlinFunctionName = "kniBridge${nextUniqueId++}"
        val kotlinParameters = nativeValues.withIndex().map {
            "p${it.index}" to it.value.type.kotlinType
        }
        val joinedKotlinParameters = kotlinParameters.joinToString { "${it.first}: ${it.second}" }

        val cFunctionParameters = nativeValues.withIndex().map {
            "p${it.index}" to it.value.type.nativeType
        }
        val joinedCParameters = cFunctionParameters.joinToString { (name, type) -> "$type $name" }
        val cReturnType = returnType.nativeType

        val symbolName = pkgName.replace('.', '_') + "_$kotlinFunctionName"
        kotlinLines.add("@konan.internal.ExportForCppRuntime(${symbolName.quoteAsKotlinLiteral()})")
        val cFunctionHeader = "$cReturnType $symbolName($joinedCParameters)"

        nativeLines.add("$cFunctionHeader;")
        kotlinLines.add("private fun $kotlinFunctionName($joinedKotlinParameters): ${returnType.kotlinType} {")

        buildKotlinCodeLines {
            var kotlinExpr = block(kotlinParameters.map { (name, _) -> name })
            if (returnType == BridgedType.OBJC_POINTER) {
                // The Kotlin code may lose the ownership on this pointer after returning from the bridge,
                // so retain the pointer and autorelease it:
                kotlinExpr = "objc_retainAutoreleaseReturnValue($kotlinExpr)"
                // (Objective-C does the same for returned pointers).
            }
            out("return $kotlinExpr")
        }.forEach {
            kotlinLines.add("    $it")
        }

        kotlinLines.add("}")

        insertNativeBridge(nativeBacked, kotlinLines, nativeLines)

        return "$symbolName(${nativeValues.joinToString { it.value }})"

    }

    override fun insertNativeBridge(nativeBacked: NativeBacked, kotlinLines: List<String>, nativeLines: List<String>) {
        val nativeBridge = NativeBridge(kotlinLines, nativeLines)
        nativeBridges.add(nativeBacked to nativeBridge)
    }

    private val nativeBridges = mutableListOf<Pair<NativeBacked, NativeBridge>>()

    override fun prepare(): NativeBridges {
        val includedBridges = mutableListOf<NativeBridge>()
        val excludedClients = mutableSetOf<NativeBacked>()

        nativeBridges.map { it.second.nativeLines }
                .mapFragmentIsCompilable(libraryForCStubs)
                .forEachIndexed { index, isCompilable ->
                    if (isCompilable) {
                        includedBridges.add(nativeBridges[index].second)
                    } else {
                        excludedClients.add(nativeBridges[index].first)
                    }
                }

        // TODO: exclude unused bridges.

        return object : NativeBridges {

            override val kotlinLines: Sequence<String>
                get() = includedBridges.asSequence().flatMap { it.kotlinLines.asSequence() }

            override val nativeLines: Sequence<String>
                get() = includedBridges.asSequence().flatMap { it.nativeLines.asSequence() }

            override fun isSupported(nativeBacked: NativeBacked): Boolean =
                    nativeBacked !in excludedClients
        }
    }
}