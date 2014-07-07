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

package org.jetbrains.jet.descriptors.serialization.descriptors

import org.jetbrains.jet.lang.descriptors.PropertyDescriptor
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.descriptors.Modality
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.descriptors.annotations.Annotations
import org.jetbrains.jet.lang.descriptors.Visibility
import org.jetbrains.jet.descriptors.serialization.NameResolver
import org.jetbrains.jet.lang.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.jet.descriptors.serialization.ProtoBuf
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor.Kind
import org.jetbrains.jet.lang.descriptors.SourceElement

public class DeserializedPropertyDescriptor(
        containingDeclaration: DeclarationDescriptor,
        original: PropertyDescriptor?,
        annotations: Annotations,
        modality: Modality,
        visibility: Visibility,
        isVar: Boolean,
        name: Name,
        kind: Kind,
        override public val proto: ProtoBuf.Callable,
        override public val nameResolver: NameResolver
) : DeserializedCallableMemberDescriptor,
    PropertyDescriptorImpl(containingDeclaration, original, annotations, modality, visibility, isVar, name, kind, SourceElement.NO_SOURCE) {

    override fun createSubstitutedCopy(
            newOwner: DeclarationDescriptor,
            newModality: Modality,
            newVisibility: Visibility,
            original: PropertyDescriptor?,
            kind: Kind
    ): PropertyDescriptorImpl {
        return DeserializedPropertyDescriptor(
                newOwner, original, getAnnotations(), newModality, newVisibility, isVar(), getName(), kind, proto, nameResolver)
    }
}