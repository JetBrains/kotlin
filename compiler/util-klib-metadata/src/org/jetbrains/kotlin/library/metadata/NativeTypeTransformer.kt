/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.metadata

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.deserialization.PlatformDependentTypeTransformer
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.NativeForwardDeclarationKind
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.SupposititiousSimpleType

class NativeTypeTransformer : PlatformDependentTypeTransformer {
    override fun transformPlatformType(classId: ClassId, computedType: SimpleType): SimpleType {
        val originalPackageFqn = classId.packageFqName
        if (originalPackageFqn in forwardPackagesSet) {
            // This hack is about keeping original class id written into proto which is required for correct IR linkage
            val constructor = computedType.constructor
            val classDescriptor = constructor.declarationDescriptor as ClassDescriptor
            val realPackageFqn = (classDescriptor.containingDeclaration as PackageFragmentDescriptor).fqName
            if (originalPackageFqn != realPackageFqn) {
                return SupposititiousSimpleType(computedType, classId)
            }
        }

        return computedType
    }

    companion object {
        private val forwardPackagesSet = NativeForwardDeclarationKind.entries.map { it.packageFqName }.toSet()
    }
}