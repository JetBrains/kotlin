/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test.cases.session.builder

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProvider
import org.jetbrains.kotlin.analysis.api.platform.packages.createPackageProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.standalone.fir.test.AbstractStandaloneTest
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSdkModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.test.services.StandardLibrariesPathProviderForKotlinProject
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class StandaloneBehaviorTest : AbstractStandaloneTest() {
    override val suiteName: String
        get() = "behavior"

    @Test
    fun testStubbedAnnotationArguments() {
        lateinit var sourceModule: KaSourceModule
        val session = buildStandaloneAnalysisAPISession(disposable) {
            buildKtModuleProvider {
                val stdlibModule = addModule(
                    buildKtLibraryModule {
                        addBinaryRoot(StandardLibrariesPathProviderForKotlinProject.runtimeJarForTests().toPath())
                        platform = JvmPlatforms.defaultJvmPlatform
                        libraryName = "stdlib"
                    }
                )

                platform = JvmPlatforms.defaultJvmPlatform
                sourceModule = addModule(
                    buildKtSourceModule {
                        addSourceRoot(testDataPath("stubbedAnnotationArguments"))
                        addRegularDependency(stdlibModule)
                        platform = JvmPlatforms.defaultJvmPlatform
                        moduleName = "source"
                    }
                )
            }
        }

        val ktFile = session.modulesWithFiles.getValue(sourceModule).single() as KtFile
        assert(ktFile.stub != null)

        val mainClass = ktFile.declarations.first { it is KtClass && it.name == "Main" } as KtClass
        val parameter = mainClass.primaryConstructor!!.valueParameters.first()

        analyze(parameter) {
            for (annotation in parameter.symbol.annotations) {
                for (argument in annotation.arguments) {
                    val argumentExpression = argument.expression
                    if (argumentExpression is KaAnnotationValue.ArrayValue) {
                        assertEquals(3, argumentExpression.values.size)
                        return
                    }
                }
            }
        }

        error("Annotation argument wasn't found")
    }

    @Test
    fun testNestedTypeAliasIndexing() {
        lateinit var sourceModule: KaSourceModule
        val session = buildStandaloneAnalysisAPISession(disposable) {
            buildKtModuleProvider {
                val stdlibModule = addModule(
                    buildKtLibraryModule {
                        addBinaryRoot(StandardLibrariesPathProviderForKotlinProject.runtimeJarForTests().toPath())
                        platform = JvmPlatforms.defaultJvmPlatform
                        libraryName = "stdlib"
                    }
                )

                platform = JvmPlatforms.defaultJvmPlatform
                sourceModule = addModule(
                    buildKtSourceModule {
                        addSourceRoot(testDataPath("nestedTypeAlias"))
                        addRegularDependency(stdlibModule)
                        platform = JvmPlatforms.defaultJvmPlatform
                        moduleName = "source"
                    }
                )
            }
        }

        val ktFile = session.modulesWithFiles.getValue(sourceModule).single() as KtFile
        assert(ktFile.stub != null)

        val topLevelTypeAlias = ktFile.declarations
            .filterIsInstance<KtTypeAlias>()
            .first { it.name == "AliasToTopLevelClassInsideNested" }

        analyze(topLevelTypeAlias) {
            val typeAliasSymbol = topLevelTypeAlias.symbol
            val expandedType = typeAliasSymbol.expandedType

            assertIs<KaClassType>(expandedType)
            assertEquals(
                ClassId(FqName("kotlin"), Name.identifier("String")),
                expandedType.classId
            )
        }
    }

    @Test
    fun testJvmPackageProvider() {
        val sharedPlatform = JvmPlatforms.defaultJvmPlatform

        lateinit var sourceModule: KaSourceModule
        buildStandaloneAnalysisAPISession(disposable) {
            buildKtModuleProvider {
                val sdkModule = addModule(
                    buildKtSdkModule {
                        addBinaryRootsFromJdkHome(Paths.get(System.getProperty("java.home")), isJre = true)
                        addBinaryRootsFromJdkHome(Paths.get(System.getProperty("java.home")), isJre = false)
                        platform = sharedPlatform
                        libraryName = "JDK"
                    }
                )

                val stdlibModule = addModule(
                    buildKtLibraryModule {
                        addBinaryRoot(ForTestCompileRuntime.runtimeJarForTests().toPath())
                        platform = sharedPlatform
                        libraryName = "stdlib"
                    }
                )

                platform = sharedPlatform
                sourceModule = addModule(
                    buildKtSourceModule {
                        addSourceRoot(testDataPath("packageProvider"))
                        addRegularDependency(sdkModule)
                        addRegularDependency(stdlibModule)
                        platform = sharedPlatform
                        moduleName = "source"
                    }
                )
            }
        }

        testPackageProvider(sourceModule) {
            checkPackageExistence("foo", isKotlinOnly = true, isPlatform = false)
            checkPackageExistence("bar", isKotlinOnly = false, isPlatform = false)
            checkPackageExistence("kotlin", isKotlinOnly = false, isPlatform = true)
            checkPackageExistence("kotlin.collections", isKotlinOnly = false, isPlatform = true)
            checkPackageExistence("kotlin.jvm.functions", isKotlinOnly = false, isPlatform = true)
            checkPackageExistence("java.lang", isKotlinOnly = false, isPlatform = true)
            checkPackageExistence("java.io", isKotlinOnly = false, isPlatform = true)

            checkSubpackages("foo", emptyList())
            checkSubpackages("bar", emptyList())
            checkSubpackages("kotlin", listOf("collections", "jvm", "js"))
            checkSubpackages("kotlin.collections", listOf("unsigned", "jdk8"))
            checkSubpackages("java", listOf("lang", "io"))
        }
    }

    @Test
    fun testJsPackageProvider() {
        val sharedPlatform = JsPlatforms.defaultJsPlatform

        lateinit var sourceModule: KaSourceModule
        buildStandaloneAnalysisAPISession(disposable) {
            buildKtModuleProvider {
                val stdlibModule = addModule(
                    buildKtLibraryModule {
                        addBinaryRoot(ForTestCompileRuntime.stdlibJsForTests().toPath())
                        platform = sharedPlatform
                        libraryName = "stdlib"
                    }
                )

                platform = sharedPlatform
                sourceModule = addModule(
                    buildKtSourceModule {
                        addSourceRoot(testDataPath("packageProvider"))
                        addRegularDependency(stdlibModule)
                        platform = sharedPlatform
                        moduleName = "source"
                    }
                )
            }
        }

        testPackageProvider(sourceModule) {
            checkPackageExistence("foo", isKotlinOnly = true, isPlatform = false)
            checkPackageExistence("bar", isKotlinOnly = false, isPlatform = false)
            checkPackageExistence("kotlin", isKotlinOnly = true, isPlatform = false)
            checkPackageExistence("kotlin.collections", isKotlinOnly = true, isPlatform = false)
            checkPackageExistence("kotlin.jvm.functions", isKotlinOnly = false, isPlatform = false)
            checkPackageExistence("java.lang", isKotlinOnly = false, isPlatform = false)
            checkPackageExistence("java.io", isKotlinOnly = false, isPlatform = false)

            checkSubpackages("foo", emptyList())
            checkSubpackages("bar", emptyList())
            checkSubpackages("kotlin", listOf("collections", "jvm", "js"))
        }
    }

    private class PackageProviderTestContext(
        private val session: KaSession,
        private val packageProvider: KotlinPackageProvider,
        private val targetPlatform: TargetPlatform,
    ) {
        fun checkPackageExistence(name: String, isKotlinOnly: Boolean, isPlatform: Boolean) {
            fun check(expected: Boolean, message: String, block: () -> Boolean) {
                if (expected) {
                    assertTrue(block(), message)
                } else {
                    assertFalse(block(), message.replace("must", "must not"))
                }
            }

            val packageFqName = FqName(name)
            check(isKotlinOnly || isPlatform, "Package '$packageFqName' must exist") {
                packageProvider.doesPackageExist(packageFqName, targetPlatform)
            }
            check(isKotlinOnly || isPlatform, "Package '$packageFqName' must be visible through 'KaSession.findPackage()'") {
                with(session) { findPackage(packageFqName) != null }
            }
            check(isKotlinOnly, "Kotlin-only package '$packageFqName' must exist") {
                packageProvider.doesKotlinOnlyPackageExist(packageFqName)
            }
            check(isPlatform, "Platform-specific package '$packageFqName' must exist") {
                packageProvider.doesPlatformSpecificPackageExist(packageFqName, targetPlatform)
            }
        }

        fun checkSubpackages(name: String, expectedInside: List<String>) {
            val packageFqName = FqName(name)
            val actualSubpackages = packageProvider.getSubpackageNames(packageFqName, targetPlatform)
                .mapTo(HashSet()) { it.asString() }

            if (expectedInside.isEmpty()) {
                assertEquals(emptySet(), actualSubpackages, "Subpackages of '$packageFqName' must be empty")
            } else {
                for (expectedSubpackage in expectedInside) {
                    val expectedSubpackage = expectedSubpackage
                    val isInside = expectedSubpackage in actualSubpackages
                    assertTrue(isInside, "Subpackage '$name.$expectedSubpackage' must exist")
                }
            }
        }
    }

    private fun testPackageProvider(module: KaModule, block: context(KaSession) PackageProviderTestContext.() -> Unit) {
        val targetPlatform = module.targetPlatform

        analyze(module) {
            val packageProvider = module.project.createPackageProvider(analysisScope)
            val packageProviderTestContext = PackageProviderTestContext(useSiteSession, packageProvider, targetPlatform)
            block(packageProviderTestContext)
        }
    }
}