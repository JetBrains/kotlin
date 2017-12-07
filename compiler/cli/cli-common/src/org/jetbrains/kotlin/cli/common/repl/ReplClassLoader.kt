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

package org.jetbrains.kotlin.cli.common.repl

import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.util.TraceClassVisitor
import java.io.ByteArrayInputStream
import java.io.File

import java.io.PrintWriter
import java.net.URL
import java.net.URLClassLoader
import java.net.URLConnection
import java.net.URLStreamHandler
import java.util.*

class ReplClassLoader(
        files: List<File>,
        parent: ClassLoader?
) : URLClassLoader(files.map { it.toURI().toURL() }.toTypedArray(), parent) {
    private val classes = LinkedHashMap<JvmClassName, ByteArray>()

    fun addJar(file: File) {
        super.addURL(file.toURI().toURL())
    }

    override fun findClass(name: String): Class<*> {
        val classBytes = classes[JvmClassName.byFqNameWithoutInnerClasses(name)]

        return if (classBytes != null) {
            defineClass(name, classBytes, 0, classBytes.size)
        }
        else {
            super.findClass(name)
        }
    }

    override fun findResource(name: String): URL {
        val jvmName = JvmClassName.byFqNameWithoutInnerClasses(name)
        classes[jvmName]?.let { return wrapToUrl(jvmName.internalName + ".class", it) }

        return super.findResource(name)
    }

    override fun findResources(name: String): Enumeration<URL> {
        val result = mutableListOf<URL>()

        // Add our custom resource
        val jvmName = JvmClassName.byFqNameWithoutInnerClasses(name)
        classes[jvmName]?.let { wrapToUrl(jvmName.internalName + ".class", it) }?.let { result += it }

        // Add other resources
        super.findResources(name).asSequence().forEach { result += it }

        return Collections.enumeration(result)
    }

    private fun wrapToUrl(path: String, bytes: ByteArray): URL {
        return URL("repl", null, 0, path, object : URLStreamHandler() {
            override fun openConnection(u: URL) = object : URLConnection(u) {
                override fun connect() {}
                override fun getInputStream() = ByteArrayInputStream(bytes)
            }
        })
    }

    fun addClass(className: JvmClassName, bytes: ByteArray) {
        classes.put(className, bytes)?.let {
            throw IllegalStateException("Rewrite at key " + className)
        }
    }

    fun dumpClasses(writer: PrintWriter) {
        for (classBytes in classes.values) {
            ClassReader(classBytes).accept(TraceClassVisitor(writer), 0)
        }
    }

}