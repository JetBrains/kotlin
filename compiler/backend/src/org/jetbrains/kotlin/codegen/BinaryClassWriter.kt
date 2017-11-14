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

import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.Type

class BinaryClassWriter(
        private val jvmBackendClassResolver: JvmBackendClassResolver
) : ClassWriter(COMPUTE_FRAMES or COMPUTE_MAXS) {
    override fun getCommonSuperClass(type1: String, type2: String): String {
        // This method is needed to generate StackFrameMap: bytecode metadata for JVM verification. For bytecode version 50.0 (JDK 6)
        // these maps can be invalid: in this case, JVM would generate them itself (potentially slowing class loading),
        // for bytecode 51.0+ (JDK 7+) JVM would crash with VerifyError.

        val classes1 = jvmBackendClassResolver.resolveToClassDescriptors(Type.getObjectType(type1))
        val classes2 = jvmBackendClassResolver.resolveToClassDescriptors(Type.getObjectType(type2))

        if (classes1.isNotEmpty() && classes2.isNotEmpty()) {
            for (class1 in classes1) {
                for (class2 in classes2) {
                    if (DescriptorUtils.isSubclass(class1, class2)) return type2
                    if (DescriptorUtils.isSubclass(class2, class1)) return type1
                }
            }
        }

        // It seems that for bytecode emitted by Kotlin compiler, it is safe to return "Object" here, because there will
        // be "checkcast" generated before making a call, anyway.
        return "java/lang/Object"
    }
}