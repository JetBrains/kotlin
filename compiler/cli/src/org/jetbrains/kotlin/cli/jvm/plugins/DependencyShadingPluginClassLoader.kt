/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.plugins

import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.commons.ClassRemapper
import org.jetbrains.org.objectweb.asm.commons.Remapper
import java.io.IOException
import java.net.URL
import java.net.URLClassLoader
import java.security.AccessControlContext
import java.security.AccessController
import java.security.PrivilegedActionException
import java.security.PrivilegedExceptionAction

/**
 * DependencyShadingPluginClassLoader will load a plugin jar, applying the necessary shading as required
 * by the variant of the Kotlin Compiler which is consuming the plugin.
 */
class DependencyShadingPluginClassLoader(urls: Array<URL>, parent: ClassLoader) : URLClassLoader(urls, parent) {

    val accessControlContext: AccessControlContext
    val bytecodeLoader = URLClassLoader(urls, null)

    init {
        accessControlContext = AccessController.getContext()
    }

    // True if the Kotlin Compiler loading this plugin is shaded, and thus the plugin jar should also be shaded
    val shouldBeShaded = com.intellij.mock.MockProject::class.java.name.startsWith("org.jetbrains.kotlin.")

    // The string constant "com/intellij/", calculated in such a way that the compiler can't figure out the value
    // of the constant string, because otherwise it will get rewritten due to the way shading occurs is performed.
    val unshadedInternalPackageName = "com/intellij/"//"c${'n'+listOf(1).size}m/intellij/"

    @Throws(ClassNotFoundException::class)
    override fun findClass(name: String): Class<*>? {
        return try {
            AccessController.doPrivileged(
                PrivilegedExceptionAction {
                    val path = name.replace('.', '/') + ".class"
                    val originalBytes = bytecodeLoader.getResourceAsStream(path)?.readBytes()

                    if (originalBytes == null) throw ClassNotFoundException(name)

                    if (!shouldBeShaded)
                        return@PrivilegedExceptionAction defineClass(name, originalBytes, 0, originalBytes.size)

                    try {
                        val cw = ClassWriter(ClassWriter.COMPUTE_MAXS)

                        val shader = ClassRemapper(cw, object : Remapper() {
                            override fun map(internalName: String): String {
                                if (internalName.startsWith(unshadedInternalPackageName)) {
                                    return "org/jetbrains/kotlin/" + internalName
                                }
                                return internalName
                            }
                        })

                        ClassReader(originalBytes).accept(shader, ClassReader.EXPAND_FRAMES)

                        val shadedBytes = cw.toByteArray()
                        defineClass(name, shadedBytes, 0, shadedBytes.size)
                    } catch (e: IOException) {
                        throw ClassNotFoundException(name, e)
                    }
                }, accessControlContext
            )
        } catch (e: PrivilegedActionException) {
            throw e.exception as? ClassNotFoundException ?: ClassNotFoundException(name, e)
        }
    }
}
