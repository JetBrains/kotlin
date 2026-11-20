/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm

import org.jetbrains.kotlin.cli.jvm.compiler.ClasspathRootsResolver
import org.jetbrains.kotlin.cli.jvm.config.JvmModulePathRoot
import org.jetbrains.kotlin.cli.jvm.index.JavaRootRegistry
import org.jetbrains.kotlin.cli.jvm.index.JvmRootEntryFactory
import org.jetbrains.kotlin.cli.jvm.index.nio.JarFilePool
import org.jetbrains.kotlin.cli.jvm.index.nio.NioRootEntryFactory
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleFinder
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModule
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.Opcodes
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Path

/** [ClasspathRootsResolver] reads module descriptors and manifests of module-path jars the same through VFS and NIO root entries. */
class ClasspathRootsResolverRootEntriesTest : JvmDependenciesIndexContractTestBase() {
    private val jarPool = checkNotNull(JarFilePool.createIfUnmappingPossible())

    @AfterEach
    fun closeJarPool() = jarPool.close()

    private fun moduleInfo(name: String, requires: String? = null): ByteArray {
        val writer = ClassWriter(0)
        writer.visit(Opcodes.V9, Opcodes.ACC_MODULE, "module-info", null, null, null)
        val module = writer.visitModule(name, 0, null)
        module.visitRequire("java.base", Opcodes.ACC_MANDATED, null)
        if (requires != null) module.visitRequire(requires, 0, null)
        module.visitEnd()
        writer.visitEnd()
        return writer.toByteArray()
    }

    private fun manifest(vararg attributes: Pair<String, String>): ByteArray =
        (listOf("Manifest-Version: 1.0") + attributes.map { "${it.first}: ${it.second}" }).joinToString("\r\n", postfix = "\r\n\r\n")
            .toByteArray()

    private fun resolveModules(jars: List<Path>, rootEntries: (JavaRootRegistry) -> JvmRootEntryFactory): List<String> {
        val configuration = KotlinTestUtils.newConfiguration()
        val rootRegistry = JavaRootRegistry()
        val resolver = ClasspathRootsResolver(
            configuration,
            additionalModules = emptyList(),
            contentRootToVirtualFile = { (it as JvmModulePathRoot).file.toPath().asJarRootVirtualFile() },
            javaModuleFinder = CliJavaModuleFinder(jdkHome = null, configuration, javaFileManager = null, jdkRelease = 8, rootRegistry),
            requireStdlibModule = false,
            outputDirectory = null,
            findClassOnClasspath = { null },
            jdkRelease = 8,
            hasKotlinSources = true,
            rootRegistry = rootRegistry,
            rootEntries = rootEntries(rootRegistry),
        )
        return resolver.convertClasspathRoots(jars.map { JvmModulePathRoot(it.toFile()) }).modules.map { module ->
            when (module) {
                is JavaModule.Explicit -> {
                    val requires = module.moduleInfo.requires.map { it.moduleName }
                    "${module.name}: ${module.moduleInfoFile.path.substringAfter("!/")}, requires $requires"
                }
                else -> "${module.name}: automatic"
            }
        }
    }

    @Test
    fun moduleDescriptorsAndManifestsAreReadTheSameThroughBothEntries() {
        val jars = listOf(
            jar("explicit.jar", "p/A.class", content = mapOf("module-info.class" to moduleInfo("m.explicit", requires = "m.other"))),
            jar(
                "multi-release.jar", content = mapOf(
                    "META-INF/MANIFEST.MF" to manifest("Multi-Release" to "true"),
                    "META-INF/versions/11/module-info.class" to moduleInfo("m.versioned"),
                    "META-INF/versions/8/module-info.class" to moduleInfo("m.ignored"),
                )
            ),
            jar("named.jar", content = mapOf("META-INF/MANIFEST.MF" to manifest("Automatic-Module-Name" to "m.named"))),
            jar("derived-name-1.2.jar", "q/B.class"),
            jar("not-multi-release.jar", content = mapOf("META-INF/versions/11/module-info.class" to moduleInfo("m.unselected"))),
        )
        val vfs = resolveModules(jars) { it.vfsEntries }
        assertEquals(
            listOf(
                "m.explicit: module-info.class, requires [java.base, m.other]",
                "m.versioned: META-INF/versions/11/module-info.class, requires [java.base]",
                "m.named: automatic",
                "derived.name: automatic",
                "not.multi.release: automatic",
            ),
            vfs,
        )
        assertEquals(vfs, resolveModules(jars) { NioRootEntryFactory(jarPool, it) })
    }
}
