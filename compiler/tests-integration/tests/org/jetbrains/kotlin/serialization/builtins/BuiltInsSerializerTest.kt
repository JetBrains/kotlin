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

package org.jetbrains.kotlin.serialization.builtins

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.descriptors.deserialization.AdditionalClassPartsProvider
import org.jetbrains.kotlin.descriptors.deserialization.PlatformDependentDeclarationFilter
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInsLoaderImpl
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.io.File
import java.io.FileInputStream

abstract class BuiltInsSerializerTest(val useK2: Boolean) {
    companion object {
        private val TEST_PACKAGE_FQNAME: FqName = FqName.topLevel(Name.identifier("test"))
    }

    @Suppress("JUnitTestCaseWithNoTests")
    class K1BuiltInsSerializerTest : BuiltInsSerializerTest(useK2 = false)

    @Suppress("JUnitTestCaseWithNoTests")
    class K2BuiltInsSerializerTest : BuiltInsSerializerTest(useK2 = true)

    private lateinit var tmpDir: File

    @BeforeEach
    fun setUp(testInfo: TestInfo) {
        tmpDir = KtTestUtil.tmpDirForTest(testInfo.testClass.get().getSimpleName(), testInfo.displayName)
    }

    private fun doTest(fileName: String) {
        val source = "compiler/testData/serialization/builtinsSerializer/$fileName"
        BuiltInsSerializer.analyzeAndSerialize(
            tmpDir,
            srcDirs = listOf(File(source)),
            extraClassPath = listOf(ForTestCompileRuntime.runtimeJarForTests()),
            dependOnOldBuiltIns = true,
            useK2 = useK2,
            onComplete = { _, _ -> }
        )

        val module = createEmptyModule()

        val packageFragmentProvider = BuiltInsLoaderImpl().createBuiltInPackageFragmentProvider(
            LockBasedStorageManager("BuiltInsSerializerTest"),
            module,
            setOf(TEST_PACKAGE_FQNAME),
            emptyList(),
            PlatformDependentDeclarationFilter.All,
            AdditionalClassPartsProvider.None,
            isFallback = false
        ) {
            File(tmpDir, it).takeIf(File::exists)?.let(::FileInputStream)
        }

        module.initialize(packageFragmentProvider)
        module.setDependencies(module, module.builtIns.builtInsModule)

        val firDifference = File(source).readText().contains("// FIR_DIFFERENCE")

        val expectedK1File = File(source.replace(".kt", ".txt"))
        val expectedK2File = File(source.replace(".kt", ".fir.txt"))

        val expectedFile = when {
            useK2 == false -> expectedK1File
            firDifference -> expectedK2File
            else -> expectedK1File
        }

        RecursiveDescriptorComparator.validateAndCompareDescriptorWithFile(
            module.getPackage(TEST_PACKAGE_FQNAME),
            RecursiveDescriptorComparator.DONT_INCLUDE_METHODS_OF_OBJECT,
            expectedFile,
            JUnit5Assertions
        )

        if (useK2 && firDifference && expectedK1File.exists()) {
            val k1Dump = expectedK1File.readText().trim()
            val k2Dump = expectedK2File.readText().trim()
            if (k1Dump == k2Dump) {
                JUnit5Assertions.fail {
                    "K1 and K2 dumps are identical. Remove `// FIR_DIFFERENCE` directive and $expectedK2File"
                }
            }
        }
    }

    private fun createEmptyModule(): ModuleDescriptorImpl {
        return ModuleDescriptorImpl(Name.special("<module>"), LockBasedStorageManager.NO_LOCKS, DefaultBuiltIns.Instance)
    }

    @Test
    fun testSimple() {
        doTest("simple.kt")
    }

    @Test
    fun testTypeParameterAnnotation() {
        doTest("typeParameterAnnotation.kt")
    }

    @Test
    fun testNestedClassesAndObjects() {
        doTest("nestedClassesAndObjects.kt")
    }

    @Test
    fun testCompileTimeConstants() {
        doTest("compileTimeConstants.kt")
    }

    @Test
    fun testAnnotationTargets() {
        doTest("annotationTargets.kt")
    }

    @Test
    fun testAnnotatedEnumEntry() {
        doTest("annotatedEnumEntry.kt")
    }

    @Test
    fun testPrimitives() {
        doTest("annotationArguments/primitives.kt")
    }

    @Test
    fun testPrimitiveArrays() {
        doTest("annotationArguments/primitiveArrays.kt")
    }

    @Test
    fun testString() {
        doTest("annotationArguments/string.kt")
    }

    @Test
    fun testAnnotation() {
        doTest("annotationArguments/annotation.kt")
    }

    @Test
    fun testEnum() {
        doTest("annotationArguments/enum.kt")
    }

    @Test
    fun testVarArgs() {
        doTest("annotationArguments/varargs.kt")
    }

    @Test
    fun testSourceRetainedAnnotation() {
        doTest("sourceRetainedAnnotation.kt")
    }

    @Test
    fun testBinaryRetainedAnnotation() {
        doTest("binaryRetainedAnnotation.kt")
    }

    @Test
    fun testPropertyAccessorAnnotations() {
        doTest("propertyAccessorAnnotations.kt")
    }
}
