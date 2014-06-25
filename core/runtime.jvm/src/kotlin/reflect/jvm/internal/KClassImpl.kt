/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

import kotlin.reflect.jvm.KOTLIN_CLASS_ANNOTATION_CLASS

enum class KClassOrigin {
    BUILT_IN
    KOTLIN
    FOREIGN
}

class KClassImpl<out T>(
        val jClass: Class<T>
) : KClass<T> {
    val origin: KClassOrigin =
            if (K_OBJECT_CLASS.isAssignableFrom(jClass) && jClass.isAnnotationPresent(KOTLIN_CLASS_ANNOTATION_CLASS)) {
                KClassOrigin.KOTLIN
            }
            else {
                KClassOrigin.FOREIGN
                // TODO: built-in classes
            }
}
