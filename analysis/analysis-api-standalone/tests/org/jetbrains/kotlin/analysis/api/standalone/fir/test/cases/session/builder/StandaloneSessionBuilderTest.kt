/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


package org.jetbrains.kotlin.analysis.api.standalone.fir.test.cases.session.builder

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.resolution.KaSuccessCallInfo
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaLocalVariableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.StandaloneProjectFactory
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSdkModule
import org.jetbrains.kotlin.analysis.project.structure.builder.buildKtSourceModule
import org.jetbrains.kotlin.analysis.test.framework.TestWithDisposable
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.file.Paths
import kotlin.test.assertEquals

@OptIn(KaExperimentalApi::class)
class StandaloneSessionBuilderTest : TestWithDisposable() {
    @Test
    fun testJdkSessionBuilder() {
        lateinit var sourceModule: KaSourceModule
        val session = buildStandaloneAnalysisAPISession(disposable) {
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
                sourceModule = addModule(
                    buildKtSourceModule {
                        addSourceRoot(testDataPath("jdkClassUsage"))
                        addRegularDependency(sdk)
                        platform = JvmPlatforms.defaultJvmPlatform
                        moduleName = "source"
                    }
                )
            }
        }
        val ktFile = session.modulesWithFiles.getValue(sourceModule).single() as KtFile
        analyze(ktFile) {
            val ktCallExpression = ktFile.findDescendantOfType<KtCallExpression>()!!
            val ktCallInfo = ktCallExpression.resolveToCall()
            Assertions.assertInstanceOf(KaSuccessCallInfo::class.java, ktCallInfo); ktCallInfo as KaSuccessCallInfo
            val symbol = ktCallInfo.successfulFunctionCallOrNull()?.symbol
            Assertions.assertInstanceOf(KaConstructorSymbol::class.java, symbol); symbol as KaConstructorSymbol
            Assertions.assertEquals(ClassId.topLevel(FqName("java.lang.Thread")), symbol.containingClassId)
        }
    }

    @Test
    fun testJvmInlineOnCommon() {
        // Example from https://youtrack.jetbrains.com/issue/KT-55085
        val root = "jvmInlineOnCommon"
        lateinit var sourceModule: KaSourceModule
        val session = buildStandaloneAnalysisAPISession(disposable) {
            buildKtModuleProvider {
                platform = CommonPlatforms.defaultCommonPlatform
                val stdlib = addModule(
                    buildKtLibraryModule {
                        addBinaryRoot(Paths.get("dist/common/kotlin-stdlib-common.jar"))
                        platform = CommonPlatforms.defaultCommonPlatform
                        libraryName = "stdlib"
                    }
                )
                val commonModule = addModule(
                    buildKtSourceModule {
                        addSourceRoot(testDataPath(root).resolve("commonMain"))
                        addRegularDependency(stdlib)
                        platform = CommonPlatforms.defaultCommonPlatform
                        moduleName = "common"
                    }
                )
                sourceModule = addModule(
                    buildKtSourceModule {
                        addSourceRoot(testDataPath(root).resolve("androidMain"))
                        addDependsOnDependency(commonModule)
                        platform = JvmPlatforms.defaultJvmPlatform
                        moduleName = "android"
                    }
                )
            }
        }
        val ktFile = session.modulesWithFiles.getValue(sourceModule).single() as KtFile

        // Test dependsOn dependency: expect in the common module, actual "typealias" in the platform-specific module
        val testFunction = ktFile.findDescendantOfType<KtFunction>()!!
        val localVariable = testFunction.findDescendantOfType<KtProperty>()!!
        analyze(localVariable) {
            val localVariableSymbol = localVariable.symbol
            val type = localVariableSymbol.returnType as KaClassType
            Assertions.assertEquals(
                ClassId(FqName("test.pkg"), FqName("NativePointerKeyboardModifiers"), isLocal = false),
                type.classId
            )
            // expanded to `actual` `typealias`
            val expandedType = type.fullyExpandedType
            Assertions.assertTrue(expandedType.isIntType)
        }

        // Test stdlib-common: @JvmInline in the common module
        val actualClass = ktFile.findDescendantOfType<KtClassOrObject>()!!
        val actualProperty = actualClass.findDescendantOfType<KtProperty>()!!
        analyze(actualProperty) {
            val symbol = actualProperty.symbol
            val type = symbol.returnType as KaClassType
            Assertions.assertEquals(
                ClassId.fromString("kotlin/jvm/JvmInline"),
                type.symbol.annotations.single().classId
            )
        }
    }

    @Test
    fun testResolveAgainstCommonKlib() {
        val root = "resolveAgainstCommonKLib"
        lateinit var sourceModule: KaSourceModule
        val session = buildStandaloneAnalysisAPISession(disposable) {
            buildKtModuleProvider {
                platform = CommonPlatforms.defaultCommonPlatform
                val kLib = addModule(
                    buildKtLibraryModule {
                        val compiledKLibRoot = compileCommonKlib(testDataPath(root).resolve("klibSrc"))
                        addBinaryRoot(compiledKLibRoot)
                        platform = CommonPlatforms.defaultCommonPlatform
                        libraryName = "klib"
                    }
                )
                sourceModule = addModule(
                    buildKtSourceModule {
                        addSourceRoot(testDataPath(root).resolve("src"))
                        addRegularDependency(kLib)
                        platform = CommonPlatforms.defaultCommonPlatform
                        moduleName = "source"
                    }
                )
            }
        }
        val ktFile = session.modulesWithFiles.getValue(sourceModule).single() as KtFile

        val ktCallExpression = ktFile.findDescendantOfType<KtCallExpression>()!!
        ktCallExpression.assertIsCallOf(CallableId(FqName("commonKLib"), Name.identifier("commonKLibFunction")))
    }

    @Test
    fun testResolveAgainstCommonKlibFromOtherModule() {
        val root = "resolveAgainstCommonKLibFromOtherModule"
        lateinit var commonModule: KaSourceModule
        lateinit var sourceModule: KaSourceModule
        val session = buildStandaloneAnalysisAPISession(disposable) {
            buildKtModuleProvider {
                platform = CommonPlatforms.defaultCommonPlatform
                val kLib = addModule(
                    buildKtLibraryModule {
                        val compiledKLibRoot = compileCommonKlib(testDataPath(root).resolve("klibSrc"))
                        addBinaryRoot(compiledKLibRoot)
                        platform = CommonPlatforms.defaultCommonPlatform
                        libraryName = "klib"
                    }
                )
                commonModule = addModule(
                    buildKtSourceModule {
                        addSourceRoot(testDataPath(root).resolve("commonMain"))
                        addRegularDependency(kLib)
                        platform = CommonPlatforms.defaultCommonPlatform
                        moduleName = "common"
                    }
                )
                sourceModule = addModule(
                    buildKtSourceModule {
                        addSourceRoot(testDataPath(root).resolve("jvmMain"))
                        addRegularDependency(kLib)
                        addRegularDependency(commonModule)
                        platform = JvmPlatforms.defaultJvmPlatform
                        moduleName = "app"
                    }
                )
            }
        }

        val ktFileInCommon = session.modulesWithFiles.getValue(commonModule).single() as KtFile
        val callInCommon = ktFileInCommon.findDescendantOfType<KtCallExpression>()!!
        callInCommon.assertIsCallOf(CallableId(FqName("some.example"), FqName("Person"), Name.identifier("greet")))

        val ktFileInJvm = session.modulesWithFiles.getValue(sourceModule).single() as KtFile
        val callInJvm = ktFileInJvm.findDescendantOfType<KtCallExpression>()!!
        callInJvm.assertIsCallOf(CallableId(FqName("common"), Name.identifier("greetEachOther")))
    }

    @Test
    fun testKotlinSourceModuleSessionBuilder() {
        val root = "otherModuleUsage"
        lateinit var sourceModule: KaSourceModule
        val session = buildStandaloneAnalysisAPISession(disposable) {
            buildKtModuleProvider {
                platform = JvmPlatforms.defaultJvmPlatform
                val main = addModule(
                    buildKtSourceModule {
                        addSourceRoot(testDataPath(root).resolve("dependent"))
                        platform = JvmPlatforms.defaultJvmPlatform
                        moduleName = "dependent"
                    }
                )
                sourceModule = addModule(
                    buildKtSourceModule {
                        addSourceRoot(testDataPath(root).resolve("main"))
                        addRegularDependency(main)
                        platform = JvmPlatforms.defaultJvmPlatform
                        moduleName = "main"
                    }
                )
            }
        }
        val ktFile = session.modulesWithFiles.getValue(sourceModule).single() as KtFile
        val ktCallExpression = ktFile.findDescendantOfType<KtCallExpression>()!!
        ktCallExpression.assertIsCallOf(CallableId(FqName.ROOT, Name.identifier("foo")))

        assertEquals("main", sourceModule.name)
        assertEquals(sourceModule.name, sourceModule.stableModuleName)
    }

    @Test
    fun testKotlinSourceModuleSessionWithVirtualFile() {
        val root = "otherModuleUsage"
        lateinit var sourceModule: KaSourceModule
        val session = buildStandaloneAnalysisAPISession(disposable) {
            buildKtModuleProvider {
                platform = JvmPlatforms.defaultJvmPlatform
                val dep = addModule(
                    buildKtSourceModule {
                        addSourceRoot(testDataPath(root).resolve("dependent"))
                        platform = JvmPlatforms.defaultJvmPlatform
                        moduleName = "dependent"
                    }
                )
                sourceModule = addModule(
                    buildKtSourceModule {
                        // addSourceRoot(testDataPath(root).resolve("main"))
                        // Instead, add [VirtualFile] on-the-fly
                        val virtualFile = createDumbVirtualFile(
                            project,
                            "test.kt",
                            """
                                fun main() {
                                    foo()
                                }
                            """.trimIndent()
                        )
                        addSourceVirtualFile(virtualFile)
                        addRegularDependency(dep)
                        platform = JvmPlatforms.defaultJvmPlatform
                        moduleName = "main"
                    }
                )
            }
        }
        checkOtherModuleUsage(session, sourceModule)
    }

    @Test
    fun testKotlinBinaryModuleSessionWithVirtualFile() {
        val root = "otherModuleUsage"
        lateinit var sourceModule: KaSourceModule
        val session = buildStandaloneAnalysisAPISession(disposable) {
            buildKtModuleProvider {
                platform = JvmPlatforms.defaultJvmPlatform
                val compiledJar = compileToJar(testDataPath(root).resolve("dependent"))
                val dep = addModule(
                    buildKtLibraryModule {
                        // addBinaryRoot(compiledJar)
                        // Instead, add [VirtualFile]
                        val virtualFiles =
                            StandaloneProjectFactory.getVirtualFilesForLibraryRoots(listOf(compiledJar), kotlinCoreProjectEnvironment)
                        addBinaryVirtualFiles(virtualFiles)
                        platform = JvmPlatforms.defaultJvmPlatform
                        libraryName = "dependent"
                    }
                )
                sourceModule = addModule(
                    buildKtSourceModule {
                        addSourceRoot(testDataPath(root).resolve("main"))
                        addRegularDependency(dep)
                        platform = JvmPlatforms.defaultJvmPlatform
                        moduleName = "main"
                    }
                )
            }
        }
        checkOtherModuleUsage(session, sourceModule)
    }

    @Test
    fun testKotlinSourceAndBinaryModuleSessionWithVirtualFile() {
        val root = "otherModuleUsage"
        lateinit var sourceModule: KaSourceModule
        val session = buildStandaloneAnalysisAPISession(disposable) {
            buildKtModuleProvider {
                platform = JvmPlatforms.defaultJvmPlatform
                val compiledJar = compileToJar(testDataPath(root).resolve("dependent"))
                val dep = addModule(
                    buildKtLibraryModule {
                        // addBinaryRoot(compiledJar)
                        // Instead, add [VirtualFile]
                        val virtualFiles =
                            StandaloneProjectFactory.getVirtualFilesForLibraryRoots(listOf(compiledJar), kotlinCoreProjectEnvironment)
                        addBinaryVirtualFiles(virtualFiles)
                        platform = JvmPlatforms.defaultJvmPlatform
                        libraryName = "dependent"
                    }
                )
                sourceModule = addModule(
                    buildKtSourceModule {
                        // addSourceRoot(testDataPath(root).resolve("main"))
                        // Instead, add [VirtualFile] on-the-fly
                        val virtualFile = createDumbVirtualFile(
                            project,
                            "test.kt",
                            """
                                fun main() {
                                    foo()
                                }
                            """.trimIndent()
                        )
                        addSourceVirtualFile(virtualFile)
                        addRegularDependency(dep)
                        platform = JvmPlatforms.defaultJvmPlatform
                        moduleName = "main"
                    }
                )
            }
        }
        checkOtherModuleUsage(session, sourceModule)
    }

    private fun checkOtherModuleUsage(
        session: StandaloneAnalysisAPISession,
        sourceModule: KaSourceModule,
    ) {
        val ktFile = session.modulesWithFiles.getValue(sourceModule).single() as KtFile
        val ktCallExpression = ktFile.findDescendantOfType<KtCallExpression>()!!
        ktCallExpression.assertIsCallOf(CallableId(FqName.ROOT, Name.identifier("foo")))
    }

    @Test
    fun testCodeFragment() {
        val root = "codeFragment"

        lateinit var contextModule: KaSourceModule
        val session = buildStandaloneAnalysisAPISession(disposable) {
            buildKtModuleProvider {
                platform = JvmPlatforms.defaultJvmPlatform
                contextModule = addModule(
                    buildKtSourceModule {
                        addSourceRoot(testDataPath(root))
                        platform = JvmPlatforms.defaultJvmPlatform
                        moduleName = "context"
                    }
                )
            }
        }

        val contextFile = session.modulesWithFiles.getValue(contextModule).single() as KtFile
        val contextElement = contextFile.findDescendantOfType<KtVariableDeclaration> { it.name == "y" }!!

        val project = contextFile.project
        val codeFragment = KtExpressionCodeFragment(project, "fragment.kt", "x - 1", imports = null, contextElement)

        val codeFragmentModule = KotlinProjectStructureProvider.getModule(project, codeFragment, useSiteModule = contextModule)
        requireIsInstance<KaDanglingFileModule>(codeFragmentModule)
        assertEquals(codeFragmentModule.contextModule, contextModule)

        analyze(codeFragment) {
            val fileSymbol = codeFragment.symbol
            assertEquals(fileSymbol.containingModule, codeFragmentModule)

            val referenceExpression = codeFragment.findDescendantOfType<KtSimpleNameExpression> { it.text == "x" }!!
            val variableSymbol = referenceExpression.mainReference.resolveToSymbol()
            assert(variableSymbol is KaLocalVariableSymbol)
        }
    }

    @Test
    fun testNonPhysicalFile() {
        val root = "nonPhysicalFile"

        lateinit var contextModule: KaSourceModule
        val session = buildStandaloneAnalysisAPISession(disposable) {
            buildKtModuleProvider {
                platform = JvmPlatforms.defaultJvmPlatform
                contextModule = addModule(
                    buildKtSourceModule {
                        addSourceRoot(testDataPath(root))
                        platform = JvmPlatforms.defaultJvmPlatform
                        moduleName = "context"
                    }
                )
            }
        }

        val contextFile = session.modulesWithFiles.getValue(contextModule).single() as KtFile

        val project = contextFile.project

        val dummyFile = KtPsiFactory
            .contextual(contextFile, markGenerated = false, eventSystemEnabled = false)
            .createFile("dummy.kt", "fun usage() { test() }")

        assert(dummyFile.virtualFile == null)

        val dummyModule = KotlinProjectStructureProvider.getModule(project, dummyFile, useSiteModule = null)
        requireIsInstance<KaDanglingFileModule>(dummyModule)
        assertEquals(dummyModule.contextModule, contextModule)

        analyze(dummyFile) {
            val fileSymbol = dummyFile.symbol
            assertEquals(fileSymbol.containingModule, dummyModule)

            val callExpression = dummyFile.findDescendantOfType<KtCallExpression>()!!
            val call = callExpression.resolveToCall()?.successfulFunctionCallOrNull() ?: error("Call inside a dummy file is unresolved")
            assert(call.symbol is KaNamedFunctionSymbol)
        }
    }
}