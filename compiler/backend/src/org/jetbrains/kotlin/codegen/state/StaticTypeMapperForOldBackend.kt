/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.state

import org.jetbrains.kotlin.codegen.signature.AsmTypeFactory
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.load.kotlin.TypeMappingConfiguration
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.load.kotlin.mapType
import org.jetbrains.kotlin.types.CommonSupertypes
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.org.objectweb.asm.Type

// This class exists only because it's used to box/unbox inline classes in the static method StackValue.coerce in the old backend, which
// has no access to the correct type mapper instance, yet has a lot of call sites across the old backend, refactoring which would be costly.
object StaticTypeMapperForOldBackend : KotlinTypeMapperBase() {
    override val typeSystem: TypeSystemCommonBackendContext
        get() = SimpleClassicTypeSystemContext

    private val staticTypeMappingConfiguration = object : TypeMappingConfiguration<Type> {
        override fun commonSupertype(types: Collection<KotlinType>): KotlinType {
            return CommonSupertypes.commonSupertype(types)
        }

        override fun getPredefinedTypeForClass(classDescriptor: ClassDescriptor): Type? {
            return null
        }

        override fun getPredefinedInternalNameForClass(classDescriptor: ClassDescriptor): String? {
            return null
        }

        override fun processErrorType(kotlinType: KotlinType, descriptor: ClassDescriptor) {
            throw IllegalStateException(KotlinTypeMapper.generateErrorMessageForErrorType(kotlinType, descriptor))
        }

        override fun preprocessType(kotlinType: KotlinType): KotlinType? {
            return null
        }
    }

    override fun mapClass(classifier: ClassifierDescriptor): Type = TODO("Should not be called")

    override fun mapTypeCommon(type: KotlinTypeMarker, mode: TypeMappingMode): Type {
        return mapType(type as KotlinType, AsmTypeFactory, mode, staticTypeMappingConfiguration, null)
    }
}
