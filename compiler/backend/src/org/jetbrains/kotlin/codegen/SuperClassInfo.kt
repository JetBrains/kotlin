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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.codegen.JvmCodegenUtil.isJvmInterface
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Type

class SuperClassInfo(
    val type: Type,
    // null means java/lang/Object or irrelevant
    val kotlinType: KotlinType?
) {

    companion object {
        @JvmStatic
        fun getSuperClassInfo(descriptor: ClassDescriptor, typeMapper: KotlinTypeMapper): SuperClassInfo {
            if (descriptor.kind == ClassKind.INTERFACE) {
                return SuperClassInfo(OBJECT_TYPE, null)
            }

            for (supertype in descriptor.typeConstructor.supertypes) {
                val superClass = supertype.constructor.declarationDescriptor
                if (superClass != null && !isJvmInterface(superClass)) {
                    return SuperClassInfo(typeMapper.mapClass(superClass), supertype)
                }
            }

            return SuperClassInfo(OBJECT_TYPE, null)
        }
    }

}