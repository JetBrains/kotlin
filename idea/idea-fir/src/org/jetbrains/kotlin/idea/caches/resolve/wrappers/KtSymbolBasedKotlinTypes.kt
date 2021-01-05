/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve.wrappers

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.refinement.TypeRefinement

abstract class KtSymbolBasedAbstractTypeConstructor<T> internal constructor(
    val ktSBDescriptor: T
) : TypeConstructor where T : KtSymbolBasedDeclarationDescriptor<*>, T : ClassifierDescriptor {
    override fun getDeclarationDescriptor(): ClassifierDescriptor = ktSBDescriptor

    // TODO: captured types
    override fun isDenotable(): Boolean = true

    // for Intention|inspection it shouldn't be important what to use.
    override fun getBuiltIns(): KotlinBuiltIns = DefaultBuiltIns.Instance

    // I don't think that we need to implement this method
    override fun isFinal(): Boolean = implementationPostponed("ktSBDescriptor = $ktSBDescriptor")

    @TypeRefinement
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner): TypeConstructor = noImplementation("ktSBDescriptor = $ktSBDescriptor")

}

class KtSymbolBasedClassTypeConstructor(ktSBDescriptor: KtSymbolBasedClassDescriptor) :
    KtSymbolBasedAbstractTypeConstructor<KtSymbolBasedClassDescriptor>(ktSBDescriptor) {
    override fun getParameters(): List<TypeParameterDescriptor> =
        ktSBDescriptor.ktSymbol.typeParameters.map { KtSymbolBasedTypeParameterDescriptor(it, ktSBDescriptor) }

    override fun getSupertypes(): Collection<KotlinType> {
        TODO("Not yet implemented")
    }
}

class KtSymbolBasedTypeParameterTypeConstructor(ktSBDescriptor: KtSymbolBasedTypeParameterDescriptor) :
    KtSymbolBasedAbstractTypeConstructor<KtSymbolBasedTypeParameterDescriptor>(ktSBDescriptor) {
    override fun getParameters(): List<TypeParameterDescriptor> = emptyList()

    override fun getSupertypes(): Collection<KotlinType> {
        ktSBDescriptor.ktSymbol.upperBounds
        TODO("Not yet implemented")
    }
}

class KtSymbolBasedTypeParameterDescriptor(
    ktSymbol: KtTypeParameterSymbol,
    val containingDeclaration: KtSymbolBasedDeclarationDescriptor<*>
) : KtSymbolBasedDeclarationDescriptor<KtTypeParameterSymbol>(ktSymbol), TypeParameterDescriptor {
    override fun getContainingDeclaration(): DeclarationDescriptor = containingDeclaration
    override fun isReified(): Boolean = ktSymbol.isReified
    override fun getVariance(): Variance = ktSymbol.variance

    override fun getTypeConstructor(): TypeConstructor {
        TODO("Not yet implemented")
    }

    override fun getDefaultType(): SimpleType {
        TODO("Not yet implemented")
    }

    override fun getUpperBounds(): List<KotlinType> {
        TODO("Not yet implemented")
    }

    override fun getOriginal(): TypeParameterDescriptor = this

    // there is no such thing in FIR, and it seems like it isn't really needed for IDE and could be bypassed on client site
    override fun getIndex(): Int = implementationPostponed()

    override fun isCapturedFromOuterDeclaration(): Boolean = noImplementation()
    override fun getStorageManager(): StorageManager = noImplementation()
}