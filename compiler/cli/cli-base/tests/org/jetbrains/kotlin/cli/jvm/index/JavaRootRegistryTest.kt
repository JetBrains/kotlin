/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.index

import com.intellij.openapi.vfs.local.CoreLocalFileSystem
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.jvm.environment.JvmClasspathRootId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories

class JavaRootRegistryTest {
    @Test
    fun testUnregisteredRootIsAnError() {
        val registry = JavaRootRegistry()
        val root = JavaRoot(JvmClasspathRootId("/not/registered"), JavaRoot.RootType.BINARY)

        val exception = assertThrows<IllegalStateException> { registry.virtualFile(root) }
        assertTrue("/not/registered" in exception.message.orEmpty(), exception.message)
        assertThrows<IllegalStateException> { registry.vfsEntries.createRootEntry(root) }
    }

    @Test
    fun testRootIsKeyedByIdAndFirstRegistrationWins(@TempDir tempDir: Path) {
        val directory = tempDir.resolve("classes").createDirectories()
        val file = checkNotNull(CoreLocalFileSystem().findFileByNioFile(directory))
        val registry = JavaRootRegistry()

        val root = registry.javaRoot(file, JavaRoot.RootType.BINARY)
        registry.register(LightVirtualFile("other"), root.id)

        assertEquals(JavaRoot(JvmClasspathRootId.of(directory), JavaRoot.RootType.BINARY), root)
        assertSame(file, registry.virtualFile(root))
        assertEquals(JvmRootLocation.LocalDirectory(Paths.get(file.path)), registry.location(root.id))
    }

    @Test
    fun testInMemoryRootIsOnlyReadableThroughItsVirtualFile() {
        val registry = JavaRootRegistry()
        val root = registry.javaRoot(LightVirtualFile("A.java"), JavaRoot.RootType.SOURCE, id = JvmClasspathRootId("/memory/A.java"))

        assertEquals(JvmRootLocation.VirtualOnly, registry.location(root.id))
    }
}
