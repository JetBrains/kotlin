/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.mockJDK

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.Opcodes
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

fun main() = removeInterfacesFromMockJdkClassfiles(
    mockJdkRuntimeJar = File("compiler/testData/mockJDK/jre/lib/rt.jar")
)

internal fun removeInterfacesFromMockJdkClassfiles(mockJdkRuntimeJar: File) {
    if (!mockJdkRuntimeJar.exists()) {
        throw AssertionError("$mockJdkRuntimeJar doesn't exist")
    }

    val tmpdir = FileUtil.createTempDirectory(
        File(System.getProperty("java.io.tmpdir")),
        "mockJdk",
        "",
        true
    )
    val copyJar = File(tmpdir, "rt.jar")
    FileUtil.copy(mockJdkRuntimeJar, copyJar)

    JarFile(mockJdkRuntimeJar).use { sourceJar ->
        JarOutputStream(FileOutputStream(copyJar))
            .use { targetJar ->
                transformJar(sourceJar, targetJar)
            }
    }

    FileUtil.copy(copyJar, mockJdkRuntimeJar)
    tmpdir.deleteRecursively()
}

private fun transformJar(sourceJar: JarFile, targetJar: JarOutputStream) {
    val sourceEntries = sourceJar.entries().toList()
    for (entry in sourceEntries) {
        if (entry.name.endsWith(".class")) {
            val inputByteArray = sourceJar.getInputStream(entry).use { it.readBytes() }
            val classReader = ClassReader(inputByteArray)
            val classWriter = ClassWriter(classReader, 0) // Neither stack frames nor stack size changes

            classReader.accept(
                InterfacesFilter(classWriter),
                ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES // Only implemented interfaces matter
            )

            targetJar.putNextEntry(ZipEntry(entry.name))
            targetJar.write(classWriter.toByteArray())

        } else {
            targetJar.putNextEntry(ZipEntry(entry.name))
            FileUtil.copy(sourceJar.getInputStream(entry), targetJar)
        }
    }
}

internal class InterfacesFilter(classVisitor: ClassVisitor) : ClassVisitor(Opcodes.API_VERSION, classVisitor) {
    private val mockJdkEntries =
        GenerateMockJdk.getClassFileEntries().mapNotNull { entry ->
            entry.substringBeforeLast(".class").takeUnless(String::isEmpty)
        }.toSet()

    override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String?, interfaces: Array<out String>?) {
        if (name !in mockJdkEntries) {
            super.visit(version, access, name, signature, superName, interfaces)
            return
        }

        val allowedInterfaces = filterInterfaces(interfaces)
        val allowedSuperclass = filterSuperclass(superName)
        super.visit(version, access, name, signature, allowedSuperclass, allowedInterfaces)
    }

    private fun filterSuperclass(oldSuperclass: String?): String? =
        oldSuperclass?.let {
            if (it !in mockJdkEntries)
                "java/lang/Object"
            else it
        } ?: oldSuperclass

    private fun filterInterfaces(oldInterfaces: Array<out String>?): Array<out String>? =
        oldInterfaces
            ?.filter { it in mockJdkEntries }
            ?.toTypedArray()
}
