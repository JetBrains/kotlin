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
import org.apache.tools.ant.Task
import org.jetbrains.kotlin.preloading.ClassPreloadingUtils
import org.jetbrains.kotlin.preloading.Preloader
import java.io.File
import java.lang.ref.SoftReference
import java.net.JarURLConnection

internal object KotlinAntTaskUtil {
    private var classLoaderRef = SoftReference<ClassLoader?>(null)

    private val libPath: File by lazy {
        // Find path of kotlin-ant.jar in the filesystem and find kotlin-compiler.jar in the same directory
        val resourcePath = "/" + this::class.java.name.replace('.', '/') + ".class"
        val jarConnection = this::class.java.getResource(resourcePath).openConnection() as? JarURLConnection
                            ?: throw UnsupportedOperationException("Kotlin compiler Ant task should be loaded from the JAR file")
        val antTaskJarPath = File(jarConnection.jarFileURL.toURI())

        antTaskJarPath.parentFile
    }

    val compilerJar: File by jar("kotlin-compiler.jar")
    val runtimeJar: File by jar("kotlin-stdlib.jar")
    val reflectJar: File by jar("kotlin-reflect.jar")

    private fun jar(name: String) = lazy {
        File(libPath, name).apply {
            if (!exists()) {
                throw IllegalStateException("File is not found in the directory of Kotlin Ant task: $name")
            }
        }
    }

    @Synchronized
    fun getOrCreateClassLoader(): ClassLoader {
        val cached = classLoaderRef.get()
        if (cached != null) return cached

        val myLoader = this::class.java.classLoader
        if (myLoader !is AntClassLoader) return myLoader

        val classLoader = ClassPreloadingUtils.preloadClasses(listOf(compilerJar), Preloader.DEFAULT_CLASS_NUMBER_ESTIMATE, myLoader, null)
        classLoaderRef = SoftReference(classLoader)

        return classLoader
    }
}

internal val Task.defaultModuleName: String?
    get() = owningTarget?.name ?: project?.name
