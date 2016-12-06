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

package org.jetbrains.kotlin.serialization.deserialization.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.deserialization.TypeTable

interface DeserializedCallableMemberDescriptor : CallableMemberDescriptor {
    val proto: MessageLite

    val nameResolver: NameResolver

    val typeTable: TypeTable

    // Information about the origin of this callable's container (class or package part on JVM) or null if there's no such information.
    val containerSource: DeserializedContainerSource?
}

interface DeserializedContainerSource : SourceElement {
    // Non-null if this container is loaded from a class with an incompatible binary version
    val incompatibility: IncompatibleVersionErrorData<*>?

    // True iff this is container is "invisible" because it's loaded from a pre-release class and this compiler is a release
    val isPreReleaseInvisible: Boolean

    // This FQ name should only be used for error messages
    val presentableFqName: FqName
}

class DeserializedSimpleFunctionDescriptor(
        containingDeclaration: DeclarationDescriptor,
        original: SimpleFunctionDescriptor?,
        annotations: Annotations,
        name: Name,
        kind: CallableMemberDescriptor.Kind,
        override val proto: ProtoBuf.Function,
        override val nameResolver: NameResolver,
        override val typeTable: TypeTable,
        override val containerSource: DeserializedContainerSource?
) : DeserializedCallableMemberDescriptor,
        SimpleFunctionDescriptorImpl(containingDeclaration, original, annotations, name, kind, SourceElement.NO_SOURCE) {

    override fun createSubstitutedCopy(
            newOwner: DeclarationDescriptor,
            original: FunctionDescriptor?,
            kind: CallableMemberDescriptor.Kind,
            newName: Name?,
            preserveSource: Boolean
    ): FunctionDescriptorImpl {
        return DeserializedSimpleFunctionDescriptor(
                newOwner, original as SimpleFunctionDescriptor?, annotations, newName ?: name, kind,
                proto, nameResolver, typeTable, containerSource
        )
    }
}

class DeserializedPropertyDescriptor(
        containingDeclaration: DeclarationDescriptor,
        original: PropertyDescriptor?,
        annotations: Annotations,
        modality: Modality,
        visibility: Visibility,
        isVar: Boolean,
        name: Name,
        kind: CallableMemberDescriptor.Kind,
        isLateInit: Boolean,
        isConst: Boolean,
        override val proto: ProtoBuf.Property,
        override val nameResolver: NameResolver,
        override val typeTable: TypeTable,
        override val containerSource: DeserializedContainerSource?
) : DeserializedCallableMemberDescriptor,
        PropertyDescriptorImpl(containingDeclaration, original, annotations,
                               modality, visibility, isVar, name, kind, SourceElement.NO_SOURCE, isLateInit, isConst) {

    override fun createSubstitutedCopy(
            newOwner: DeclarationDescriptor,
            newModality: Modality,
            newVisibility: Visibility,
            original: PropertyDescriptor?,
            kind: CallableMemberDescriptor.Kind
    ): PropertyDescriptorImpl {
        return DeserializedPropertyDescriptor(
                newOwner, original, annotations, newModality, newVisibility, isVar, name, kind, isLateInit, isConst,
                proto, nameResolver, typeTable, containerSource
        )
    }
}

class DeserializedConstructorDescriptor(
        containingDeclaration: ClassDescriptor,
        original: ConstructorDescriptor?,
        annotations: Annotations,
        isPrimary: Boolean,
        kind: CallableMemberDescriptor.Kind,
        override val proto: ProtoBuf.Constructor,
        override val nameResolver: NameResolver,
        override val typeTable: TypeTable,
        override val containerSource: DeserializedContainerSource?
) : DeserializedCallableMemberDescriptor,
        ConstructorDescriptorImpl(containingDeclaration, original, annotations, isPrimary, kind, SourceElement.NO_SOURCE) {

    override fun createSubstitutedCopy(
            newOwner: DeclarationDescriptor,
            original: FunctionDescriptor?,
            kind: CallableMemberDescriptor.Kind,
            newName: Name?,
            preserveSource: Boolean
    ): DeserializedConstructorDescriptor {
        return DeserializedConstructorDescriptor(
                newOwner as ClassDescriptor, original as ConstructorDescriptor?, annotations, isPrimary, kind,
                proto, nameResolver, typeTable, containerSource
        )
    }

    override fun isExternal(): Boolean = false

    override fun isInline(): Boolean = false

    override fun isTailrec(): Boolean = false
}
