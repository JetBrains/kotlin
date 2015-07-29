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

package org.jetbrains.kotlin.load.java.typeEnhacement

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.types.JetType

public fun <D : CallableMemberDescriptor> enhanceSignatures(platformSignatures: Collection<D>): Collection<D> {
    return platformSignatures.map {
        it.enhanceSignature()
    }
}

public fun <D : CallableMemberDescriptor> D.enhanceSignature(): D {
    // TODO type parameters
    // TODO use new type parameters while enhancing other types
    // TODO Propagation into generic type arguments

    val enhancedReceiverType =
            if (getExtensionReceiverParameter() != null)
                parts(isCovariant = false) { it.getExtensionReceiverParameter()!!.getType() }.enhance()
            else null

    val enhancedValueParametersTypes = getValueParameters().map {
        p -> parts(isCovariant = false) { it.getValueParameters()[p.getIndex()].getType() }.enhance()
    }

    val enhancedReturnType = parts(isCovariant = true) { it.getReturnType()!! }.enhance()

    if (this is JavaCallableMemberDescriptor) {
        @suppress("UNCHECKED_CAST")
        return this.enhance(enhancedReceiverType, enhancedValueParametersTypes, enhancedReturnType) as D
    }

    return this
}

private class SignatureParts(
    val fromOverride: JetType,
    val fromOverridden: Collection<JetType>,
    val isCovariant: Boolean
) {
    fun enhance(): JetType {
        val qualifiers = fromOverride.computeIndexedQualifiersForOverride(this.fromOverridden, isCovariant)
        return fromOverride.enhance(qualifiers)
    }
}

private fun <D : CallableMemberDescriptor> D.parts(isCovariant: Boolean, collector: (D) -> JetType): SignatureParts {
    return SignatureParts(
            collector(this),
            this.getOverriddenDescriptors().map {
                @suppress("UNCHECKED_CAST")
                collector(it as D)
            },
            isCovariant
    )
}