/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.kotlin.analysis.api.KaExperimentalApi::class)

package org.jetbrains.kotlin.buildtools.tests.restricted

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.scopes.combinedDeclaredMemberScope
import org.jetbrains.kotlin.analysis.api.scopes.declaredMemberScope
import org.jetbrains.kotlin.analysis.api.scopes.packageScope
import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPackageSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolVisibility
import org.jetbrains.kotlin.analysis.api.symbols.findClass
import org.jetbrains.kotlin.analysis.api.symbols.findPackage
import org.jetbrains.kotlin.analysis.api.symbols.isSubClassOf
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSdkModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Paths

private const val COMPILER_CLASSPATH_PROPERTY = "kotlin.build-tools-api.test.compilerClasspath"

class AutoCloseablesTest : BaseCompilationTest() {

    val excludedPackages = listOf(
        "org.jetbrains.kotlin.buildtools.internal.compat",
        "org.jetbrains.kotlin.buildtools.internal.scripting",
    )

    @Test
    @DisplayName("Test that all AutoCloseable methods check for already closed state")
    fun testAutoCloseables() {
        val compilerClasspath = System.getProperty(COMPILER_CLASSPATH_PROPERTY)
            ?: error("$COMPILER_CLASSPATH_PROPERTY is not set")
        val classpathEntries = compilerClasspath.split(File.pathSeparator).map { Paths.get(it) }

        val disposable = Disposer.newDisposable("AutoCloseablesTest")
        val tempDir = Files.createTempDirectory("emptySource")
        try {
            val dummyFile = tempDir.resolve("dummy.kt")
            dummyFile.toFile().writeText("package test\n")

            lateinit var sourceModule: KaSourceModule
            buildStandaloneAnalysisAPISession(disposable) {
                buildKtModuleProvider {
                    platform = JvmPlatforms.defaultJvmPlatform
                    val sdk = addModule(
                        buildKtSdkModule {
                            addBinaryRootsFromJdkHome(Paths.get(System.getProperty("java.home")), isJre = true)
                            addBinaryRootsFromJdkHome(Paths.get(System.getProperty("java.home")), isJre = false)
                            platform = JvmPlatforms.defaultJvmPlatform
                            libraryName = "JDK"
                        }
                    )
                    val libraryModule = addModule(
                        buildKtLibraryModule {
                            addBinaryRoots(classpathEntries)
                            addRegularDependency(sdk)
                            platform = JvmPlatforms.defaultJvmPlatform
                            libraryName = "compilerClasspath"
                        }
                    )
                    sourceModule = addModule(
                        buildKtSourceModule {
                            addSourceRoot(tempDir)
                            addRegularDependency(sdk)
                            addRegularDependency(libraryModule)
                            platform = JvmPlatforms.defaultJvmPlatform
                            moduleName = "source"
                        }
                    )
                }
            }

            analyze(sourceModule) {
                val currentSession = this
                val autoCloseableSymbol = findClass(ClassId.fromString("java/lang/AutoCloseable"))
                    ?: error("AutoCloseable not found")

                fun collectClasses(classSymbol: KaClassSymbol): List<KaClassSymbol> {
                    val result = mutableListOf(classSymbol)
                    with(currentSession) {
                        for (member in classSymbol.combinedDeclaredMemberScope.classifiers) {
                            if (member is KaClassSymbol) {
                                result.addAll(collectClasses(member))
                            }
                        }
                    }
                    return result
                }

                val targetPackage = FqName("org.jetbrains.kotlin.buildtools.internal")
                val rootPkg = findPackage(targetPackage)
                    ?: error("Package $targetPackage not found")

                val classesInPackage =
                    rootPkg.withAllChildPackages().filter { pkgName -> excludedPackages.none { pkgName.fqName.asString().startsWith(it) } }
                        .flatMap { pkg ->
                            pkg.packageScope.classifiers
                                .filterIsInstance<KaClassSymbol>()
                                .flatMap { collectClasses(it) }
                        }.toList()

                val autoCloseableClasses = classesInPackage.filter { it.isSubClassOf(autoCloseableSymbol) }
                assertTrue(autoCloseableClasses.isNotEmpty(), "Expected to find AutoCloseable classes in $targetPackage")

                val urlClassLoader = URLClassLoader(classpathEntries.map { it.toUri().toURL() }.toTypedArray())
                for (classSymbol in autoCloseableClasses) {
                    val classId = classSymbol.classId ?: error("Class symbol has no classId: $classSymbol")
                    val classResourceName = classId.packageFqName.asString().replace('.', '/') + "/" +
                            classId.relativeClassName.asString().replace('.', '$') + ".class"
                    val classBytes = urlClassLoader.getResourceAsStream(classResourceName)?.use { it.readBytes() }
                        ?: error("Class file $classResourceName not found in compiler classpath")

                    val classNode = ClassNode()
                    ClassReader(classBytes).accept(classNode, 0)

                    with(currentSession) {
                        val functionsToCheck = classSymbol.declaredMemberScope.declarations
                            .filterIsInstance<KaNamedFunctionSymbol>()
                            .filter { it.visibility != KaSymbolVisibility.PRIVATE && it.name.asString() != "close" }
                            .toList()

                        for (function in functionsToCheck) {
                            val functionName = function.name.asString()
                            val bytecodeMethods = classNode.methods.filter {
                                it.name == functionName && (it.access and Opcodes.ACC_SYNTHETIC) == 0 && it.access and Opcodes.ACC_PRIVATE == 0
                            }
                            assertTrue(
                                bytecodeMethods.isNotEmpty(),
                                "No bytecode method found for function $functionName in class ${classId.asString()}"
                            )

                            for (method in bytecodeMethods) {
                                assertTrue(
                                    method.callsRequireNotClosedFirst(),
                                    "Method ${classId.asString()}.${method.name} does not call CloseableGuard.requireNotClosed() first"
                                )
                            }
                        }
                    }
                }
            }
        } finally {
            Disposer.dispose(disposable)
            tempDir.toFile().deleteRecursively()
        }
    }

    private fun MethodNode.callsRequireNotClosedFirst(): Boolean {
        for (insn in instructions) {
            if (insn is MethodInsnNode) {
                if (insn.owner == "kotlin/jvm/internal/Intrinsics" &&
                    (insn.name == "checkNotNullParameter" || insn.name == "checkParameterIsNotNull")
                ) {
                    continue
                }
                return insn.owner == "org/jetbrains/kotlin/buildtools/internal/CloseableGuard" &&
                        (insn.name == "requireNotClosed" || insn.name.startsWith("access\$isClosed"))
            }
        }
        return false
    }

    context(session: KaSession)
    private fun KaPackageSymbol.withAllChildPackages(): Sequence<KaPackageSymbol> {
        return packageScope.getPackageSymbols().flatMap { pkg ->
            pkg.withAllChildPackages()
        } + this
    }
}
