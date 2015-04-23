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

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.load.java.structure.reflect.classId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeUtils

object RuntimeTypeMapper {
    // TODO: this logic must be shared with JetTypeMapper
    fun mapTypeToJvmDesc(type: JetType): String {
        val classifier = type.getConstructor().getDeclarationDescriptor()
        if (classifier is TypeParameterDescriptor) {
            return mapTypeToJvmDesc(classifier.getUpperBounds().first())
        }

        if (KotlinBuiltIns.isArray(type)) {
            val elementType = KotlinBuiltIns.getInstance().getArrayElementType(type)
            // makeNullable is called here to map primitive types to the corresponding wrappers,
            // because the given type is Array<Something>, not SomethingArray
            return "[" + mapTypeToJvmDesc(TypeUtils.makeNullable(elementType))
        }

        val classDescriptor = classifier as ClassDescriptor
        val fqName = DescriptorUtils.getFqName(classDescriptor)

        KotlinBuiltIns.getPrimitiveTypeByFqName(fqName)?.let { primitiveType ->
            val jvmType = JvmPrimitiveType.get(primitiveType)
            return if (TypeUtils.isNullableType(type)) ClassId.topLevel(jvmType.getWrapperFqName()).desc else jvmType.getDesc()
        }

        KotlinBuiltIns.getPrimitiveTypeByArrayClassFqName(fqName)?.let { primitiveType ->
            return "[" + JvmPrimitiveType.get(primitiveType).getDesc()
        }

        JavaToKotlinClassMap.INSTANCE.mapKotlinToJava(fqName)?.let { return it.desc }

        return classDescriptor.classId.desc
    }

    fun mapJvmClassToKotlinClassId(klass: Class<*>): ClassId {
        if (klass.isArray()) {
            klass.getComponentType().primitiveType?.let {
                return KotlinBuiltIns.getInstance().getPrimitiveArrayClassDescriptor(it).classId
            }
            return ClassId.topLevel(KotlinBuiltIns.FQ_NAMES.array.toSafe())
        }

        klass.primitiveType?.let {
            return KotlinBuiltIns.getInstance().getPrimitiveClassDescriptor(it).classId
        }

        val classId = klass.classId
        if (!classId.isLocal()) {
            JavaToKotlinClassMap.INSTANCE.mapJavaToKotlin(classId.asSingleFqName())?.let { return it.classId }
        }

        return classId
    }

    private val Class<*>.primitiveType: PrimitiveType?
        get() = if (isPrimitive()) JvmPrimitiveType.get(getSimpleName()).getPrimitiveType() else null

    private val ClassId.desc: String
        get() = "L${JvmClassName.byClassId(this).getInternalName()};"
}
