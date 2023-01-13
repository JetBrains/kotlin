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
package org.jetbrains.kotlin.descriptors

enum class ClassKind(val codeRepresentation: String?) {
    CLASS( "class"),
    INTERFACE("interface"),
    ENUM_CLASS( "enum class"),
    ENUM_ENTRY( null),
    ANNOTATION_CLASS( "annotation class"),
    OBJECT("object");

    val isSingleton: Boolean
        get() = this == OBJECT || this == ENUM_ENTRY
}

inline val ClassKind.isClass: Boolean
    get() = this == ClassKind.CLASS

inline val ClassKind.isInterface: Boolean
    get() = this == ClassKind.INTERFACE

inline val ClassKind.isEnumClass: Boolean
    get() = this == ClassKind.ENUM_CLASS

inline val ClassKind.isEnumEntry: Boolean
    get() = this == ClassKind.ENUM_ENTRY

inline val ClassKind.isAnnotationClass: Boolean
    get() = this == ClassKind.ANNOTATION_CLASS

inline val ClassKind.isObject: Boolean
    get() = this == ClassKind.OBJECT
