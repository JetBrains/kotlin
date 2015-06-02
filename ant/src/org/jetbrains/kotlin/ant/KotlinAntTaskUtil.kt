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

package org.jetbrains.kotlin.ant

import org.apache.tools.ant.AntClassLoader
import org.jetbrains.kotlin.preloading.ClassPreloadingUtils
import java.io.File
import java.lang.ref.SoftReference
import java.net.JarURLConnection

object KotlinAntTaskUtil {
    private var classLoaderRef = SoftReference<ClassLoader?>(null)

    synchronized fun getOrCreateClassLoader(): ClassLoader {
        val cached = classLoaderRef.get()
        if (cached != null) return cached

        val myLoader = javaClass.getClassLoader()
        if (myLoader !is AntClassLoader) return myLoader

        // Find path of kotlin-ant.jar in the filesystem and find kotlin-compiler.jar in the same directory
        val resourcePath = "/" + javaClass.getName().replace('.', '/') + ".class"
        val jarConnection = javaClass.getResource(resourcePath).openConnection() as? JarURLConnection
                            ?: throw UnsupportedOperationException("Kotlin compiler Ant task should be loaded from the JAR file")
        val antTaskJarPath = File(jarConnection.getJarFileURL().toURI())

        val compilerJarPath = File(antTaskJarPath.getParent(), "kotlin-compiler.jar")
        if (!compilerJarPath.exists()) {
            throw IllegalStateException("kotlin-compiler.jar is not found in the directory of Kotlin Ant task")
        }

        val classLoader = ClassPreloadingUtils.preloadClasses(listOf(compilerJarPath), 4096, myLoader, null)
        classLoaderRef = SoftReference(classLoader)

        return classLoader
    }
}
