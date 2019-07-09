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

package org.jetbrains.kotlin.backend.common.descriptors

import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.builtins.getFunctionalClassKind
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.replace

val String.synthesizedName: Name get() = Name.identifier(this.synthesizedString)

val String.synthesizedString: String get() = "\$$this"

val DeclarationDescriptor.propertyIfAccessor: DeclarationDescriptor
    get() = if (this is PropertyAccessorDescriptor)
        this.correspondingProperty
    else this

val CallableDescriptor.isSuspend: Boolean
    get() = this is FunctionDescriptor && isSuspend

/**
 * @return naturally-ordered list of all parameters available inside the function body.
 */
// Used in Kotlin/Native
@Suppress("unused")
val CallableDescriptor.allParameters: List<ParameterDescriptor>
    get() = if (this is ConstructorDescriptor) {
        listOf(this.constructedClass.thisAsReceiverParameter) + explicitParameters
    } else {
        explicitParameters
    }

/**
 * @return naturally-ordered list of the parameters that can have values specified at call site.
 */
val CallableDescriptor.explicitParameters: List<ParameterDescriptor>
    get() {
        val result = ArrayList<ParameterDescriptor>(valueParameters.size + 2)

        this.dispatchReceiverParameter?.let {
            result.add(it)
        }

        this.extensionReceiverParameter?.let {
            result.add(it)
        }

        result.addAll(valueParameters)

        return result
    }

// Used in Kotlin/Native
@Suppress("unused")
fun FunctionDescriptor.substitute(vararg types: KotlinType): FunctionDescriptor {
    val typeSubstitutor = TypeSubstitutor.create(
        typeParameters
            .withIndex()
            .associateBy({ it.value.typeConstructor }, { TypeProjectionImpl(types[it.index]) })
    )
    return substitute(typeSubstitutor)!!
}

val KotlinType.isFunctionOrKFunctionType: Boolean
    get() {
        val kind = constructor.declarationDescriptor?.getFunctionalClassKind()
        return kind == FunctionClassDescriptor.Kind.Function || kind == FunctionClassDescriptor.Kind.KFunction
    }
