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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.descriptors.*
import java.util.ArrayList

/**
 * Given a function descriptor, creates another function descriptor with type parameters copied from outer context(s).
 * This is needed because once we're serializing this to a proto, there's no place to store information about external type parameters.
 */
fun createFreeFakeLambdaDescriptor(descriptor: FunctionDescriptor): FunctionDescriptor {
    val builder = descriptor.newCopyBuilder()
    val typeParameters = ArrayList<TypeParameterDescriptor>(0)
    builder.setTypeParameters(typeParameters)

    var container: DeclarationDescriptor? = descriptor.containingDeclaration
    while (container != null) {
        if (container is ClassDescriptor) {
            typeParameters.addAll(container.declaredTypeParameters)
        }
        else if (container is CallableDescriptor && container !is ConstructorDescriptor) {
            typeParameters.addAll(container.typeParameters)
        }
        container = container.containingDeclaration
    }

    return if (typeParameters.isEmpty()) descriptor else builder.build()!!
}
