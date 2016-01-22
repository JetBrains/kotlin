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

import org.jetbrains.kotlin.codegen.context.FieldOwnerContext
import org.jetbrains.kotlin.codegen.intrinsics.TypeIntrinsics
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature.SpecialSignatureInfo
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

fun generateIsCheck(
        v: InstructionAdapter,
        kotlinType: KotlinType,
        asmType: Type
) {
    if (TypeUtils.isNullableType(kotlinType)) {
        val nope = Label()
        val end = Label()

        with(v) {
            dup()

            ifnull(nope)

            TypeIntrinsics.instanceOf(this, kotlinType, asmType)

            goTo(end)

            mark(nope)
            pop()
            iconst(1)

            mark(end)
        }
    }
    else {
        TypeIntrinsics.instanceOf(v, kotlinType, asmType)
    }
}

fun generateAsCast(
        v: InstructionAdapter,
        kotlinType: KotlinType,
        asmType: Type,
        isSafe: Boolean
) {
    if (!isSafe) {
        if (!TypeUtils.isNullableType(kotlinType)) {
            generateNullCheckForNonSafeAs(v, kotlinType)
        }
    }
    else {
        with(v) {
            dup()
            TypeIntrinsics.instanceOf(v, kotlinType, asmType)
            val ok = Label()
            ifne(ok)
            pop()
            aconst(null)
            mark(ok)
        }
    }

    TypeIntrinsics.checkcast(v, kotlinType, asmType, isSafe)
}

private fun generateNullCheckForNonSafeAs(
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

fun SpecialSignatureInfo.replaceValueParametersIn(sourceSignature: String?): String?
        = valueParametersSignature?.let { sourceSignature?.replace("^\\(.*\\)".toRegex(), "($it)") }

fun populateCompanionBackingFieldNamesToOuterContextIfNeeded(companion: KtObjectDeclaration, outerContext: FieldOwnerContext<*>, state: GenerationState) {
    val descriptor = state.bindingContext.get(BindingContext.CLASS, companion)

    if (descriptor == null || ErrorUtils.isError(descriptor)) {
        return
    }

    if (!JvmAbi.isCompanionObjectWithBackingFieldsInOuter(descriptor)) {
        return
    }
    val properties = companion.declarations.filterIsInstance<KtProperty>()

    properties.forEach {
        val variableDescriptor = state.bindingContext.get(BindingContext.VARIABLE, it)
        if (variableDescriptor is PropertyDescriptor) {
            outerContext.getFieldName(variableDescriptor, it.hasDelegate())
        }
    }

}