/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.modules

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.local.CoreLocalFileSystem
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.VfsClassFileHandle
import org.jetbrains.kotlin.jvm.environment.JvmClasspathRootId
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileHandle
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes

/**
 * The default [JavaModuleResolver.checkAccessibility] overload for a [BinaryClassFileHandle], used by resolvers which know their modules
 * by virtual files only.
 */
class JavaModuleResolverHandleOverloadTest {

    private class VirtualFileOnlyResolver : JavaModuleResolver {
        val checkedFiles = mutableListOf<VirtualFile>()

        override fun checkAccessibility(
            fileFromOurModule: VirtualFile?, referencedFile: VirtualFile, referencedPackage: FqName?,
        ): JavaModuleResolver.AccessError {
            checkedFiles += referencedFile
            return JavaModuleResolver.AccessError.ModuleDoesNotReadUnnamedModule
        }

        override fun getAnnotationsForModuleOwnerOfClass(classId: ClassId): List<JavaAnnotation>? = null
    }

    @Test
    fun testVirtualFileHandleIsCheckedByItsFile(@TempDir tempDir: Path) {
        val root = tempDir.resolve("out").createDirectories()
        root.resolve("A.class").writeBytes(ByteArray(0))
        val fileSystem = CoreLocalFileSystem()
        val rootFile = checkNotNull(fileSystem.findFileByNioFile(root))
        val classFile = checkNotNull(rootFile.findChild("A.class"))
        val resolver = VirtualFileOnlyResolver()

        val result = resolver.checkAccessibility(null, VfsClassFileHandle(classFile, JavaRoot(rootFile, JavaRoot.RootType.BINARY)), null)

        assertEquals(JavaModuleResolver.AccessError.ModuleDoesNotReadUnnamedModule, result)
        assertEquals(listOf(classFile), resolver.checkedFiles)
    }

    @Test
    fun testHandleWithoutVirtualFileFails() {
        val resolver = VirtualFileOnlyResolver()

        val exception = assertThrows<IllegalStateException> {
            resolver.checkAccessibility(null, InMemoryClassFileHandleForTests, FqName("p"))
        }

        assertTrue(InMemoryClassFileHandleForTests.pathForDiagnostics in exception.message.orEmpty(), exception.message)
        assertTrue(resolver.checkedFiles.isEmpty(), "the check is not skipped by passing something else")
    }

    private object InMemoryClassFileHandleForTests : BinaryClassFileHandle {
        override val nameWithoutExtension: String get() = "A"
        override val extension: String get() = "class"
        override val rootId: JvmClasspathRootId = JvmClasspathRootId("memory:out")
        override val rootPath: Path? get() = null
        override val pathForDiagnostics: String get() = "memory:out/p/A.class"
        override fun readBytes(): ByteArray = ByteArray(0)
        override fun sibling(nameWithExtension: String): BinaryClassFileHandle? = null
    }
}
