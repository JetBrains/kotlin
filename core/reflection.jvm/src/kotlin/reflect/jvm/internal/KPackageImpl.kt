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

package kotlin.reflect.jvm.internal

import kotlin.reflect.KPackage
import kotlin.jvm.internal.KotlinPackage

private val KOTLIN_PACKAGE_ANNOTATION_CLASS = javaClass<KotlinPackage>()

class KPackageImpl(val jClass: Class<*>) : KPackage {
    override fun equals(other: Any?): Boolean =
            other is KPackageImpl && jClass == other.jClass

    override fun hashCode(): Int =
            jClass.hashCode()

    override fun toString(): String {
        val name = jClass.getName()
        return "package " + if (jClass.isAnnotationPresent(KOTLIN_PACKAGE_ANNOTATION_CLASS)) {
            if (name == "_DefaultPackage") "<default>" else name.substringBeforeLast(".")
        }
        else name
    }
}
