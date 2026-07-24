/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.serialization.klib

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.metadata.KotlinMetadataCompiler
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.jvm.compiler.LoadDescriptorUtil.TEST_PACKAGE_FQNAME
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.NullFlexibleTypeDeserializer
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparatorAdaptor
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Note that `BuiltInsSerializerTest.K2BuiltInsSerializerTest` uses the same testdata
 */
class KotlinKlibSerializerTest : TestCaseWithTmpdir() {
    private val BASE_DIR = ForTestCompileRuntime.transformTestDataPath("compiler/testData/serialization").path

    private fun doTest(fileName: String, goldenDataExtension: String = ".txt") {
        val source = "$BASE_DIR/$fileName"
        val klibFile = compileOneFile(source)

        compareDumps(klibFile, source, goldenDataExtension)
    }

    private fun doTestWithDependency(mainFileName: String, dependencyFileName: String, goldenDataExtension: String = ".txt") {
        val dependencySource = "$BASE_DIR/$dependencyFileName"
        val dependencyKlibFile = compileOneFile(dependencySource)
        compareDumps(dependencyKlibFile, dependencySource, goldenDataExtension)

        val mainSource = "$BASE_DIR/$mainFileName"
        val mainKlibFile = compileOneFile(mainSource, dependencyKlibFile.absolutePath)
        compareDumps(mainKlibFile, mainSource, goldenDataExtension)
    }

    private fun compileOneFile(source: String, vararg additionalClassPath: String): File {
        val klibName = File(source).nameWithoutExtension
        val klibFile = File(tmpdir, "$klibName.klib")

        val classpath = buildList {
            addAll(additionalClassPath)
            add(ForTestCompileRuntime.stdlibCommonForTests().absolutePath)
        }
        CompilerTestUtil.executeCompilerAssertSuccessful(
            KotlinMetadataCompiler(), listOf(
                File(source).absolutePath,
                "-d", klibFile.absolutePath,
                "-module-name", klibName,
                // support for the legacy version of kotlin-stdlib-common (JAR with .kotlin_metadata)
                "-classpath", classpath.joinToString(File.pathSeparatorChar.toString())
            )
        )
        return klibFile
    }

    private fun compareDumps(klibFile: File, source: String, goldenDataExtension: String) {
        val module = deserializeKlibToCommonModule(klibFile)

        RecursiveDescriptorComparatorAdaptor.validateAndCompareDescriptorWithFile(
            module.getPackage(TEST_PACKAGE_FQNAME),
            RecursiveDescriptorComparator.DONT_INCLUDE_METHODS_OF_OBJECT,
            File(source.replace(".kt", goldenDataExtension))
        )
    }

    private fun deserializeKlibToCommonModule(klibFile: File): ModuleDescriptorImpl {
        val library = KlibLoader { libraryPaths(klibFile) }.load().librariesStdlibFirst.single()

        val metadataFactories = KlibMetadataFactories({ DefaultBuiltIns.Instance }, NullFlexibleTypeDeserializer)

        val module = metadataFactories.DefaultDeserializedDescriptorFactory.createDescriptor(
            library = library,
            languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
            storageManager = LockBasedStorageManager.NO_LOCKS,
            builtIns = DefaultBuiltIns.Instance,
        )
        module.setDependencies(listOf(DefaultBuiltIns.Instance.builtInsModule, module))

        return module
    }


    @Test
    fun testSimple() {
        doTest("builtinsSerializer/simple.kt")
    }

    @Test
    fun testNestedClassesAndObjects() {
        doTest("builtinsSerializer/nestedClassesAndObjects.kt", ".fir.txt")
    }

    @Test
    fun testCompileTimeConstants() {
        // After implementation of https://youtrack.jetbrains.com/issue/KT-65805/Migrate-builtins-serializer-to-K2,
        // compileTimeConstants.txt will be same as compileTimeConstants.fir.txt. So, it would be worthwhile to unify them.
        doTest("builtinsSerializer/compileTimeConstants.kt", ".fir.txt")
    }

    @Test
    fun testAnnotationTargets() {
        doTest("builtinsSerializer/annotationTargets.kt")
    }

    @Test
    fun testAnnotatedEnumEntry() {
        doTest("builtinsSerializer/annotatedEnumEntry.kt")
    }

    @Test
    fun testPrimitives() {
        doTest("builtinsSerializer/annotationArguments/primitives.kt")
    }

    @Test
    fun testPrimitiveArrays() {
        doTest("builtinsSerializer/annotationArguments/primitiveArrays.kt")
    }

    @Test
    fun testString() {
        doTest("builtinsSerializer/annotationArguments/string.kt")
    }

    @Test
    fun testAnnotation() {
        doTest("builtinsSerializer/annotationArguments/annotation.kt")
    }

    @Test
    fun testEnum() {
        doTest("builtinsSerializer/annotationArguments/enum.kt")
    }

    @Test
    fun testPropertyAccessorAnnotations() {
        doTest("builtinsSerializer/propertyAccessorAnnotations.kt", ".fir.txt")
    }

    @Test
    fun testReceiverAnnotations() {
        doTest("klib/receiverAnnotations.kt")
    }

    @Test
    fun testFieldAnnotations() {
        doTest("klib/fieldAnnotations.kt")
    }

    @Test
    fun testDelegationToInterfaceWithDeprecation() {
        doTestWithDependency("klib/delegationToInterfaceWithDeprecation_main.kt", "klib/delegationToInterfaceWithDeprecation_dep.kt")
    }

    @Test
    fun testComplexDeprecation() {
        doTest("klib/complexDeprecation.kt")
    }
}
