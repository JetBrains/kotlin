/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.checker

import org.jetbrains.kotlin.container.DefaultImplementation
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.AbstractTypeRefiner
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.TypeRefinement

@DefaultImplementation(impl = KotlinTypeRefiner.Default::class)
abstract class KotlinTypeRefiner : AbstractTypeRefiner() {
    @TypeRefinement
    abstract override fun refineType(type: KotlinTypeMarker): KotlinType

    @TypeRefinement
    abstract fun refineSupertypes(classDescriptor: ClassDescriptor): Collection<KotlinType>

    @TypeRefinement
    abstract fun refineDescriptor(descriptor: DeclarationDescriptor): ClassifierDescriptor?

    @TypeRefinement
    abstract fun findClassAcrossModuleDependencies(classId: ClassId): ClassDescriptor?

    @TypeRefinement
    abstract fun isRefinementNeededForModule(moduleDescriptor: ModuleDescriptor): Boolean

    @TypeRefinement
    abstract fun isRefinementNeededForTypeConstructor(typeConstructor: TypeConstructor): Boolean

    @TypeRefinement
    abstract fun <S : MemberScope> getOrPutScopeForClass(classDescriptor: ClassDescriptor, compute: () -> S): S

    object Default : KotlinTypeRefiner() {
        @TypeRefinement
        override fun refineType(type: KotlinTypeMarker): KotlinType = type as KotlinType

        @TypeRefinement
        override fun refineSupertypes(classDescriptor: ClassDescriptor): Collection<KotlinType> {
            return classDescriptor.typeConstructor.supertypes
        }

        @TypeRefinement
        override fun refineDescriptor(descriptor: DeclarationDescriptor): ClassDescriptor? {
            return null
        }

        @TypeRefinement
        override fun findClassAcrossModuleDependencies(classId: ClassId): ClassDescriptor? {
            return null
        }

        @TypeRefinement
        override fun isRefinementNeededForModule(moduleDescriptor: ModuleDescriptor): Boolean {
            return false
        }

        @TypeRefinement
        override fun isRefinementNeededForTypeConstructor(typeConstructor: TypeConstructor): Boolean {
            return false
        }

        @TypeRefinement
        override fun <S : MemberScope> getOrPutScopeForClass(classDescriptor: ClassDescriptor, compute: () -> S): S {
            return compute()
        }
    }
}

@TypeRefinement
fun KotlinTypeRefiner.refineTypes(types: Iterable<KotlinType>): List<KotlinType> = types.map { refineType(it) }

class Ref<T : Any>(var value: T)

@TypeRefinement
val REFINER_CAPABILITY = ModuleCapability<Ref<TypeRefinementSupport>>("KotlinTypeRefiner")

sealed class TypeRefinementSupport(val isEnabled: Boolean) {
    object Disabled : TypeRefinementSupport(isEnabled = false)
    object EnabledUninitialized : TypeRefinementSupport(isEnabled = true)
    class Enabled(val typeRefiner: KotlinTypeRefiner) : TypeRefinementSupport(isEnabled = true)
}
