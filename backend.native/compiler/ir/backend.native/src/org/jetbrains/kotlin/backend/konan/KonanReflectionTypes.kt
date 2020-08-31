/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan


import org.jetbrains.kotlin.builtins.StandardNames.KOTLIN_REFLECT_FQ_NAME
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import kotlin.reflect.KProperty

class KonanReflectionTypes(module: ModuleDescriptor, internalPackage: FqName) {

    private val kotlinReflectScope: MemberScope by lazy(LazyThreadSafetyMode.PUBLICATION) {
        module.getPackage(KOTLIN_REFLECT_FQ_NAME).memberScope
    }

    private val internalScope: MemberScope by lazy(LazyThreadSafetyMode.PUBLICATION) {
        module.getPackage(internalPackage).memberScope
    }

    private fun find(memberScope: MemberScope, className: String): ClassDescriptor {
        val name = Name.identifier(className)
        return memberScope.getContributedClassifier(name, NoLookupLocation.FROM_REFLECTION) as ClassDescriptor
    }

    private class ClassLookup(val memberScope: MemberScope) {
        operator fun getValue(types: KonanReflectionTypes, property: KProperty<*>): ClassDescriptor {
            return types.find(memberScope, property.name.capitalize())
        }
    }

    fun getKFunction(n: Int): ClassDescriptor = find(kotlinReflectScope, "KFunction$n")

    fun getKSuspendFunction(n: Int): ClassDescriptor = find(kotlinReflectScope, "KSuspendFunction$n")

    val kProperty0: ClassDescriptor by ClassLookup(kotlinReflectScope)
    val kMutableProperty0: ClassDescriptor by ClassLookup(kotlinReflectScope)
    val kMutableProperty1: ClassDescriptor by ClassLookup(kotlinReflectScope)
    val kMutableProperty2: ClassDescriptor by ClassLookup(kotlinReflectScope)
    val kTypeProjection: ClassDescriptor by ClassLookup(kotlinReflectScope)

    val kFunctionImpl: ClassDescriptor by ClassLookup(internalScope)
    val kSuspendFunctionImpl: ClassDescriptor by ClassLookup(internalScope)
    val kProperty0Impl: ClassDescriptor by ClassLookup(internalScope)
    val kProperty1Impl: ClassDescriptor by ClassLookup(internalScope)
    val kProperty2Impl: ClassDescriptor by ClassLookup(internalScope)
    val kMutableProperty0Impl: ClassDescriptor by ClassLookup(internalScope)
    val kMutableProperty1Impl: ClassDescriptor by ClassLookup(internalScope)
    val kMutableProperty2Impl: ClassDescriptor by ClassLookup(internalScope)
    val kLocalDelegatedPropertyImpl: ClassDescriptor by ClassLookup(internalScope)
    val kLocalDelegatedMutablePropertyImpl: ClassDescriptor by ClassLookup(internalScope)
}
