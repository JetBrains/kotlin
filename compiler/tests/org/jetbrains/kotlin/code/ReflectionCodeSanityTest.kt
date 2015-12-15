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

package org.jetbrains.kotlin.code

import junit.framework.TestCase
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.load.java.JvmAbi
import java.lang.reflect.Field

class ReflectionCodeSanityTest : TestCase() {
    fun testNoDelegatedPropertiesInKClassAndKProperties() {
        val classLoader = ForTestCompileRuntime.runtimeAndReflectJarClassLoader()

        val classesToCheck = linkedSetOf<Class<*>>()
        fun addClassToCheck(klass: Class<*>) {
            if (classesToCheck.add(klass)) {
                @Suppress("UNNECESSARY_SAFE_CALL") // https://youtrack.jetbrains.com/issue/KT-9294
                klass.superclass?.let { addClassToCheck(it) }
            }
        }

        fun addClass(name: String) = addClassToCheck(classLoader.loadClass("kotlin.reflect.jvm.internal.$name"))

        addClass("KClassImpl")
        addClass("KMutableProperty0Impl")
        addClass("KMutableProperty1Impl")
        addClass("KMutableProperty2Impl")

        val badFields = linkedSetOf<Field>()
        for (klass in classesToCheck) {
            badFields.addAll(klass.declaredFields.filter { it.name.endsWith(JvmAbi.DELEGATED_PROPERTY_NAME_SUFFIX) })
        }

        if (badFields.isNotEmpty()) {
            fail("The fields listed below appear to be delegates for properties.\n" +
                 "It's highly not recommended to use property delegates in reflection.jvm because a KProperty instance\n" +
                 "is created for each delegated property and that makes the initialization sequence of reflection\n" +
                 "implementation classes unpredictable and leads to a deadlock or ExceptionInInitializerError.\n\n" +
                 "Please un-delegate the corresponding properties:\n\n" +
                 badFields.joinToString("\n"))
        }
    }
}
