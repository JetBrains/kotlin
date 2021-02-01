/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler

import com.intellij.openapi.util.io.FileUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.cli.WrongBytecodeVersionTest
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.metadata.K2MetadataCompiler
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.codegen.inline.remove
import org.jetbrains.kotlin.codegen.optimization.common.asSequence
import org.jetbrains.kotlin.codegen.optimization.common.intConstant
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.KotlinCompilerVersion.TEST_IS_PRE_RELEASE_SYSTEM_PROPERTY
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.incremental.LocalFileKotlinClass
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils.isObject
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparatorAdaptor.validateAndCompareDescriptorWithFile
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.File
import java.net.URLClassLoader
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.zip.ZipOutputStream
import kotlin.experimental.xor

class CompileKotlinAgainstCustomBinariesTest : AbstractKotlinCompilerIntegrationTest() {
    override val testDataPath: String
        get() = "compiler/testData/compileKotlinAgainstCustomBinaries/"

    private fun doTestWithTxt(vararg extraClassPath: File) {
        validateAndCompareDescriptorWithFile(
            analyzeFileToPackageView(*extraClassPath),
            AbstractLoadJavaTest.COMPARATOR_CONFIGURATION,
            getTestDataFileWithExtension("txt")
        )
    }

    private fun analyzeFileToPackageView(vararg extraClassPath: File): PackageViewDescriptor {
        val environment = createEnvironment(extraClassPath.toList())

        val ktFile = KotlinTestUtils.loadJetFile(environment.project, getTestDataFileWithExtension("kt"))
        val result = JvmResolveUtil.analyzeAndCheckForErrors(ktFile, environment)

        return result.moduleDescriptor.getPackage(LoadDescriptorUtil.TEST_PACKAGE_FQNAME).also {
            assertFalse("Failed to find package: " + LoadDescriptorUtil.TEST_PACKAGE_FQNAME, it.isEmpty())
        }
    }

