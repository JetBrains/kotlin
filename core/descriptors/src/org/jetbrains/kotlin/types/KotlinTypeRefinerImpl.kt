/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.checker.NewCapturedTypeConstructor
import org.jetbrains.kotlin.types.checker.REFINER_CAPABILITY
import org.jetbrains.kotlin.types.checker.TypeRefinementSupport
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.utils.DFS

@OptIn(TypeRefinement::class)
class KotlinTypeRefinerImpl(
    private val moduleDescriptor: ModuleDescriptor,
    storageManager: StorageManager
) : KotlinTypeRefiner() {
    private var isStandalone: Boolean = false

    private constructor(
        moduleDescriptor: ModuleDescriptor,
        storageManager: StorageManager,
        isStandalone: Boolean
    ) : this(moduleDescriptor, storageManager) {
        this.isStandalone = isStandalone
    }

    init {
        if (!isStandalone) {
            moduleDescriptor.getCapability(REFINER_CAPABILITY)?.value = TypeRefinementSupport.Enabled(this)
        }
    }

    private val refinedTypeCache = storageManager.createCacheWithNotNullValues<TypeConstructor, KotlinType>()
    private val isRefinementNeededForTypeConstructorCache = storageManager.createCacheWithNotNullValues<ClassifierDescriptor, Boolean>()
    private val scopes = storageManager.createCacheWithNotNullValues<ClassDescriptor, MemberScope>()

    /**
     * IMPORTANT: that function has not obvious contract: it refines only supertypes,
     *   and don't refines type arguments, so return type is "partly refined".
     *
     *   It's fine for subtyping, because we refine type arguments inside type checker when it needs to
     *   It's fine for scopes, because we refine type of every expression:
     *
     *   // common module
     *   expect interface A
     *   class Inv<T>(val value: T)
     *   fun getA(): Inv<A> = ...
     *
     *   // platform module
     *
     *   actual interface A {
     *       val x: Int
     *   }
     *
     *   fun foo() {
     *      getA().value.x
     *   }
     *
     *   Let's call type of `actual interface A` A'
     *
     *   expression `getA()` has not refined type Inv<A> and same refined type
     *   expression `getA().value` has not refined type A that refines into type A', so there is a
     *     field `x` in it's member scope
     */
    @TypeRefinement
    override fun refineType(type: KotlinTypeMarker): KotlinType {
        require(type is KotlinType)
        if (type.constructor.declarationDescriptor?.module == moduleDescriptor) return type
        return when {
            !type.needsRefinement() -> type

            type.canBeCached() -> {
                val cached = refinedTypeCache.computeIfAbsent(type.constructor) {
                    type.constructor.declarationDescriptor!!.defaultType.refineWithRespectToAbbreviatedTypes(this)
                }

                cached.restoreAdditionalTypeInformation(type)
            }

            else -> type.refineWithRespectToAbbreviatedTypes(this)
        }
    }

    private fun KotlinType.refineWithRespectToAbbreviatedTypes(refiner: KotlinTypeRefiner): KotlinType {
        var previousRefinement: KotlinType
        var currentRefinement: KotlinType = this

        do {
            previousRefinement = currentRefinement
            currentRefinement = previousRefinement.refine(refiner)
        } while (currentRefinement is AbbreviatedType && currentRefinement != previousRefinement)

        return currentRefinement
    }

    private fun KotlinType.needsRefinement(): Boolean = isRefinementNeededForTypeConstructor(constructor)

    private fun KotlinType.canBeCached(): Boolean = hasNotTrivialRefinementFactory && constructor.declarationDescriptor != null

    @TypeRefinement
    override fun refineSupertypes(classDescriptor: ClassDescriptor): Collection<KotlinType> {
        // Note that we can't omit refinement even if classDescriptor.module == moduleDescriptor,
        // because such class may have supertypes which need refinement
        return classDescriptor.typeConstructor.supertypes.map { refineType(it) }
    }

    @TypeRefinement
    override fun refineDescriptor(descriptor: DeclarationDescriptor): ClassifierDescriptor? {
        if (descriptor !is ClassifierDescriptorWithTypeParameters) return null
        val classId = descriptor.classId ?: return null
        return moduleDescriptor.findClassifierAcrossModuleDependencies(classId)
    }

    @TypeRefinement
    override fun findClassAcrossModuleDependencies(classId: ClassId): ClassDescriptor? {
        return moduleDescriptor.findClassAcrossModuleDependencies(classId)
    }

    @TypeRefinement
    override fun isRefinementNeededForModule(moduleDescriptor: ModuleDescriptor): Boolean {
        return this.moduleDescriptor !== moduleDescriptor
    }

    @TypeRefinement
    override fun isRefinementNeededForTypeConstructor(typeConstructor: TypeConstructor): Boolean {
        val owner = typeConstructor.declarationDescriptor
            ?: return typeConstructor.isRefinementNeededForTypeConstructorNoCache()
        return isRefinementNeededForTypeConstructorCache.computeIfAbsent(owner) {
            typeConstructor.isRefinementNeededForTypeConstructorNoCache()
        }
    }

    @TypeRefinement
    override fun <S : MemberScope> getOrPutScopeForClass(classDescriptor: ClassDescriptor, compute: () -> S): S {
        @Suppress("UNCHECKED_CAST")
        return scopes.computeIfAbsent(classDescriptor, compute) as S
    }

    private fun TypeConstructor.isRefinementNeededForTypeConstructorNoCache(): Boolean {
        return declarationDescriptor.isEnumEntryOrEnum() || areThereExpectSupertypes()
    }

    // Enum-type itself should be refined because on JVM it has Serializable
    // supertype, but it's not marked as expect.
    // Enum entries need refinement only to force refinement of the Enum-type
    // in their supertypes.
    private fun DeclarationDescriptor?.isEnumEntryOrEnum(): Boolean =
        if (this is ClassDescriptor)
            kind == ClassKind.ENUM_CLASS || KotlinBuiltIns.isEnum(this)
        else
            false

    private fun TypeConstructor.areThereExpectSupertypes(): Boolean {
        var result = false
        DFS.dfs(
            listOf(this),
            DFS.Neighbors(TypeConstructor::allDependentTypeConstructors),
            DFS.VisitedWithSet(),
            object : DFS.AbstractNodeHandler<TypeConstructor, Unit>() {
                override fun beforeChildren(current: TypeConstructor): Boolean {
                    if (current.isExpectClass() && current.declarationDescriptor?.module != moduleDescriptor) {
                        result = true
                        return false
                    }
                    return true
                }

                override fun result() = Unit
            }
        )

        return result
    }

    companion object {
        /**
         * Create a new *thread unsafe* type refiner instance for the specified module.
         * Note, that module's type refiner capability won't be changed.
         */
        fun createStandaloneInstanceFor(moduleDescriptor: ModuleDescriptor): KotlinTypeRefinerImpl =
            KotlinTypeRefinerImpl(moduleDescriptor, LockBasedStorageManager.NO_LOCKS, isStandalone = true)
    }
}

private val TypeConstructor.allDependentTypeConstructors: Collection<TypeConstructor>
    get() = when (this) {
        is NewCapturedTypeConstructor -> {
            supertypes.map { it.constructor } + projection.type.constructor
        }

        else -> supertypes.map { it.constructor }
    }

private fun TypeConstructor.isExpectClass(): Boolean =
    (declarationDescriptor as? ClassDescriptor)?.isExpect == true

private fun KotlinType.restoreAdditionalTypeInformation(prototype: KotlinType): KotlinType {
    return TypeUtils.makeNullableAsSpecified(this, prototype.isMarkedNullable).replace(prototype.arguments)
}
