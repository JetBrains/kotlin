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

@file:JvmName("KAnnotatedElements")
package kotlin.reflect

/**
 * Returns an annotation of the given type on this element.
 */
@Deprecated("Use 'findAnnotation' from kotlin.reflect.full package", ReplaceWith("this.findAnnotation<T>()", "kotlin.reflect.full.findAnnotation"), level = DeprecationLevel.ERROR)
@SinceKotlin("1.1")
inline fun <reified T : Annotation> KAnnotatedElement.findAnnotation(): T? =
        @Suppress("UNCHECKED_CAST")
        annotations.firstOrNull { it is T } as T?
