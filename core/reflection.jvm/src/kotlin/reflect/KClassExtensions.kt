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

package kotlin.reflect

import kotlin.reflect.jvm.internal.KClassImpl

/**
 * Returns all functions declared in this class and all of its superclasses.
 * If this is a Java class, it includes all non-static methods declared in the class and the superclasses,
 * as well as static methods declared in the class.
 */
public val <T> KClass<T>.functions: Collection<KFunction<*>>
    get() = (this as KClassImpl<T>)
            .getMembers(declaredOnly = false, nonExtensions = true, extensions = true)
            .filterIsInstance<KFunction<*>>()
            .toList()

/**
 * Returns all functions declared in this class.
 * If this is a Java class, it includes both non-static and static methods.
 */
public val KClass<*>.declaredFunctions: Collection<KFunction<*>>
    get() = (this as KClassImpl)
            .getMembers(declaredOnly = true, nonExtensions = true, extensions = true)
            .filterIsInstance<KFunction<*>>()
            .toList()

/**
 * Returns non-extension properties declared in this class and all of its superclasses.
 */
public val <T> KClass<T>.properties: Collection<KProperty1<T, *>>
    get() = (this as KClassImpl<T>)
            .getMembers(declaredOnly = false, nonExtensions = true, extensions = false)
            .filterIsInstance<KProperty1<T, *>>()
            .toList()

/**
 * Returns extension properties declared in this class and all of its superclasses.
 */
public val <T> KClass<T>.extensionProperties: Collection<KProperty2<T, *, *>>
    get() = (this as KClassImpl<T>)
            .getMembers(declaredOnly = false, nonExtensions = false, extensions = true)
            .filterIsInstance<KProperty2<T, *, *>>()
            .toList()

/**
 * Returns non-extension properties declared in this class.
 */
public val <T> KClass<T>.declaredProperties: Collection<KProperty1<T, *>>
    get() = (this as KClassImpl<T>)
            .getMembers(declaredOnly = true, nonExtensions = true, extensions = false)
            .filterIsInstance<KProperty1<T, *>>()
            .toList()

/**
 * Returns extension properties declared in this class.
 */
public val <T> KClass<T>.declaredExtensionProperties: Collection<KProperty2<T, *, *>>
    get() = (this as KClassImpl<T>)
            .getMembers(declaredOnly = true, nonExtensions = false, extensions = true)
            .filterIsInstance<KProperty2<T, *, *>>()
            .toList()
