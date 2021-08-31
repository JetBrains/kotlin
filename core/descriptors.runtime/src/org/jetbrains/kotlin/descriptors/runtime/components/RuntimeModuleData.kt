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

package org.jetbrains.kotlin.descriptors.runtime.components

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.load.kotlin.DeserializationComponentsForJava.Companion.createModuleData
import org.jetbrains.kotlin.serialization.deserialization.DeserializationComponents

class RuntimeModuleData private constructor(
    val deserialization: DeserializationComponents,
    val packagePartScopeCache: PackagePartScopeCache
) {
    val module: ModuleDescriptor get() = deserialization.moduleDescriptor

    companion object {
        fun create(classLoader: ClassLoader): RuntimeModuleData {
            val kotlinClassFinder = ReflectKotlinClassFinder(classLoader)
            val moduleData = createModuleData(
                kotlinClassFinder = kotlinClassFinder,
                // .kotlin_builtins files should be found by the same class loader that loaded stdlib classes
                jvmBuiltInsKotlinClassFinder = ReflectKotlinClassFinder(Unit::class.java.classLoader),
                javaClassFinder = ReflectJavaClassFinder(classLoader),
                moduleName = "runtime module for $classLoader",
                errorReporter = RuntimeErrorReporter,
                javaSourceElementFactory = RuntimeSourceElementFactory
            )
            return RuntimeModuleData(
                moduleData.deserializationComponentsForJava.components,
                PackagePartScopeCache(moduleData.deserializedDescriptorResolver, kotlinClassFinder)
            )
        }
    }
}
