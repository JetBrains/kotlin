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

import org.jetbrains.kotlin.cli.metadata.K2MetadataCompiler
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.jvm.compiler.LoadDescriptorUtil.TEST_PACKAGE_FQNAME
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.KlibTestUtil
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparatorAdaptor
import java.io.File

class KotlinKlibSerializerTest : TestCaseWithTmpdir() {
    private val BASE_DIR = "compiler/testData/serialization"

    private fun doTest(fileName: String, goldenDataExtension: String = ".txt") {
        val source = "$BASE_DIR/$fileName"
        val klibName = File(source).nameWithoutExtension
        val klibFile = File(tmpdir, "$klibName.klib")

        CompilerTestUtil.executeCompilerAssertSuccessful(K2MetadataCompiler(), listOf(
                File(source).absolutePath,
                "-d", klibFile.absolutePath,
                "-module-name", klibName,
                // support for the legacy version of kotlin-stdlib-common (JAR with .kotlin_metadata)
                "-classpath", ForTestCompileRuntime.stdlibCommonForTests().absolutePath
            )
        )

        val module = KlibTestUtil.deserializeKlibToCommonModule(klibFile)

        RecursiveDescriptorComparatorAdaptor.validateAndCompareDescriptorWithFile(
            module.getPackage(TEST_PACKAGE_FQNAME),
            RecursiveDescriptorComparator.DONT_INCLUDE_METHODS_OF_OBJECT,
            File(source.replace(".kt", goldenDataExtension))
        )
    }

    fun testSimple() {
        doTest("builtinsSerializer/simple.kt")
    }

    fun testNestedClassesAndObjects() {
        doTest("builtinsSerializer/nestedClassesAndObjects.kt")
    }

    fun testCompileTimeConstants() {
        // After implementation of https://youtrack.jetbrains.com/issue/KT-65805/Migrate-builtins-serializer-to-K2,
        // compileTimeConstants.txt will be same as compileTimeConstants.fir.txt. So, it would be worthwhile to unify them.
        doTest("builtinsSerializer/compileTimeConstants.kt", ".fir.txt")
    }

    fun testAnnotationTargets() {
        doTest("builtinsSerializer/annotationTargets.kt")
    }

    fun testAnnotatedEnumEntry() {
        doTest("builtinsSerializer/annotatedEnumEntry.kt")
    }

    fun testPrimitives() {
        doTest("builtinsSerializer/annotationArguments/primitives.kt")
    }

    fun testPrimitiveArrays() {
        doTest("builtinsSerializer/annotationArguments/primitiveArrays.kt")
    }

    fun testString() {
        doTest("builtinsSerializer/annotationArguments/string.kt")
    }

    fun testAnnotation() {
        doTest("builtinsSerializer/annotationArguments/annotation.kt")
    }

    fun testEnum() {
        doTest("builtinsSerializer/annotationArguments/enum.kt")
    }

    fun testPropertyAccessorAnnotations() {
        doTest("builtinsSerializer/propertyAccessorAnnotations.kt")
    }

    fun testReceiverAnnotations() {
        doTest("klib/receiverAnnotations.kt")
    }

    fun testFieldAnnotations() {
        doTest("klib/fieldAnnotations.kt")
    }
}
