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
import org.jetbrains.kotlin.descriptors.deserialization.PlatformDependentDeclarationFilter
import org.jetbrains.kotlin.jvm.compiler.LoadDescriptorUtil.TEST_PACKAGE_FQNAME
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInsLoaderImpl
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator
import java.io.File
import java.io.FileInputStream

class BuiltInsSerializerTest : TestCaseWithTmpdir() {
    private fun doTest(fileName: String) {
        val source = "compiler/testData/serialization/builtinsSerializer/$fileName"
        BuiltInsSerializer(dependOnOldBuiltIns = true).serialize(
                tmpdir,
                srcDirs = listOf(File(source)),
                extraClassPath = listOf(ForTestCompileRuntime.runtimeJarForTests()),
                onComplete = { _, _ -> }
        )

        val module = KotlinTestUtils.createEmptyModule("<module>", DefaultBuiltIns.Instance)

        val packageFragmentProvider = BuiltInsLoaderImpl().createBuiltInPackageFragmentProvider(
                LockBasedStorageManager("BuiltInsSerializerTest"), module, setOf(TEST_PACKAGE_FQNAME), emptyList(), PlatformDependentDeclarationFilter.All
        ) {
            val file = File(tmpdir, it)
            if (file.exists()) FileInputStream(file) else null
        }

        module.initialize(packageFragmentProvider)
        module.setDependencies(module, module.builtIns.builtInsModule)

        RecursiveDescriptorComparator.validateAndCompareDescriptorWithFile(
                module.getPackage(TEST_PACKAGE_FQNAME),
                RecursiveDescriptorComparator.DONT_INCLUDE_METHODS_OF_OBJECT,
                File(source.replace(".kt", ".txt"))
        )
    }

    fun testSimple() {
        doTest("simple.kt")
    }

    fun testTypeParameterAnnotation() {
        doTest("typeParameterAnnotation.kt")
    }

    fun testNestedClassesAndObjects() {
        doTest("nestedClassesAndObjects.kt")
    }

    fun testCompileTimeConstants() {
        doTest("compileTimeConstants.kt")
    }

    fun testAnnotationTargets() {
        doTest("annotationTargets.kt")
    }

    fun testAnnotatedEnumEntry() {
        doTest("annotatedEnumEntry.kt")
    }

    fun testPrimitives() {
        doTest("annotationArguments/primitives.kt")
    }

    fun testPrimitiveArrays() {
        doTest("annotationArguments/primitiveArrays.kt")
    }

    fun testString() {
        doTest("annotationArguments/string.kt")
    }

    fun testAnnotation() {
        doTest("annotationArguments/annotation.kt")
    }

    fun testEnum() {
        doTest("annotationArguments/enum.kt")
    }

    fun testVarArgs() {
        doTest("annotationArguments/varargs.kt")
    }

    fun testSourceRetainedAnnotation() {
        doTest("sourceRetainedAnnotation.kt")
    }

    fun testBinaryRetainedAnnotation() {
        doTest("binaryRetainedAnnotation.kt")
    }

    fun testPropertyAccessorAnnotations() {
        doTest("propertyAccessorAnnotations.kt")
    }
}
