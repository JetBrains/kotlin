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

class GlobalVariableStub(global: GlobalDecl, stubGenerator: StubGenerator) : KotlinStub, NativeBacked {

    val getAddressExpression: KotlinExpression
    val header: String
    val getter: KotlinExpression
    val setter: KotlinExpression?

    init {
        getAddressExpression = stubGenerator.simpleBridgeGenerator.kotlinToNative(
                nativeBacked = this,
                returnType = BridgedType.NATIVE_PTR,
                kotlinValues = emptyList()
        ) {
            "&${global.name}"
        }

        val kotlinScope = stubGenerator.kotlinFile

        // TODO: consider sharing the logic below with field generator.

        val kotlinType: KotlinType

        val mirror = mirror(stubGenerator.declarationMapper, global.type)
        val unwrappedType = global.type.unwrapTypedefs()

        if (unwrappedType is ArrayType) {
            kotlinType = (mirror as TypeMirror.ByValue).valueType
            getter = mirror.info.argFromBridged(getAddressExpression, kotlinScope) + "!!"
            setter = null
        } else {
            val pointedTypeName = mirror.pointedType.render(kotlinScope)
            val storagePointed = "interpretPointed<$pointedTypeName>($getAddressExpression)"
            if (mirror is TypeMirror.ByValue) {
                kotlinType = mirror.argType
                val valueProperty = "$storagePointed.value"
                getter = valueProperty
                setter = if (global.isConst) null else "$valueProperty = value"
            } else {
                kotlinType = mirror.pointedType
                getter = storagePointed
                setter = null
            }
        }

        header = buildString {
            append(if (setter != null) "var" else "val")
            append(" ")
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
            lines.add(header)
            lines.add("    get() = $getter")
            if (setter != null) {
                lines.add("    set(value) { $setter }")
            }
        } else {
            lines.add(annotationForUnableToImport)
            lines.add(header)
            lines.add("    get() = TODO()")
            if (setter != null) {
                lines.add("    set(value) = TODO()")
            }
        }

        return lines.asSequence()
    }

}