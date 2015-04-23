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

package org.jetbrains.kotlin.load.java.components

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.types.JetType

fun <D : CallableMemberDescriptor> enhanceSignatures(platformSignatures: Collection<D>): Collection<D> {
    return platformSignatures.map {
        it.enhance()
    }
}

fun <D : CallableMemberDescriptor> D.enhance(): D {
    // TODO type parameters
    // TODO use new type parameters while enhancing other types
    // TODO Propagation into generic type arguments

    val enhancedReceiverType =
            if (getExtensionReceiverParameter() != null)
                parts { it.getExtensionReceiverParameter()!!.toPart() }.enhance()
            else null

    val enhancedValueParameters = getValueParameters().map {
        p -> parts { it.getValueParameters()[p.getIndex()]!!.toPart() }.enhance()
    }

    val enhancedReturnType = parts { it.getReturnType()!!.toReturnTypePart() }.enhance()

    if (this is JavaMethodDescriptor) {
        val enhancedFunction = JavaMethodDescriptor.createJavaMethod(
                getContainingDeclaration()!!,
                getAnnotations(),
                getName(),
                getSource()
        )
        enhancedFunction.initialize(
                enhancedReceiverType,
                getDispatchReceiverParameter(),
                getTypeParameters(),
                enhancedValueParameters,
                enhancedReturnType,
                getModality(),
                getVisibility()
        )
        enhancedFunction.setHasStableParameterNames(hasStableParameterNames())
        enhancedFunction.setHasSynthesizedParameterNames(hasSynthesizedParameterNames())

        for (overridden in getOverriddenDescriptors()) {
            enhancedFunction.addOverriddenDescriptor(overridden)
        }

        @suppress("UNCHECKED_CAST")
        return enhancedFunction as D
    }

    return this
}

fun <T, P : SignaturePart<T>> SignatureParts<T, P>.enhance(): T {
    val qualifiers = fromOverride.type.computeIndexedQualifiersForOverride(this.fromOverridden.map { it.type }, fromOverride.isCovariant)
    return fromOverride.replaceType(fromOverride.type.enhance(qualifiers))
}

class SignatureParts<T, P: SignaturePart<T>>(
        val fromOverride: P,
        val fromOverridden: Collection<P>
)

interface SignaturePart<out T> {
    val isCovariant: Boolean
        get() = false

    val type: JetType

    fun replaceType(newType: JetType): T
}

fun ReceiverParameterDescriptor.toPart() = object : SignaturePart<JetType> {
    override val type = this@toPart.getType() // workaround for KT-7557

    override fun replaceType(newType: JetType) = newType
}

fun ValueParameterDescriptor.toPart() = object : SignaturePart<ValueParameterDescriptor> {
    override val type = this@toPart.getType() // workaround for KT-7557

    override fun replaceType(newType: JetType) = ValueParameterDescriptorImpl(
            getContainingDeclaration(),
            null,
            getIndex(),
            getAnnotations(),
            getName(),
            newType,
            declaresDefaultValue(),
            if (getVarargElementType() != null) KotlinBuiltIns.getInstance().getArrayElementType(newType) else null,
            getSource()
    )
}

fun JetType.toReturnTypePart() = object : SignaturePart<JetType> {
    override val type = this@toReturnTypePart

    override val isCovariant: Boolean = true

    override fun replaceType(newType: JetType) = newType
}

fun <D : CallableMemberDescriptor, T, P : SignaturePart<T>> D.parts(collector: (D) -> P): SignatureParts<T, P> {
    return SignatureParts(
            collector(this),
            this.getOverriddenDescriptors().map {
                @suppress("UNCHECKED_CAST")
                collector(it as D)
            }
    )
}