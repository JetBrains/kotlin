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
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
package kotlin.reflect

/**
 * Returns an annotation of the given type on this element.
 */
@kotlin.internal.LowPriorityInOverloadResolution
@Deprecated("Use 'findAnnotation' from kotlin.reflect.full package", ReplaceWith("this.findAnnotation<T>()", "kotlin.reflect.full.findAnnotation"), level = DeprecationLevel.WARNING)
@Suppress("UNCHECKED_CAST")
inline fun <reified T : Annotation> KAnnotatedElement.findAnnotation(): T? =
        annotations.firstOrNull { it is T } as T?
