/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.builtins

import org.jetbrains.kotlin.builtins.StandardNames.BUILT_INS_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.builtins.StandardNames.COROUTINES_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.builtins.StandardNames.KOTLIN_REFLECT_FQ_NAME
import org.jetbrains.kotlin.builtins.StandardNames.K_FUNCTION_PREFIX
import org.jetbrains.kotlin.builtins.StandardNames.K_SUSPEND_FUNCTION_PREFIX
import org.jetbrains.kotlin.builtins.StandardNames.PREFIXES
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import kotlin.reflect.KProperty

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
            return types.find(property.name.capitalizeAsciiOnly(), numberOfTypeParameters)
        }
    }

    fun getKFunction(n: Int): ClassDescriptor = find("$K_FUNCTION_PREFIX$n", n + 1)
    fun getKSuspendFunction(n: Int): ClassDescriptor = find("$K_SUSPEND_FUNCTION_PREFIX$n", n + 1)

    val kClass: ClassDescriptor by ClassLookup(1)
    val kProperty: ClassDescriptor by ClassLookup(1)
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
        contextReceiverTypes: List<KotlinType>,
        parameterTypes: List<KotlinType>,
        parameterNames: List<Name>?,
        returnType: KotlinType,
        builtIns: KotlinBuiltIns,
        isSuspend: Boolean
    ): SimpleType {
        val arguments =
            getFunctionTypeArgumentProjections(receiverType, contextReceiverTypes, parameterTypes, parameterNames, returnType, builtIns)
        val classDescriptor =
            if (isSuspend) getKSuspendFunction(arguments.size - 1 /* return type */) else getKFunction(arguments.size - 1 /* return type */)
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
            type.isFunctionTypeOrSubtype || type.isSuspendFunctionTypeOrSubtype || isKCallableType(type)

        fun isBaseTypeForNumberedReferenceTypes(type: KotlinType): Boolean =
            ReflectionTypes.hasKPropertyTypeFqName(type) ||
                    ReflectionTypes.hasKMutablePropertyTypeFqName(type) ||
                    ReflectionTypes.hasKCallableTypeFqName(type)

        @JvmStatic
        fun isNumberedKPropertyOrKMutablePropertyType(type: KotlinType): Boolean =
                isNumberedKPropertyType(type) || isNumberedKMutablePropertyType(type)

        fun isKCallableType(type: KotlinType): Boolean =
            hasKCallableTypeFqName(type) || type.constructor.supertypes.any { isKCallableType(it) }

        fun hasKCallableTypeFqName(type: KotlinType): Boolean =
            hasFqName(type.constructor, StandardNames.FqNames.kCallable)

        fun hasKMutablePropertyTypeFqName(type: KotlinType): Boolean =
            hasFqName(type.constructor, StandardNames.FqNames.kMutablePropertyFqName)

        fun isNumberedKMutablePropertyType(type: KotlinType): Boolean {
            val descriptor = type.constructor.declarationDescriptor as? ClassDescriptor ?: return false
            return hasFqName(descriptor, StandardNames.FqNames.kMutableProperty0) ||
                   hasFqName(descriptor, StandardNames.FqNames.kMutableProperty1) ||
                   hasFqName(descriptor, StandardNames.FqNames.kMutableProperty2)
        }

        fun isNumberedTypeWithOneOrMoreNumber(type: KotlinType): Boolean {
            val descriptor = type.constructor.declarationDescriptor as? ClassDescriptor ?: return false
            val fqName = DescriptorUtils.getFqName(descriptor)
            if (fqName.isRoot) return false

            if (fqName.parent().toSafe() != KOTLIN_REFLECT_FQ_NAME) return false
            val shortName = descriptor.name.asString()

            for (prefix in PREFIXES) {
                if (shortName.startsWith(prefix)) {
                    val number = shortName.removePrefix(prefix)
                    return number.isNotEmpty() && number != "0"
                }
            }
            return false
        }

        fun hasKPropertyTypeFqName(type: KotlinType): Boolean =
            hasFqName(type.constructor, StandardNames.FqNames.kPropertyFqName)

        fun isNumberedKPropertyType(type: KotlinType): Boolean {
            val descriptor = type.constructor.declarationDescriptor as? ClassDescriptor ?: return false
            return hasFqName(descriptor, StandardNames.FqNames.kProperty0) ||
                   hasFqName(descriptor, StandardNames.FqNames.kProperty1) ||
                   hasFqName(descriptor, StandardNames.FqNames.kProperty2)
        }

        fun isNumberedKFunctionOrKSuspendFunction(type: KotlinType): Boolean {
            return isNumberedKFunction(type) || isNumberedKSuspendFunction(type)
        }

        fun isNumberedKFunction(type: KotlinType): Boolean {
            val descriptor = type.constructor.declarationDescriptor as? ClassDescriptor ?: return false
            val shortName = descriptor.name.asString()

            return (shortName.length > K_FUNCTION_PREFIX.length && shortName.startsWith(K_FUNCTION_PREFIX)) &&
                    DescriptorUtils.getFqName(descriptor).parent().toSafe() == KOTLIN_REFLECT_FQ_NAME
        }

        fun isNumberedKSuspendFunction(type: KotlinType): Boolean {
            val descriptor = type.constructor.declarationDescriptor as? ClassDescriptor ?: return false
            val shortName = descriptor.name.asString()
            return shortName.length > K_SUSPEND_FUNCTION_PREFIX.length && shortName.startsWith(K_SUSPEND_FUNCTION_PREFIX) &&
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
            val kPropertyClass = module.findClassAcrossModuleDependencies(StandardNames.FqNames.kProperty) ?: return null
            return KotlinTypeFactory.simpleNotNullType(Annotations.EMPTY, kPropertyClass,
                                                       listOf(StarProjectionImpl(kPropertyClass.typeConstructor.parameters.single())))
        }

        fun isPossibleExpectedCallableType(typeConstructor: TypeConstructor): Boolean {
            val descriptor = typeConstructor.declarationDescriptor as? ClassDescriptor ?: return false
            if (KotlinBuiltIns.isAny(descriptor)) return true

            val shortName = descriptor.name.asString()

            val fqName = DescriptorUtils.getFqName(descriptor)
            if (fqName.isRoot) return false

            val packageName = fqName.parent().toSafe()
            if (packageName == KOTLIN_REFLECT_FQ_NAME) {
                return shortName.startsWith("KFunction") // KFunctionN, KFunction
                       || shortName.startsWith("KSuspendFunction") // KSuspendFunctionN
                       || shortName.startsWith("KProperty") // KPropertyN, KProperty
                       || shortName.startsWith("KMutableProperty") // KMutablePropertyN, KMutableProperty
                       || shortName == "KCallable" || shortName == "KAnnotatedElement"

            }
            if (packageName == BUILT_INS_PACKAGE_FQ_NAME || packageName == COROUTINES_PACKAGE_FQ_NAME) {
                return shortName.startsWith("Function") // FunctionN, Function
                        || shortName.startsWith("SuspendFunction")
            }

            return false
        }
    }
}
