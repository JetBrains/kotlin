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

package org.jetbrains.kotlin.serialization.builtins

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.constants.NullValue
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.serialization.*

public class BuiltInsSerializerExtension : SerializerExtension() {
    private val stringTable = StringTableImpl()
    private val annotationSerializer = AnnotationSerializer(stringTable)

    override fun getStringTable(): StringTable = stringTable

    override fun serializeClass(descriptor: ClassDescriptor, proto: ProtoBuf.Class.Builder) {
        for (annotation in descriptor.annotations) {
            proto.addExtension(BuiltInsProtoBuf.classAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
    }

    override fun serializePackage(packageFragments: Collection<PackageFragmentDescriptor>, proto: ProtoBuf.Package.Builder) {
        val classes = packageFragments.flatMap {
            it.getMemberScope().getDescriptors(DescriptorKindFilter.CLASSIFIERS).filterIsInstance<ClassDescriptor>()
        }

        for (descriptor in DescriptorSerializer.sort(classes)) {
            proto.addExtension(BuiltInsProtoBuf.className, stringTable.getSimpleNameIndex(descriptor.name))
        }
    }

    override fun serializeCallable(callable: CallableMemberDescriptor, proto: ProtoBuf.Callable.Builder) {
        for (annotation in callable.annotations) {
            proto.addExtension(BuiltInsProtoBuf.callableAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
        val propertyDescriptor = callable as? PropertyDescriptor ?: return
        val compileTimeConstant = propertyDescriptor.compileTimeInitializer
        if (compileTimeConstant != null && compileTimeConstant !is NullValue) {
            val valueProto = annotationSerializer.valueProto(compileTimeConstant)
            proto.setExtension(BuiltInsProtoBuf.compileTimeValue, valueProto.build())
        }
    }

    override fun serializeValueParameter(descriptor: ValueParameterDescriptor, proto: ProtoBuf.Callable.ValueParameter.Builder) {
        for (annotation in descriptor.annotations) {
            proto.addExtension(BuiltInsProtoBuf.parameterAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
    }
}
