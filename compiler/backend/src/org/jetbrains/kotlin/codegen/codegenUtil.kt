/*
 * Copyright 2010-2015 JetBrains s.r.o.
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


package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature.SpecialSignatureInfo
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

fun generateIsCheck(
        v: InstructionAdapter,
        isNullable: Boolean,
        generateInstanceOfInstruction: (InstructionAdapter) -> Unit
) {
    if (isNullable) {
        val nope = Label()
        val end = Label()

        with(v) {
            dup()

            ifnull(nope)

            generateInstanceOfInstruction(this)
            goTo(end)

            mark(nope)
            pop()
            iconst(1)

            mark(end)
        }
    }
    else {
        generateInstanceOfInstruction(v)
    }
}

fun generateNullCheckForNonSafeAs(
        v: InstructionAdapter,
        type: KotlinType
) {
    with(v) {
        dup()
        val nonnull = Label()
        ifnonnull(nonnull)
        AsmUtil.genThrow(v, "kotlin/TypeCastException", "null cannot be cast to non-null type " + DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(type))
        mark(nonnull)
    }
}

public fun SpecialSignatureInfo.replaceValueParametersIn(sourceSignature: String?): String?
        = valueParametersSignature?.let { sourceSignature?.replace("^\\(.*\\)".toRegex(), "($it)") }

