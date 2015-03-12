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
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.load.java.structure.reflect.classId
import org.jetbrains.kotlin.load.java.structure.reflect.desc
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.JavaToKotlinClassMapBuilder
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeUtils

object RuntimeTypeMapper : JavaToKotlinClassMapBuilder() {
    private val kotlinFqNameToJvmDesc = linkedMapOf<FqName, String>()
    private val kotlinFqNameToJvmDescNullable = linkedMapOf<FqName, String>()
    private val jvmDescToKotlinClassId = linkedMapOf<String, ClassId>();

    {
        init()
        initPrimitives()
    }

    private fun initPrimitives() {
        val builtIns = KotlinBuiltIns.getInstance()

        for (type in JvmPrimitiveType.values()) {
            val primitiveType = type.getPrimitiveType()
            val primitiveClassDescriptor = builtIns.getPrimitiveClassDescriptor(primitiveType)

            recordMapping(primitiveClassDescriptor, type.getDesc())
            recordMapping(builtIns.getPrimitiveArrayClassDescriptor(primitiveType), "[" + type.getDesc())

            recordNullableMapping(primitiveClassDescriptor, ClassId.topLevel(type.getWrapperFqName()).desc)
        }
    }

    private fun recordMapping(kotlinDescriptor: ClassDescriptor, jvmDesc: String) {
        kotlinFqNameToJvmDesc[DescriptorUtils.getFqNameSafe(kotlinDescriptor)] = jvmDesc
        jvmDescToKotlinClassId[jvmDesc] = kotlinDescriptor.classId

        val defaultObject = kotlinDescriptor.getDefaultObjectDescriptor()
        if (defaultObject != null) {
            // TODO: see org.jetbrains.kotlin.codegen.intrinsics.IntrinsicObjects, extract that logic to core/
            recordMapping(defaultObject, "Lkotlin/jvm/internal/${kotlinDescriptor.getName().asString()}DefaultObject;")
        }
    }

    private fun recordNullableMapping(kotlinDescriptor: ClassDescriptor, jvmDesc: String) {
        kotlinFqNameToJvmDescNullable[DescriptorUtils.getFqNameSafe(kotlinDescriptor)] = jvmDesc
        jvmDescToKotlinClassId[jvmDesc] = kotlinDescriptor.classId
    }

    override fun register(javaClass: Class<*>, kotlinDescriptor: ClassDescriptor, direction: JavaToKotlinClassMapBuilder.Direction) {
        recordMapping(kotlinDescriptor, javaClass.classId.desc)
    }

    override fun register(javaClass: Class<*>, kotlinDescriptor: ClassDescriptor, kotlinMutableDescriptor: ClassDescriptor) {
        // TODO: readonly collection mapping just rewrites the mutable one, improve readability here
        register(javaClass, kotlinMutableDescriptor, JavaToKotlinClassMapBuilder.Direction.BOTH)
        register(javaClass, kotlinDescriptor, JavaToKotlinClassMapBuilder.Direction.BOTH)
    }

    fun mapTypeToJvmDesc(type: JetType): String {
        val classifier = type.getConstructor().getDeclarationDescriptor()
        if (classifier is TypeParameterDescriptor) return mapTypeToJvmDesc(classifier.getUpperBounds().first())

        val classDescriptor = classifier as ClassDescriptor
        val fqNameUnsafe = DescriptorUtils.getFqName(classDescriptor)
        if (fqNameUnsafe.isSafe()) {
            val fqName = fqNameUnsafe.toSafe()
            if (TypeUtils.isNullableType(type)) {
                kotlinFqNameToJvmDescNullable[fqName]?.let { return it }
            }
            kotlinFqNameToJvmDesc[fqName]?.let { return it }
        }

        if (KotlinBuiltIns.isArray(type)) {
            var dimension = 0
            var elementType = type

            while (KotlinBuiltIns.isArray(elementType)) {
                elementType = KotlinBuiltIns.getInstance().getArrayElementType(elementType)
                dimension++
            }

            return "[".repeat(dimension) + mapTypeToJvmDesc(TypeUtils.makeNullable(elementType))
        }

        return classDescriptor.classId.desc
    }

    fun mapJvmClassToKotlinClassId(klass: Class<*>): ClassId {
        return jvmDescToKotlinClassId[klass.desc] ?: klass.classId
    }

    private val ClassId.desc: String
        get() = "L${JvmClassName.byClassId(this).getInternalName()};"
}
