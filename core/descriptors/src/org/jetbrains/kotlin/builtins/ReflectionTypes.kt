/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.*
import kotlin.reflect.KProperty

val KOTLIN_REFLECT_FQ_NAME = FqName("kotlin.reflect")
val K_FUNCTION_PREFIX = "KFunction"

class ReflectionTypes(module: ModuleDescriptor, private val notFoundClasses: NotFoundClasses) {
    private val kotlinReflectScope: MemberScope by lazy(LazyThreadSafetyMode.PUBLICATION) {
        module.getPackage(KOTLIN_REFLECT_FQ_NAME).memberScope
    }

    private fun find(className: String, numberOfTypeParameters: Int): ClassDescriptor {
        val name = Name.identifier(className)
        return kotlinReflectScope.getContributedClassifier(name, NoLookupLocation.FROM_REFLECTION) as? ClassDescriptor
                ?: notFoundClasses.getClass(ClassId(KOTLIN_REFLECT_FQ_NAME, name), listOf(numberOfTypeParameters))
    }

    private class ClassLookup(val numberOfTypeParameters: Int) {
        operator fun getValue(types: ReflectionTypes, property: KProperty<*>): ClassDescriptor {
            return types.find(property.name.capitalize(), numberOfTypeParameters)
        }
    }

    fun getKFunction(n: Int): ClassDescriptor = find("$K_FUNCTION_PREFIX$n", n + 1)

    val kClass: ClassDescriptor by ClassLookup(1)
    val kProperty0: ClassDescriptor by ClassLookup(1)
    val kProperty1: ClassDescriptor by ClassLookup(2)
    val kProperty2: ClassDescriptor by ClassLookup(3)
    val kMutableProperty0: ClassDescriptor by ClassLookup(1)
    val kMutableProperty1: ClassDescriptor by ClassLookup(2)
    val kMutableProperty2: ClassDescriptor by ClassLookup(3)

    fun getKClassType(annotations: Annotations, type: KotlinType, variance: Variance): KotlinType =
            KotlinTypeFactory.simpleNotNullType(annotations, kClass, listOf(TypeProjectionImpl(variance, type)))

    fun getKFunctionType(
            annotations: Annotations,
            receiverType: KotlinType?,
            parameterTypes: List<KotlinType>,
            parameterNames: List<Name>?,
            returnType: KotlinType,
            builtIns: KotlinBuiltIns
    ): SimpleType {
        val arguments = getFunctionTypeArgumentProjections(receiverType, parameterTypes, parameterNames, returnType, builtIns)
        val classDescriptor = getKFunction(arguments.size - 1 /* return type */)
        return KotlinTypeFactory.simpleNotNullType(annotations, classDescriptor, arguments)
    }

    fun getKPropertyType(annotations: Annotations, receiverTypes: List<KotlinType>, returnType: KotlinType, mutable: Boolean): SimpleType {
        val classDescriptor = when (receiverTypes.size) {
            0 -> when {
                mutable -> kMutableProperty0
                else -> kProperty0
            }
            1 -> when {
                mutable -> kMutableProperty1
                else -> kProperty1
            }
            2 ->  when {
                mutable -> kMutableProperty2
                else -> kProperty2
            }
            else -> throw AssertionError("More than 2 receivers is not allowed")
        }

        val arguments = (receiverTypes + returnType).map(::TypeProjectionImpl)
        return KotlinTypeFactory.simpleNotNullType(annotations, classDescriptor, arguments)
    }

    companion object {
        fun isReflectionClass(descriptor: ClassDescriptor): Boolean {
            val containingPackage = DescriptorUtils.getParentOfType(descriptor, PackageFragmentDescriptor::class.java)
            return containingPackage != null && containingPackage.fqName == KOTLIN_REFLECT_FQ_NAME
        }

        fun isCallableType(type: KotlinType): Boolean =
                type.isFunctionTypeOrSubtype || isKCallableType(type)

        @JvmStatic
        fun isNumberedKPropertyOrKMutablePropertyType(type: KotlinType): Boolean =
                isNumberedKPropertyType(type) || isNumberedKMutablePropertyType(type)

        private fun isKCallableType(type: KotlinType): Boolean =
                hasFqName(type.constructor, KotlinBuiltIns.FQ_NAMES.kCallable) ||
                type.constructor.supertypes.any { isKCallableType(it) }

        fun isNumberedKMutablePropertyType(type: KotlinType): Boolean {
            val descriptor = type.constructor.declarationDescriptor as? ClassDescriptor ?: return false
            return hasFqName(descriptor, KotlinBuiltIns.FQ_NAMES.kMutableProperty0) ||
                   hasFqName(descriptor, KotlinBuiltIns.FQ_NAMES.kMutableProperty1) ||
                   hasFqName(descriptor, KotlinBuiltIns.FQ_NAMES.kMutableProperty2)
        }

        fun isNumberedKPropertyType(type: KotlinType): Boolean {
            val descriptor = type.constructor.declarationDescriptor as? ClassDescriptor ?: return false
            return hasFqName(descriptor, KotlinBuiltIns.FQ_NAMES.kProperty0) ||
                   hasFqName(descriptor, KotlinBuiltIns.FQ_NAMES.kProperty1) ||
                   hasFqName(descriptor, KotlinBuiltIns.FQ_NAMES.kProperty2)
        }

        fun isNumberedKFunction(type: KotlinType): Boolean {
            val descriptor = type.constructor.declarationDescriptor as? ClassDescriptor ?: return false
            val shortName = descriptor.name.asString()

            return shortName.length > K_FUNCTION_PREFIX.length &&
                   shortName.startsWith(K_FUNCTION_PREFIX) &&
                   DescriptorUtils.getFqName(descriptor).parent().toSafe() == KOTLIN_REFLECT_FQ_NAME
        }

        private fun hasFqName(typeConstructor: TypeConstructor, fqName: FqNameUnsafe): Boolean {
            val descriptor = typeConstructor.declarationDescriptor
            return descriptor is ClassDescriptor && hasFqName(descriptor, fqName)
        }

        private fun hasFqName(descriptor: ClassDescriptor, fqName: FqNameUnsafe): Boolean {
            return descriptor.name == fqName.shortName() && DescriptorUtils.getFqName(descriptor) == fqName
        }

        fun createKPropertyStarType(module: ModuleDescriptor): KotlinType? {
            val kPropertyClass = module.findClassAcrossModuleDependencies(KotlinBuiltIns.FQ_NAMES.kProperty) ?: return null
            return KotlinTypeFactory.simpleNotNullType(Annotations.EMPTY, kPropertyClass,
                                                       listOf(StarProjectionImpl(kPropertyClass.typeConstructor.parameters.single())))
        }
    }
}
