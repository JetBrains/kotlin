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

import org.jetbrains.kotlin.native.interop.gen.jvm.StubGenerator
import org.jetbrains.kotlin.native.interop.indexer.ArrayType
import org.jetbrains.kotlin.native.interop.indexer.GlobalDecl
import org.jetbrains.kotlin.native.interop.indexer.unwrapTypedefs

class GlobalVariableStub(global: GlobalDecl, stubGenerator: StubGenerator) : KotlinStub, NativeBacked {

    private val getAddressExpression: KotlinExpression by lazy {
        stubGenerator.simpleBridgeGenerator.kotlinToNative(
                nativeBacked = this,
                returnType = BridgedType.NATIVE_PTR,
                kotlinValues = emptyList()
        ) {
            "&${global.name}"
        }
    }

    private val setterStub = object : NativeBacked {}

    val header: String
    val getter: KotlinExpression
    val setter: KotlinExpression?

    init {
        val kotlinScope = stubGenerator.kotlinFile

        // TODO: consider sharing the logic below with field generator.

        val kotlinType: KotlinType

        val mirror = mirror(stubGenerator.declarationMapper, global.type)
        val unwrappedType = global.type.unwrapTypedefs()

        if (unwrappedType is ArrayType) {
            kotlinType = (mirror as TypeMirror.ByValue).valueType
            getter = mirror.info.argFromBridged(getAddressExpression, kotlinScope, nativeBacked = this) + "!!"
            setter = null
        } else {
            if (mirror is TypeMirror.ByValue) {
                getter = mirror.info.argFromBridged(stubGenerator.simpleBridgeGenerator.kotlinToNative(
                        nativeBacked = this,
                        returnType = mirror.info.bridgedType,
                        kotlinValues = emptyList()
                ) {
                    mirror.info.cToBridged(expr = global.name)
                }, kotlinScope, nativeBacked = this)

                setter = if (global.isConst) {
                    null
                } else {
                    val bridgedValue = BridgeTypedKotlinValue(mirror.info.bridgedType, mirror.info.argToBridged("value"))

                    stubGenerator.simpleBridgeGenerator.kotlinToNative(
                            nativeBacked = setterStub,
                            returnType = BridgedType.VOID,
                            kotlinValues = listOf(bridgedValue)
                    ) { nativeValues ->
                        out("${global.name} = ${mirror.info.cFromBridged(
                                nativeValues.single(),
                                scope,
                                nativeBacked = setterStub
                        )};")
                        ""
                    }
                }

                kotlinType = mirror.argType
            } else {
                val pointedTypeName = mirror.pointedType.render(kotlinScope)
                val storagePointed = "interpretPointed<$pointedTypeName>($getAddressExpression)"
                kotlinType = mirror.pointedType
                getter = storagePointed
                setter = null
            }
        }

        header = buildString {
            append(getDeclarationName(kotlinScope, global.name))
            append(": ")
            append(kotlinType.render(kotlinScope))
        }
    }

    // Try to use the provided name. If failed, mangle it with underscore and try again:
    private tailrec fun getDeclarationName(scope: KotlinScope, name: String): String =
            scope.declareProperty(name) ?: getDeclarationName(scope, name + "_")

    override fun generate(context: StubGenerationContext): Sequence<String> {
        val lines = mutableListOf<String>()
        if (context.nativeBridges.isSupported(this)) {
            val mutable = setter != null && context.nativeBridges.isSupported(setterStub)
            val kind = if (mutable) "var" else "val"
            lines.add("$kind $header")
            lines.add("    get() = $getter")
            if (mutable) {
                lines.add("    set(value) { $setter }")
            }
        } else {
            lines.add(annotationForUnableToImport)
            lines.add("val $header")
            lines.add("    get() = TODO()")
        }

        return lines.asSequence()
    }

}