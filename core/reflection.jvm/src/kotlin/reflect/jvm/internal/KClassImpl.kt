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

import kotlin.reflect.*
import kotlin.jvm.internal.KotlinClass
import kotlin.jvm.internal.KotlinSyntheticClass

enum class KClassOrigin {
    BUILT_IN
    KOTLIN
    FOREIGN
}

private val KOTLIN_CLASS_ANNOTATION_CLASS = javaClass<KotlinClass>()
private val KOTLIN_SYNTHETIC_CLASS_ANNOTATION_CLASS = javaClass<KotlinSyntheticClass>()

class KClassImpl<T>(val jClass: Class<T>, isKnownToBeKotlin: Boolean = false) : KClass<T> {
    // TODO: write metadata to local classes
    private val origin: KClassOrigin =
            if (isKnownToBeKotlin ||
                jClass.isAnnotationPresent(KOTLIN_CLASS_ANNOTATION_CLASS) ||
                jClass.isAnnotationPresent(KOTLIN_SYNTHETIC_CLASS_ANNOTATION_CLASS)
            ) {
                KClassOrigin.KOTLIN
            }
            else {
                KClassOrigin.FOREIGN
                // TODO: built-in classes
            }

    fun memberProperty(name: String): KMemberProperty<T, *> =
            if (origin === KClassOrigin.KOTLIN) {
                KMemberPropertyImpl<T, Any>(name, this)
            }
            else {
                KForeignMemberProperty<T, Any>(name, this)
            }

    fun mutableMemberProperty(name: String): KMutableMemberProperty<T, *> =
            if (origin === KClassOrigin.KOTLIN) {
                KMutableMemberPropertyImpl<T, Any>(name, this)
            }
            else {
                KMutableForeignMemberProperty<T, Any>(name, this)
            }

    override fun equals(other: Any?): Boolean =
            other is KClassImpl<*> && jClass == other.jClass

    override fun hashCode(): Int =
            jClass.hashCode()

    override fun toString(): String =
            jClass.toString()
}
