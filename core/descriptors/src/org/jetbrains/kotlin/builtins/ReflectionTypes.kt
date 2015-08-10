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

package org.jetbrains.kotlin.builtins

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.addToStdlib.singletonList
import java.util.ArrayList

val KOTLIN_REFLECT_FQ_NAME = FqName("kotlin.reflect")

public class ReflectionTypes(private val module: ModuleDescriptor) {
    private val kotlinReflectScope: JetScope by lazy {
        module.getPackage(KOTLIN_REFLECT_FQ_NAME).memberScope
    }

    fun find(className: String): ClassDescriptor {
        val name = Name.identifier(className)
        return kotlinReflectScope.getClassifier(name) as? ClassDescriptor
                ?: ErrorUtils.createErrorClass(KOTLIN_REFLECT_FQ_NAME.child(name).asString())
    }

    private object ClassLookup {
        fun get(types: ReflectionTypes, property: PropertyMetadata): ClassDescriptor {
            return types.find(property.name.capitalize())
        }
    }

    public fun getKFunction(n: Int): ClassDescriptor = find("KFunction$n")

    public val kClass: ClassDescriptor by ClassLookup
    public val kProperty0: ClassDescriptor by ClassLookup
    public val kProperty1: ClassDescriptor by ClassLookup
    public val kProperty2: ClassDescriptor by ClassLookup
    public val kMutableProperty0: ClassDescriptor by ClassLookup
    public val kMutableProperty1: ClassDescriptor by ClassLookup

    public fun getKClassType(annotations: Annotations, type: JetType): JetType {
        val descriptor = kClass
        if (ErrorUtils.isError(descriptor)) {
            return descriptor.getDefaultType()
        }

        val arguments = listOf(TypeProjectionImpl(Variance.INVARIANT, type))
        return JetTypeImpl.create(annotations, descriptor, false, arguments)
    }

    public fun getKFunctionType(
            annotations: Annotations,
            receiverType: JetType?,
            parameterTypes: List<JetType>,
            returnType: JetType
    ): JetType {
        val arguments = KotlinBuiltIns.getFunctionTypeArgumentProjections(receiverType, parameterTypes, returnType)

        val classDescriptor = getKFunction(arguments.size() - 1 /* return type */)

        if (ErrorUtils.isError(classDescriptor)) {
            return classDescriptor.getDefaultType()
        }

        return JetTypeImpl.create(annotations, classDescriptor, false, arguments)
    }

    public fun getKPropertyType(annotations: Annotations, receiverType: JetType?, returnType: JetType, mutable: Boolean): JetType {
        val classDescriptor =
                when {
                    receiverType != null -> when {
                        mutable -> kMutableProperty1
                        else -> kProperty1
                    }
                    else -> when {
                        mutable -> kMutableProperty0
                        else -> kProperty0
                    }
                }

        if (ErrorUtils.isError(classDescriptor)) {
            return classDescriptor.getDefaultType()
        }

        val arguments = ArrayList<TypeProjection>(2)
        if (receiverType != null) {
            arguments.add(TypeProjectionImpl(receiverType))
        }
        arguments.add(TypeProjectionImpl(returnType))
        return JetTypeImpl.create(annotations, classDescriptor, false, arguments)
    }

    companion object {
        public fun isReflectionClass(descriptor: ClassDescriptor): Boolean {
            val containingPackage = DescriptorUtils.getParentOfType(descriptor, javaClass<PackageFragmentDescriptor>())
            return containingPackage != null && containingPackage.fqName == KOTLIN_REFLECT_FQ_NAME
        }

        private val KCALLABLE_CLASS_NAME = "KCallable"

        public fun isCallableType(type: JetType): Boolean =
                KotlinBuiltIns.isFunctionOrExtensionFunctionType(type) ||
                isCallableReflectionType(type)

        public fun isCallableReflectionType(type: JetType): Boolean =
                isExactCallableReflectionType(type) ||
                type.constructor.getSupertypes().any { isCallableReflectionType(it) }

        public fun isExactCallableReflectionType(type: JetType): Boolean {
            val descriptor = type.getConstructor().getDeclarationDescriptor()
            if (descriptor is ClassDescriptor) {
                val fqName = DescriptorUtils.getFqName(descriptor)
                val parentName = fqName.parent().asString()
                if (parentName != KOTLIN_REFLECT_FQ_NAME.asString())
                    return false
                val shortName = fqName.shortName().asString()
                return KCALLABLE_CLASS_NAME == shortName
            }
            return false
        }
    }
}
