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
    const val COMPILER_JAR_NAME = "kotlin-compiler.jar"
    const val RUNTIME_JAR_NAME = "kotlin-runtime.jar"
    const val REFLECT_JAR_NAME = "kotlin-reflect.jar"

    private var classLoaderRef = SoftReference<ClassLoader?>(null)

    val libPath: File? by lazy {
        // Find path of kotlin-ant.jar in the filesystem and find kotlin-compiler.jar in the same directory
        val resourcePath = "/" + javaClass.name.replace('.', '/') + ".class"
        val jarConnection = javaClass.getResource(resourcePath).openConnection() as? JarURLConnection
        jarConnection?.let {
            File(it.jarFileURL.toURI()).parentFile
        }
    }

    val compilerJar: File? by jar(COMPILER_JAR_NAME)
    val runtimeJar: File? by jar(RUNTIME_JAR_NAME)
    val reflectJar: File? by jar(REFLECT_JAR_NAME)

    private fun jar(name: String): Lazy<File?> = lazy {
        libPath?.let {
            File(libPath, name).let {
                if (it.exists()) {
                    it
                } else {
                    null
                }
            }
        }
    }

    @Synchronized
    fun getOrCreateClassLoader(compilerFQClassName: String): ClassLoader {
        val cached = classLoaderRef.get()
        if (cached != null) return cached

        val myLoader = javaClass.classLoader
        if (myLoader !is AntClassLoader) return myLoader

        val cJar = compilerJar
        val rJar = reflectJar
        val rtJar = runtimeJar
        val jars = if (cJar === null || rJar === null || rtJar === null) {
            //We check if the runtime and reflect jars present, just because preloading of the compiler jar
            //would cause loading of the kotlin-runtime.jar and kotlin-reflect.jar as dependencies,
            //even thou their classes might have already been added to the Kotlin Ant task's classpath.
            //
            //If we haven't been able to find the needed compiler jars for whatever reason
            //(the Ant task is not in a jar, thus we do not know its parent folder,
            //or there is no jar with the predefined jar name in the parent folder),
            //the compiler still might have been loaded,
            //just because its classes are on the Ant task's classpath.
            //So, why to break here if we can run the compiler?
            //
            //We do not need to check if Kotlin runtime or preloader classes have been loaded.
            //They were loaded either from the Kotlin Ant task's classpath tag
            //or from the filesystem indirectly, we know that for sure,
            //otherwise the Kotlin Ant task wouldn't have started.
            //Ant by the way, classes of the Kotlin StdLib have already been loaded,
            //for one of the same reasons, and they are on the Kotlin Ant task's classpath,
            //while the Kotlin compiler or the code it compiles still may not need it.
            //
            //A funny thing is that Reflect is actually optional.
            //At least I was able to compile a simple code without the library.
            for (className in arrayOf(
              compilerFQClassName //kotlin-compiler.jar
              /*"kotlin.reflect.jvm.ReflectJvmMapping"*/ //kotlin-reflect.jar
            )) {
                try {
                    Class.forName(className, false, myLoader);
                } catch(e: Exception) {
                    if (libPath === null) {
                        throw IllegalStateException(
                          "\n$className cannot be loaded from the Ant task's classpath, thus we cannot load the compiler.\n" +
                          "To fix this you may add the jars to the Kotlin Ant task's classpath."
                        )
                    } else {
                        throw IllegalStateException(
                          "\n$className cannot be loaded from the Ant task's classpath, thus we cannot load the compiler.\n" +
                          "To fix this, you may either add compiler jars to the folder of the Kotlin Ant's jar: $COMPILER_JAR_NAME, $REFLECT_JAR_NAME, $RUNTIME_JAR_NAME\n" +
                          "or you may add the jars to the Kotlin Ant task' classpath."
                        )
                    }
                }
            }
            listOf() //Nothing to preload, the classes have already been loaded.
        } else {
            listOf(cJar)
        }
        val classLoader = ClassPreloadingUtils.preloadClasses(jars, Preloader.DEFAULT_CLASS_NUMBER_ESTIMATE, myLoader, null)
        classLoaderRef = SoftReference(classLoader)

        return classLoader
    }
}

internal val Task.defaultModuleName: String?
    get() = owningTarget?.name ?: project?.name
