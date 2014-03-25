/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.types.error

import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.types.TypeConstructorImpl
import org.jetbrains.jet.lang.descriptors.annotations.Annotations
import java.util.ArrayList
import org.jetbrains.jet.lang.types.TypeConstructor
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.types.TypeSubstitutor
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.jet.lang.descriptors.impl.ConstructorDescriptorImpl
import java.util.Collections
import org.jetbrains.jet.lang.descriptors.Visibilities
import org.jetbrains.jet.lang.descriptors.Modality
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor
import org.jetbrains.jet.lang.types.ErrorUtils.*
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.types.TypeProjection

// marker for DescriptorRenderer to treat specially in decompiler mode
public trait MissingDependencyErrorClass {
    val fullFqName: FqName
}

public class MissingDependencyErrorClassDescriptor(override val fullFqName: FqName)
: MissingDependencyErrorClass, ClassDescriptorImpl(getErrorModule(), fullFqName.shortName(), Modality.OPEN, Collections.emptyList<JetType>()) {

    override fun substitute(substitutor: TypeSubstitutor): ClassDescriptor {
        return this
    }

    override fun toString(): String {
        return getName().asString()
    }

    override fun getMemberScope(typeArguments: List<TypeProjection?>): JetScope {
        return createErrorScope("Error scope for class " + getName() + " with arguments: " + typeArguments)
    }

    {
        val emptyConstructor = ConstructorDescriptorImpl.create(this, Annotations.EMPTY, true)
        emptyConstructor.initialize(Collections.emptyList<TypeParameterDescriptor>(), Collections.emptyList<ValueParameterDescriptor>(), Visibilities.INTERNAL, false)
        emptyConstructor.setReturnType(createErrorType("<ERROR RETURN TYPE>"))
        initialize(JetScope.EMPTY, Collections.singleton<ConstructorDescriptor>(emptyConstructor), emptyConstructor)
    }
}
