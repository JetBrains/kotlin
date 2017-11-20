/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.serialization.jvm

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType

// The purpose of this class is to map Kotlin classes to JVM bytecode desc strings, as KotlinTypeMapper does in the backend.
// It's used as an optimization during serialization/deserialization: if there's no JVM signature for a method/property/constructor,
// it means that the JVM signature should be trivially computable from the Kotlin signature with this class.
// It's not required to support everything in KotlinTypeMapper, but the more it does, the more we save on JVM signatures in proto metadata.
// Note that improving the behavior of this class may break binary compatibility of code compiled by Kotlin, because it may make
// the new compiler skip writing the signatures it now thinks are trivial, and the old compiler would recreate them incorrectly.
object ClassMapperLite {
    @JvmStatic
    fun mapClass(classId: ClassId): String {
        val internalName = classId.asString().replace('.', '$')
        val simpleName = internalName.removePrefix("kotlin/")
        if (simpleName != internalName) {
            for (jvmPrimitive in JvmPrimitiveType.values()) {
                val primitiveType = jvmPrimitive.primitiveType
                if (simpleName == primitiveType.typeName.asString()) return jvmPrimitive.desc
                if (simpleName == primitiveType.arrayTypeName.asString()) return "[" + jvmPrimitive.desc
            }

            if (simpleName == KotlinBuiltIns.FQ_NAMES.unit.shortName().asString()) return "V"
        }

        val javaClassId = JavaToKotlinClassMap.mapKotlinToJava(classId.asSingleFqName().toUnsafe())
        if (javaClassId != null) {
            return "L" + javaClassId.asString().replace('.', '$') + ";"
        }

        return "L$internalName;"
    }
}
