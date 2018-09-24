/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen

import junit.framework.TestCase
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.utils.rethrow
import java.lang.reflect.Method
import java.net.URL
import java.net.URLClassLoader

fun clearReflectionCache(classLoader: ClassLoader) {
    try {
        val klass = classLoader.loadClass(JvmAbi.REFLECTION_FACTORY_IMPL.asSingleFqName().asString())
        val method = klass.getDeclaredMethod("clearCaches")
        method.invoke(null)
    }
    catch (e: ClassNotFoundException) {
        // This is OK for a test without kotlin-reflect in the dependencies
    }
}



fun ClassLoader?.extractUrls(): List<URL> {
    return (this as? URLClassLoader)?.let {
        it.urLs.toList() + it.parent.extractUrls()
    } ?: emptyList()

}