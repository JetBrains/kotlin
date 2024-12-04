/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.error.ErrorUtils

abstract class ClassifierBasedTypeConstructor : TypeConstructor {
    private var hashCode = 0

    abstract override fun getDeclarationDescriptor(): ClassifierDescriptor

    override fun hashCode(): Int {
        val cachedHashCode = hashCode
        if (cachedHashCode != 0) return cachedHashCode

        val descriptor = declarationDescriptor
        val computedHashCode = if (hasMeaningfulFqName(descriptor)) {
            DescriptorUtils.getFqName(descriptor).hashCode()
        } else {
            System.identityHashCode(this)
        }

        return computedHashCode.also { hashCode = it }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TypeConstructor) return false

        // performance optimization: getFqName is slow method
        // Cast to Any is needed as a workaround for KT-45008.
        if ((other as Any).hashCode() != hashCode()) return false

        // Sometimes we can get two classes from different modules with different counts of type parameters.
        // To avoid problems in type checker we suppose that it is different type constructors.
        if (other.parameters.size != parameters.size) return false

        val myDescriptor = declarationDescriptor
        val otherDescriptor = other.declarationDescriptor ?: return false
        if (!hasMeaningfulFqName(myDescriptor) || !hasMeaningfulFqName(otherDescriptor)) {
            // All error types and local classes have the same descriptor,
            // but we've already checked identity equality in the beginning of the method
            return false
        }

        return isSameClassifier(otherDescriptor)
    }

    protected abstract fun isSameClassifier(classifier: ClassifierDescriptor): Boolean

    protected fun areFqNamesEqual(first: ClassifierDescriptor, second: ClassifierDescriptor): Boolean {
        if (first.name != second.name) return false
        var a: DeclarationDescriptor? = first.containingDeclaration
        var b: DeclarationDescriptor? = second.containingDeclaration
        while (a != null && b != null) {
            when {
                a is ModuleDescriptor -> return b is ModuleDescriptor
                b is ModuleDescriptor -> return false
                a is PackageFragmentDescriptor -> return b is PackageFragmentDescriptor && a.fqName == b.fqName
                b is PackageFragmentDescriptor -> return false
                a.name != b.name -> return false
                else -> {
                    a = a.containingDeclaration
                    b = b.containingDeclaration
                }
            }
        }
        return true
    }

    private fun hasMeaningfulFqName(descriptor: ClassifierDescriptor): Boolean =
        !ErrorUtils.isError(descriptor) && !DescriptorUtils.isLocal(descriptor)
}