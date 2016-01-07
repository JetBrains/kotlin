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

@file:JvmName("KClassesJvm")
package kotlin.reflect.jvm

import kotlin.reflect.KClass
import kotlin.reflect.jvm.internal.KClassImpl

/**
 * Returns the JVM name of the class represented by this [KClass] instance.
 *
 * @see [java.lang.Class.getName]
 */
val KClass<*>.jvmName: String
    get() {
        return (this as KClassImpl).jClass.name
    }