    private fun createEnvironment(extraClassPath: List<File>): KotlinCoreEnvironment {
        val configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, *extraClassPath.toTypedArray())
        return KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    }

    private fun analyzeAndGetAllDescriptors(vararg extraClassPath: File): Collection<DeclarationDescriptor> =
        DescriptorUtils.getAllDescriptors(analyzeFileToPackageView(*extraClassPath).memberScope)

    private fun doTestBrokenLibrary(libraryName: String, vararg pathsToDelete: String, additionalOptions: List<String> = emptyList()) {
        // This function compiles a library, then deletes one class file and attempts to compile a Kotlin source against
        // this broken library. The expected result is an error message from the compiler
        val library = copyJarFileWithoutEntry(compileLibrary(libraryName), *pathsToDelete)
        compileKotlin("source.kt", tmpdir, listOf(library), additionalOptions = additionalOptions)
    }

    private fun doTestKotlinLibraryWithWrongMetadataVersion(
        libraryName: String,
        additionalTransformation: ((fieldName: String, value: Any?) -> Any?)?,
        vararg additionalOptions: String
    ) {
        val library = transformJar(
            compileLibrary(libraryName, additionalOptions = listOf("-Xmetadata-version=42.0.0")),
            { _, bytes ->
                WrongBytecodeVersionTest.transformMetadataInClassFile(bytes) { fieldName, value ->
                    additionalTransformation?.invoke(fieldName, value)
                }
            }
        )
        compileKotlin("source.kt", tmpdir, listOf(library), K2JVMCompiler(), additionalOptions.toList())
    }

    private fun doTestKotlinLibraryWithWrongMetadataVersionJs(libraryName: String, vararg additionalOptions: String) {
        val library = compileJsLibrary(libraryName, additionalOptions = listOf("-Xmetadata-version=42.0.0"))
        compileKotlin("source.kt", File(tmpdir, "usage.js"), listOf(library), K2JSCompiler(), additionalOptions.toList())
    }

    private fun doTestPreReleaseKotlinLibrary(
        compiler: CLICompiler<*>,
        libraryName: String,
        usageDestination: File,
        vararg additionalOptions: String
    ) {
        // Compiles the library with the "pre-release" flag, then compiles a usage of this library in the release mode

        val result = withPreRelease(true) {
            when (compiler) {
                is K2JSCompiler -> compileJsLibrary(libraryName)
                is K2JVMCompiler -> compileLibrary(libraryName)
                else -> throw UnsupportedOperationException(compiler.toString())
            }
        }

        withPreRelease(false) {
            compileKotlin("source.kt", usageDestination, listOf(result), compiler, additionalOptions.toList())
        }
    }

    private fun <T> withPreRelease(value: Boolean, block: () -> T): T =
        try {
            System.setProperty(TEST_IS_PRE_RELEASE_SYSTEM_PROPERTY, value.toString())
            block()
        } finally {
            System.clearProperty(TEST_IS_PRE_RELEASE_SYSTEM_PROPERTY)
        }

    // ------------------------------------------------------------------------------

    fun testRawTypes() {
        compileKotlin("main.kt", tmpdir, listOf(compileLibrary("library")))
    }

    fun testSuspensionPointInMonitor() {
        compileKotlin(
            "source.kt",
            tmpdir,
            listOf(compileLibrary("library", additionalOptions = listOf("-Xskip-metadata-version-check"))),
            additionalOptions = listOf("-Xskip-metadata-version-check")
        )
    }

    fun testDuplicateObjectInBinaryAndSources() {
        val allDescriptors = analyzeAndGetAllDescriptors(compileLibrary("library"))
        assertEquals(allDescriptors.toString(), 2, allDescriptors.size)
        for (descriptor in allDescriptors) {
            assertTrue("Wrong name: " + descriptor, descriptor.name.asString() == "Lol")
            assertTrue("Should be an object: " + descriptor, isObject(descriptor))
        }
    }

    fun testBrokenJarWithNoClassForObject() {
        val brokenJar = copyJarFileWithoutEntry(compileLibrary("library"), "test/Lol.class")
        val allDescriptors = analyzeAndGetAllDescriptors(brokenJar)
        assertEmpty("No descriptors should be found: " + allDescriptors, allDescriptors)
    }

    fun testSameLibraryTwiceInClasspath() {
        doTestWithTxt(compileLibrary("library-1"), compileLibrary("library-2"))
    }

    fun testMissingEnumReferencedInAnnotationArgument() {
        doTestWithTxt(copyJarFileWithoutEntry(compileLibrary("library"), "test/E.class"))
    }

    fun testMissingEnumReferencedInAnnotationArgumentIr() {
        doTestBrokenLibrary("library", "a/E.class", additionalOptions = listOf("-Xuse-ir"))
    }

    fun testNoWarningsOnJavaKotlinInheritance() {
        // This test checks that there are no PARAMETER_NAME_CHANGED_ON_OVERRIDE or DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES
        // warnings when subclassing in Kotlin from Java binaries (in case when no parameter names are available for Java classes)

        val library = compileLibrary("library")
        val environment = createEnvironment(listOf(library))

        val ktFile = KotlinTestUtils.loadJetFile(environment.project, getTestDataFileWithExtension("kt"))
        val result = JvmResolveUtil.analyze(ktFile, environment)
        result.throwIfError()

        AnalyzerWithCompilerReport.reportDiagnostics(
            result.bindingContext.diagnostics,
            PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false)
        )

        assertEquals("There should be no diagnostics", 0, result.bindingContext.diagnostics.count())
    }

    fun testIncompleteHierarchyInJava() {
        doTestBrokenLibrary("library", "test/Super.class")
    }

    fun testIncompleteHierarchyInKotlin() {
        doTestBrokenLibrary("library", "test/Super.class")
    }

    fun testIncompleteHierarchyMissingInterface() {
        doTestBrokenLibrary("library", "test/A.class")
    }

    fun testIncompleteHierarchyOnlyImport() {
        doTestBrokenLibrary("library", "test/Super.class")
    }

    fun testMissingStaticClass() {
        doTestBrokenLibrary("library", "test/C\$D.class")
    }

    fun testIncompleteHierarchyNoErrors() {
        doTestBrokenLibrary("library", "test/Super.class")
    }

    fun testIncompleteHierarchyErrorPositions() {
        doTestBrokenLibrary("library", "test/Super.class")
    }

    fun testIncompleteHierarchyOfEnclosingClass() {
        doTestBrokenLibrary("library", "test/Super.class")
    }

    fun testMissingDependencySimple() {
        doTestBrokenLibrary("library", "a/A.class")
    }

    fun testNonTransitiveDependencyWithJavac() {
        doTestBrokenLibrary("library", "my/Some.class", additionalOptions = listOf("-Xuse-javac", "-Xcompile-java"))
    }

    fun testComputeSupertypeWithMissingDependency() {
        doTestBrokenLibrary("library", "a/A.class")
    }

    fun testMissingDependencyDifferentCases() {
        doTestBrokenLibrary("library", "a/A.class")
    }

    fun testMissingDependencyNestedAnnotation() {
        doTestBrokenLibrary("library", "a/A\$Anno.class")
    }

    fun testMissingDependencyNestedAnnotationIr() {
        doTestBrokenLibrary("library", "a/A\$Anno.class", additionalOptions = listOf("-Xuse-ir"))
    }

    fun testMissingDependencyConflictingLibraries() {
        val library1 = copyJarFileWithoutEntry(
            compileLibrary("library1"),
            "a/A.class", "a/A\$Inner.class", "a/AA.class", "a/AA\$Inner.class",
            "a/AAA.class", "a/AAA\$Inner.class", "a/AAA\$Inner\$Inner.class"
        )
        val library2 = copyJarFileWithoutEntry(
            compileLibrary("library2"),
            "a/A.class", "a/A\$Inner.class", "a/AA.class", "a/AA\$Inner.class",
            "a/AAA.class", "a/AAA\$Inner.class", "a/AAA\$Inner\$Inner.class"
        )
        compileKotlin("source.kt", tmpdir, listOf(library1, library2))
    }

    fun testMissingDependencyJava() {
        doTestBrokenLibrary("library", "test/Bar.class")
    }

    fun testMissingDependencyJavaConflictingLibraries() {
        val library1 = copyJarFileWithoutEntry(compileLibrary("library1"), "test/A.class", "test/A\$Inner.class")
        val library2 = copyJarFileWithoutEntry(compileLibrary("library2"), "test/A.class", "test/A\$Inner.class")
        compileKotlin("source.kt", tmpdir, listOf(library1, library2))
    }

    fun testMissingDependencyJavaNestedAnnotation() {
        doTestBrokenLibrary("library", "test/A\$Anno.class")
    }

    fun testReleaseCompilerAgainstPreReleaseLibrary() {
        doTestPreReleaseKotlinLibrary(K2JVMCompiler(), "library", tmpdir)
    }

    fun testReleaseCompilerAgainstPreReleaseLibraryJs() {
        doTestPreReleaseKotlinLibrary(K2JSCompiler(), "library", File(tmpdir, "usage.js"))
    }

    fun testReleaseCompilerAgainstPreReleaseLibrarySkipPrereleaseCheck() {
        doTestPreReleaseKotlinLibrary(K2JVMCompiler(), "library", tmpdir, "-Xskip-prerelease-check")
    }

    fun testReleaseCompilerAgainstPreReleaseLibraryJsSkipPrereleaseCheck() {
        doTestPreReleaseKotlinLibrary(K2JSCompiler(), "library", File(tmpdir, "usage.js"), "-Xskip-prerelease-check")
    }

    fun testReleaseCompilerAgainstPreReleaseLibrarySkipMetadataVersionCheck() {
        doTestPreReleaseKotlinLibrary(K2JVMCompiler(), "library", tmpdir, "-Xskip-metadata-version-check")
    }

    fun testPreReleaseCompilerAgainstPreReleaseLibraryStableLanguageVersion() {
        withPreRelease(true) {
            val library = compileLibrary("library")
            val someStableReleasedVersion = LanguageVersion.values().first { it.isStable && it >= LanguageVersion.FIRST_SUPPORTED }
            compileKotlin(
                "source.kt", tmpdir, listOf(library), K2JVMCompiler(),
                listOf("-language-version", someStableReleasedVersion.versionString)
            )

            checkPreReleaseness(File(tmpdir, "usage/SourceKt.class"), shouldBePreRelease = false)
        }
    }

    fun testPreReleaseCompilerAgainstPreReleaseLibraryLatestStable() {
        withPreRelease(true) {
            val library = compileLibrary("library")
            compileKotlin(
                "source.kt", tmpdir, listOf(library), K2JVMCompiler(),
                listOf("-language-version", LanguageVersion.LATEST_STABLE.versionString)
            )

            checkPreReleaseness(File(tmpdir, "usage/SourceKt.class"), shouldBePreRelease = true)
        }
    }

    fun testWrongMetadataVersion() {
        doTestKotlinLibraryWithWrongMetadataVersion("library", null)
    }

    fun testWrongMetadataVersionJs() {
        doTestKotlinLibraryWithWrongMetadataVersionJs("library")
    }

    fun testWrongMetadataVersionBadMetadata() {
        doTestKotlinLibraryWithWrongMetadataVersion("library", { name, value ->
            if (JvmAnnotationNames.METADATA_DATA_FIELD_NAME == name) {
                @Suppress("UNCHECKED_CAST")
                val strings = value as Array<String>
                strings.map { string ->
                    String(string.toByteArray().map { x -> x xor 42 }.toTypedArray().toByteArray())
                }.toTypedArray()
            } else null
        })
    }

    fun testWrongMetadataVersionBadMetadata2() {
        doTestKotlinLibraryWithWrongMetadataVersion("library", { name, _ ->
            if (JvmAnnotationNames.METADATA_STRINGS_FIELD_NAME == name) arrayOf<String>() else null
        })
    }

    fun testWrongMetadataVersionSkipVersionCheck() {
        doTestKotlinLibraryWithWrongMetadataVersion("library", null, "-Xskip-metadata-version-check")
    }

    fun testWrongMetadataVersionJsSkipVersionCheck() {
        doTestKotlinLibraryWithWrongMetadataVersionJs("library", "-Xskip-metadata-version-check")
    }

    fun testWrongMetadataVersionSkipPrereleaseCheckHasNoEffect() {
        doTestKotlinLibraryWithWrongMetadataVersion("library", null, "-Xskip-prerelease-check")
    }

    fun testRequireKotlin() {
        compileKotlin("source.kt", tmpdir, listOf(compileLibrary("library")))
    }

    fun testRequireKotlinInNestedClasses() {
        compileKotlin("source.kt", tmpdir, listOf(compileLibrary("library")))
    }

    fun testRequireKotlinInNestedClassesJs() {
        compileKotlin("source.kt", File(tmpdir, "usage.js"), listOf(compileJsLibrary("library")), K2JSCompiler())
    }

    fun testRequireKotlinInNestedClassesAgainst13() {
        val library = compileLibrary(
            "library",
            additionalOptions = listOf("-language-version", "1.3"),
            checkKotlinOutput = { actual ->
                assertEquals(
                    normalizeOutput(
                        "warning: language version 1.3 is deprecated and its support will be removed in a future version of Kotlin\n" to ExitCode.OK
                    ), actual
                )
            }
        )
        compileKotlin("source.kt", tmpdir, listOf(library))
    }

    fun testRequireKotlinInNestedClassesAgainst14Js() {
        val library = compileJsLibrary("library", additionalOptions = listOf("-Xmetadata-version=1.4.0"))
        compileKotlin(
            "source.kt", File(tmpdir, "usage.js"), listOf(library), K2JSCompiler(),
            additionalOptions = listOf("-Xskip-metadata-version-check")
        )
    }

    fun testStrictMetadataVersionSemanticsSameVersion() {
        val library = compileLibrary("library", additionalOptions = listOf("-Xgenerate-strict-metadata-version"))
        compileKotlin("source.kt", tmpdir, listOf(library))
    }

    fun testStrictMetadataVersionSemanticsOldVersion() {
        val library = compileLibrary(
            "library", additionalOptions = listOf("-Xgenerate-strict-metadata-version", "-Xmetadata-version=1.5.0")
        )
        compileKotlin("source.kt", tmpdir, listOf(library))
    }

    fun testMetadataVersionDerivedFromLanguage() {
        compileKotlin("source.kt", tmpdir, additionalOptions = listOf("-language-version", "1.3"), expectedFileName = null)

        val expectedVersion = JvmMetadataVersion(1, 1, 18)
        val topLevelClass = LocalFileKotlinClass.create(File(tmpdir.absolutePath, "Foo.class"))!!
        assertEquals(expectedVersion, topLevelClass.classHeader.metadataVersion)

        val moduleFile = File(tmpdir.absolutePath, "META-INF/main.kotlin_module").readBytes()
        val versionNumber = ModuleMapping.readVersionNumber(DataInputStream(ByteArrayInputStream(moduleFile)))!!
        assertEquals(expectedVersion, JvmMetadataVersion(*versionNumber))
    }

    /*test source mapping generation when source info is absent*/
    fun testInlineFunWithoutDebugInfo() {
        compileKotlin("sourceInline.kt", tmpdir)

        val inlineFunClass = File(tmpdir.absolutePath, "test/A.class")
        val cw = ClassWriter(Opcodes.API_VERSION)
        ClassReader(inlineFunClass.readBytes()).accept(object : ClassVisitor(Opcodes.API_VERSION, cw) {
            override fun visitSource(source: String?, debug: String?) {
                //skip debug info
            }
        }, 0)

        assert(inlineFunClass.delete())
        assert(!inlineFunClass.exists())

        inlineFunClass.writeBytes(cw.toByteArray())

        compileKotlin("source.kt", tmpdir, listOf(tmpdir))

        val resultFile = File(tmpdir.absolutePath, "test/B.class")
        ClassReader(resultFile.readBytes()).accept(object : ClassVisitor(Opcodes.API_VERSION) {
            override fun visitSource(source: String?, debug: String?) {
                assertEquals(null, debug)
            }
        }, 0)
    }

    /* Regression test for KT-37107: compile against .class file without any constructors. */
    fun testClassfileWithoutConstructors() {
        compileKotlin("TopLevel.kt", tmpdir, expectedFileName = "TopLevel.txt")

        val inlineFunClass = File(tmpdir.absolutePath, "test/TopLevelKt.class")
        val cw = ClassWriter(Opcodes.API_VERSION)
        ClassReader(inlineFunClass.readBytes()).accept(object : ClassVisitor(Opcodes.API_VERSION, cw) {
            override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? =
                if (desc == JvmAnnotationNames.METADATA_DESC) null else super.visitAnnotation(desc, visible)

            override fun visitMethod(
                access: Int,
                name: String?,
                descriptor: String?,
                signature: String?,
                exceptions: Array<out String>?
            ): MethodVisitor {
                assertEquals("foo", name) // test sanity: shouldn't see any constructors, only the "foo" method
                return super.visitMethod(access, name, descriptor, signature, exceptions)
            }
        }, 0)

        assert(inlineFunClass.delete())
        assert(!inlineFunClass.exists())

        inlineFunClass.writeBytes(cw.toByteArray())

        val (_, exitCode) = compileKotlin("shouldNotCompile.kt", tmpdir, listOf(tmpdir))
        assertEquals(1, exitCode.code) // double-check that we failed :) output.txt also says so
    }

    fun testReplaceAnnotationClassWithInterface() {
        val library1 = compileLibrary("library-1")
        val usage = compileLibrary("usage", extraClassPath = listOf(library1))
        val library2 = compileLibrary("library-2")
        doTestWithTxt(usage, library2)
    }

    fun testProhibitNestedClassesByDollarName() {
        val library = compileLibrary("library")
        compileKotlin("main.kt", tmpdir, listOf(library))
    }

    fun testInnerClassPackageConflict() {
        val output = compileLibrary("library", destination = File(tmpdir, "library"))
        File(testDataDirectory, "library/test/Foo/x.txt").copyTo(File(output, "test/Foo/x.txt"))
        MockLibraryUtil.createJarFile(tmpdir, output, "library")
        compileKotlin("source.kt", tmpdir, listOf(File(tmpdir, "library.jar")))
    }

    fun testInnerClassPackageConflict2() {
        val library1 = compileLibrary("library1", destination = File(tmpdir, "library1"))
        val library2 = compileLibrary("library2", destination = File(tmpdir, "library2"))

        // Copy everything from library2 to library1
        FileUtil.visitFiles(library2) { file ->
            if (!file.isDirectory) {
                val newFile = File(library1, file.relativeTo(library2).path)
                if (!newFile.parentFile.exists()) {
                    assert(newFile.parentFile.mkdirs())
                }
                assert(file.renameTo(newFile))
            }
            true
        }

        compileKotlin("source.kt", tmpdir, listOf(library1))
    }

    fun testWrongInlineTarget() {
        val library = compileLibrary("library", additionalOptions = listOf("-jvm-target", "11"))

        compileKotlin("source.kt", tmpdir, listOf(library), additionalOptions = listOf("-jvm-target", "1.8"))

        compileKotlin(
            "warningsOnly_1_3.kt", tmpdir, listOf(library),
            additionalOptions = listOf("-language-version", "1.3"),
            expectedFileName = "warningsOnly_1_3.txt"
        )
    }

    fun testInlineFunctionsWithMatchingJvmSignatures() {
        val library = compileLibrary(
            "library",
            additionalOptions = listOf("-XXLanguage:+InlineClasses"),
            checkKotlinOutput = { _ -> }
        )
        compileKotlin("source.kt", tmpdir, listOf(library), additionalOptions = listOf("-XXLanguage:+InlineClasses"))

        URLClassLoader(arrayOf(library.toURI().toURL(), tmpdir.toURI().toURL()), ForTestCompileRuntime.runtimeJarClassLoader())
            .loadClass("SourceKt").getDeclaredMethod("run").invoke(null)
    }

    fun testClassFromJdkInLibrary() {
        val library = compileLibrary("library")
        compileKotlin("source.kt", tmpdir, listOf(library))
    }

    fun testInternalFromForeignModule() {
        compileKotlin("source.kt", tmpdir, listOf(compileLibrary("library")))
    }

    fun testInternalFromFriendModule() {
        val library = compileLibrary("library")
        compileKotlin("source.kt", tmpdir, listOf(library), additionalOptions = listOf("-Xfriend-paths=${library.path}"))
    }

    fun testInternalFromFriendModuleFir() {
        val library = compileLibrary("library")
        compileKotlin("source.kt", tmpdir, listOf(library), additionalOptions = listOf("-Xfriend-paths=${library.path}", "-Xuse-fir"))
    }

    fun testJvmDefaultClashWithOld() {
        val library = compileLibrary("library", additionalOptions = listOf("-Xjvm-default=disable"))
        compileKotlin("source.kt", tmpdir, listOf(library), additionalOptions = listOf("-jvm-target", "1.8", "-Xjvm-default=all"))
    }

    fun testJvmDefaultCompatibilityAgainstJava() {
        val library = compileLibrary("library", additionalOptions = listOf("-Xjvm-default=disable"))
        compileKotlin(
            "source.kt",
            tmpdir,
            listOf(library),
            additionalOptions = listOf("-jvm-target", "1.8", "-Xjvm-default=all-compatibility")
        )
    }

    fun testInternalFromForeignModuleJs() {
        compileKotlin("source.kt", File(tmpdir, "usage.js"), listOf(compileJsLibrary("library")), K2JSCompiler())
    }

    fun testInternalFromFriendModuleJs() {
        val library = compileJsLibrary("library")
        compileKotlin("source.kt", File(tmpdir, "usage.js"), listOf(library), K2JSCompiler(), listOf("-Xfriend-modules=${library.path}"))
    }

    /*
    // TODO: see KT-15661 and KT-23483
    fun testInternalFromForeignModuleCommon() {
        compileKotlin("source.kt", tmpdir, listOf(compileCommonLibrary("library")), K2MetadataCompiler())
    }
    */

    fun testInternalFromFriendModuleCommon() {
        val library = compileCommonLibrary("library")
        compileKotlin(
            "source.kt", tmpdir, listOf(library), K2MetadataCompiler(), listOf(
                // TODO: "-Xfriend-paths=${library.path}"
            )
        )
    }

    fun testInlineAnonymousObjectWithDifferentTarget() {
        val library = compileLibrary("library", additionalOptions = listOf("-jvm-target", JvmTarget.JVM_1_8.description))
        compileKotlin("source.kt", tmpdir, listOf(library), additionalOptions = listOf("-jvm-target", JvmTarget.JVM_9.description))
        for (name in listOf("SourceKt", "SourceKt\$main\$\$inlined\$foo$1")) {
            val node = ClassNode()
            ClassReader(File(tmpdir, "$name.class").readBytes()).accept(node, 0)
            assertEquals(JvmTarget.JVM_9.majorVersion, node.version)
        }
    }

    fun testFirAgainstFir() {
        val library = compileLibrary("library", additionalOptions = listOf("-Xuse-fir"))
        compileKotlin("source.kt", tmpdir, listOf(library), additionalOptions = listOf("-Xuse-fir"))
    }

    fun testFirAgainstOldJvm() {
        val library = compileLibrary("library")
        compileKotlin("source.kt", tmpdir, listOf(library), additionalOptions = listOf("-Xuse-fir"))
    }

    fun testOldJvmAgainstJvmIr() {
        val library = compileLibrary("library", additionalOptions = listOf("-Xuse-ir"))
        compileKotlin("source.kt", tmpdir, listOf(library))

        val library2 = compileLibrary("library", additionalOptions = listOf("-Xuse-ir", "-Xabi-stability=stable"))
        compileKotlin("source.kt", tmpdir, listOf(library2))
    }

    fun testOldJvmAgainstFir() {
        val library = compileLibrary("library", additionalOptions = listOf("-Xuse-fir"))
        compileKotlin("source.kt", tmpdir, listOf(library))

        val library2 = compileLibrary("library", additionalOptions = listOf("-Xuse-fir", "-Xabi-stability=unstable"))
        compileKotlin("source.kt", tmpdir, listOf(library2))
    }

    fun testOldJvmAgainstJvmIrWithUnstableAbi() {
        val library = compileLibrary("library", additionalOptions = listOf("-Xuse-ir", "-Xabi-stability=unstable"))
        compileKotlin("source.kt", tmpdir, listOf(library))
    }

    fun testOldJvmAgainstFirWithStableAbi() {
        val library = compileLibrary("library", additionalOptions = listOf("-Xuse-fir", "-Xabi-stability=stable"))
        compileKotlin("source.kt", tmpdir, listOf(library))
    }

    fun testOldJvmAgainstFirWithAllowUnstableDependencies() {
        val library = compileLibrary("library", additionalOptions = listOf("-Xuse-fir"))
        compileKotlin("source.kt", tmpdir, listOf(library), additionalOptions = listOf("-Xallow-unstable-dependencies"))
    }

    fun testSealedClassesAndInterfaces() {
        val features = listOf("-XXLanguage:+AllowSealedInheritorsInDifferentFilesOfSamePackage", "-XXLanguage:+SealedInterfaces")
        val library = compileLibrary("library", additionalOptions = features, checkKotlinOutput = {})
        compileKotlin("main.kt", tmpdir, listOf(library), additionalOptions = features)
    }

    fun testSealedInheritorInDifferentModule() {
        val features = listOf("-XXLanguage:+AllowSealedInheritorsInDifferentFilesOfSamePackage", "-XXLanguage:+SealedInterfaces")
        val library = compileLibrary("library", additionalOptions = features, checkKotlinOutput = {})
        compileKotlin("main.kt", tmpdir, listOf(library), additionalOptions = features)
    }

    // If this test fails, then bootstrap compiler most likely should be advanced
    fun testPreReleaseFlagIsConsistentBetweenBootstrapAndCurrentCompiler() {
        val bootstrapCompiler = JarFile(PathUtil.kotlinPathsForCompiler.compilerPath)
        val classFromBootstrapCompiler = bootstrapCompiler.getEntry(LanguageFeature::class.java.name.replace(".", "/") + ".class")
        checkPreReleaseness(
            bootstrapCompiler.getInputStream(classFromBootstrapCompiler).readBytes(),
            KotlinCompilerVersion.isPreRelease()
        )
    }

    fun testPreReleaseFlagIsConsistentBetweenStdlibAndCurrentCompiler() {
        val stdlib = JarFile(PathUtil.kotlinPathsForCompiler.stdlibPath)
        val classFromStdlib = stdlib.getEntry(KotlinVersion::class.java.name.replace(".", "/") + ".class")
        checkPreReleaseness(
            stdlib.getInputStream(classFromStdlib).readBytes(),
            KotlinCompilerVersion.isPreRelease()
        )
    }

    fun testInlineClassesManglingAgainstLV13() {
        val library = compileLibrary(
            "library",
            additionalOptions = listOf("-language-version", "1.3", "-Xinline-classes"),
            checkKotlinOutput = {}
        )
        compileKotlin(
            "source.kt",
            tmpdir,
            listOf(library),
            additionalOptions = listOf("-XXLanguage:-MangleClassMembersReturningInlineClasses", "-Xinline-classes")
        )
        // Difference in mangling becomes apparent only on load time as NSME, so, to check the mangling we need to load the classfile
        loadClassFile("SourceKt", tmpdir, library)
    }

    fun testAnonymousObjectTypeMetadata() {
        val library = compileCommonLibrary(
            libraryName = "library",
        )
        compileKotlin(
            "anonymousObjectTypeMetadata.kt",
            tmpdir,
            listOf(library),
            K2MetadataCompiler(),
        )
    }

    fun testAnonymousObjectTypeMetadataKlib() {
        val klibLibrary = compileCommonLibrary(
            libraryName = "library",
            listOf("-Xexpect-actual-linker"),
        )
        compileKotlin(
            "anonymousObjectTypeMetadata.kt",
            tmpdir,
            listOf(klibLibrary),
            K2MetadataCompiler(),
            listOf("-Xexpect-actual-linker")
        )
    }

    private fun loadClassFile(className: String, dir: File, library: File) {
        val classLoader = URLClassLoader(arrayOf(dir.toURI().toURL(), library.toURI().toURL()))
        val mainClass = classLoader.loadClass(className)
        mainClass.getDeclaredMethod("main", Array<String>::class.java).invoke(null, arrayOf<String>())
    }

    companion object {
        // compiler before 1.1.4 version  did not include suspension marks into bytecode.
        private fun stripSuspensionMarksToImitateLegacyCompiler(bytes: ByteArray): Pair<ByteArray, Int> {
            val writer = ClassWriter(0)
            var removedCounter = 0
            ClassReader(bytes).accept(object : ClassVisitor(Opcodes.API_VERSION, writer) {
                override fun visitMethod(
                    access: Int,
                    name: String?,
                    desc: String?,
                    signature: String?,
                    exceptions: Array<out String>?
                ): MethodVisitor {
                    val superMV = super.visitMethod(access, name, desc, signature, exceptions)
                    return object : MethodNode(Opcodes.API_VERSION, access, name, desc, signature, exceptions) {
                        override fun visitEnd() {
                            val removeList = instructions.asSequence()
                                .flatMap { suspendMarkerInsns(it).asSequence() }.toList()
                            remove(removeList)
                            removedCounter += removeList.size
                            accept(superMV)
                        }
                    }
                }
            }, 0)
            return writer.toByteArray() to removedCounter
        }

        // KLUDGE: here is a simplified copy of compiler's logic for suspend markers

        private fun suspendMarkerInsns(insn: AbstractInsnNode): List<AbstractInsnNode> =
            if (insn is MethodInsnNode
                && insn.opcode == Opcodes.INVOKESTATIC
                && insn.owner == "kotlin/jvm/internal/InlineMarker"
                && insn.name == "mark"
                && insn.previous.intConstant in 0..1
            ) listOf(insn, insn.previous)
            else emptyList()

        // -----

        private fun copyJarFileWithoutEntry(jarPath: File, vararg entriesToDelete: String): File =
            transformJar(jarPath, { _, bytes -> bytes }, entriesToDelete.toSet())

        private fun transformJar(
            jarPath: File,
            transformEntry: (String, ByteArray) -> ByteArray,
            entriesToDelete: Set<String> = emptySet()
        ): File {
            val outputFile = File(jarPath.parentFile, "${jarPath.nameWithoutExtension}-after.jar")

            JarFile(jarPath).use { jar ->
                ZipOutputStream(outputFile.outputStream().buffered()).use { output ->
                    for (jarEntry in jar.entries()) {
                        val name = jarEntry.name
                        if (name in entriesToDelete) continue

                        val bytes = jar.getInputStream(jarEntry).readBytes()
                        val newBytes = if (name.endsWith(".class")) transformEntry(name, bytes) else bytes
                        val newEntry = JarEntry(name)
                        newEntry.size = newBytes.size.toLong()
                        output.putNextEntry(newEntry)
                        output.write(newBytes)
                        output.closeEntry()
                    }
                }
            }

            return outputFile
        }

        private fun checkPreReleaseness(classFileBytes: ByteArray, shouldBePreRelease: Boolean) {
            // If there's no "xi" field in the Metadata annotation, it's value is assumed to be 0, i.e. _not_ pre-release
            var isPreRelease = false

            ClassReader(classFileBytes).accept(object : ClassVisitor(Opcodes.API_VERSION) {
                override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
                    if (desc != JvmAnnotationNames.METADATA_DESC) return null

                    return object : AnnotationVisitor(Opcodes.API_VERSION) {
                        override fun visit(name: String, value: Any) {
                            if (name != JvmAnnotationNames.METADATA_EXTRA_INT_FIELD_NAME) return

                            isPreRelease = (value as Int and JvmAnnotationNames.METADATA_PRE_RELEASE_FLAG) != 0
                        }
                    }
                }
            }, 0)

            TestCase.assertEquals("Pre-release flag of the class file has incorrect value", shouldBePreRelease, isPreRelease)
        }


        private fun checkPreReleaseness(file: File, shouldBePreRelease: Boolean) {
            checkPreReleaseness(file.readBytes(), shouldBePreRelease)
        }
    }
}
