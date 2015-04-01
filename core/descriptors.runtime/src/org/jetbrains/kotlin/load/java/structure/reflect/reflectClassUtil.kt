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

package org.jetbrains.kotlin.load.java.structure.reflect

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.lang.reflect.Array

public val Class<*>.classLoader: ClassLoader
    get() = getClassLoader() ?: ClassLoader.getSystemClassLoader()

public fun Class<*>.isEnumClassOrSpecializedEnumEntryClass(): Boolean =
        javaClass<Enum<*>>().isAssignableFrom(this)

/**
 * NOTE: does not perform a Java -> Kotlin mapping. If this is not expected, consider using KClassImpl#classId instead
 */
public val Class<*>.classId: ClassId
    get() = when {
        isPrimitive() -> throw IllegalArgumentException("Can't compute ClassId for primitive type: $this")
        isArray() -> throw IllegalArgumentException("Can't compute ClassId for array type: $this")
        getEnclosingMethod() != null, getEnclosingConstructor() != null, getSimpleName().isEmpty() -> {
            val fqName = FqName(getName())
            ClassId(fqName.parent(), FqName.topLevel(fqName.shortName()), /* local = */ true)
        }
        else -> getDeclaringClass()?.classId?.createNestedClassId(Name.identifier(getSimpleName())) ?: ClassId.topLevel(FqName(getName()))
    }

public val Class<*>.desc: String
    get() {
        if (this == Void.TYPE) return "V"
        // This is a clever exploitation of a format returned by Class.getName(): for arrays, it's almost an internal name,
        // but with '.' instead of '/'
        return createArrayType().getName().substring(1).replace('.', '/')
    }

public fun Class<*>.createArrayType(): Class<*> =
        Array.newInstance(this, 0).javaClass
