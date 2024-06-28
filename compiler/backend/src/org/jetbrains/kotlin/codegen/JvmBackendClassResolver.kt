/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMapper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.org.objectweb.asm.Type

interface JvmBackendClassResolver {
    fun resolveToClassDescriptors(type: Type): List<ClassDescriptor>

    object Dummy : JvmBackendClassResolver {
        override fun resolveToClassDescriptors(type: Type): List<ClassDescriptor> = emptyList()
    }
}


class JvmBackendClassResolverForModuleWithDependencies(
    private val moduleDescriptor: ModuleDescriptor
) : JvmBackendClassResolver {

    override fun resolveToClassDescriptors(type: Type): List<ClassDescriptor> {
        if (type.sort != Type.OBJECT) return emptyList()

        val platformClass = moduleDescriptor.findClassAcrossModuleDependencies(type.classId) ?: return emptyList()

        return JavaToKotlinClassMapper.mapPlatformClass(platformClass) + platformClass
    }
}

val Type.classId: ClassId
    get() {
        val className = this.className
        val lastDotIndex = className.lastIndexOf('.')
        val packageFQN = if (lastDotIndex >= 0) FqName(className.substring(0, lastDotIndex)) else FqName.ROOT
        val classRelativeNameWithDollars = if (lastDotIndex >= 0) className.substring(lastDotIndex + 1) else className
        val classFQN = FqName(classRelativeNameWithDollars.replace('$', '.'))
        return ClassId(packageFQN, classFQN, isLocal = false)
    }
