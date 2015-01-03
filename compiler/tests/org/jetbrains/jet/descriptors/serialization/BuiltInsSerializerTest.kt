/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.descriptors.serialization

import java.io.File
import org.jetbrains.kotlin.serialization.builtins.BuiltInsSerializer
import org.jetbrains.jet.test.TestCaseWithTmpdir
import org.jetbrains.jet.lang.types.lang.BuiltinsPackageFragment
import org.jetbrains.jet.jvm.compiler.LoadDescriptorUtil.TEST_PACKAGE_FQNAME
import org.jetbrains.jet.storage.LockBasedStorageManager
import org.jetbrains.jet.JetTestUtils
import java.io.FileInputStream
import org.jetbrains.jet.test.util.RecursiveDescriptorComparator
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.codegen.forTestCompile.ForTestCompileRuntime

public class BuiltInsSerializerTest : TestCaseWithTmpdir() {
    private fun doTest(fileName: String) {
        val source = "compiler/testData/serialization/$fileName"
        BuiltInsSerializer(dependOnOldBuiltIns = true).serialize(
                tmpdir,
                srcDirs = listOf(File(source)),
                extraClassPath = listOf(ForTestCompileRuntime.runtimeJarForTests()),
                onComplete = { totalSize, totalFiles -> }
        )

        val module = JetTestUtils.createEmptyModule("<module>")

        val packageFragment = BuiltinsPackageFragment(TEST_PACKAGE_FQNAME, LockBasedStorageManager(), module) {
            path ->
            val file = File(tmpdir, path)
            if (file.exists()) FileInputStream(file) else null
        }

        module.initialize(packageFragment.provider)
        module.addDependencyOnModule(module)
        module.addDependencyOnModule(KotlinBuiltIns.getInstance().getBuiltInsModule())
        module.seal()

        RecursiveDescriptorComparator.validateAndCompareDescriptorWithFile(
                module.getPackage(TEST_PACKAGE_FQNAME),
                RecursiveDescriptorComparator.DONT_INCLUDE_METHODS_OF_OBJECT,
                File(source.replace(".kt", ".txt"))
        )
    }

    fun testSimple() {
        doTest("simple.kt")
    }

    fun testAnnotationTargets() {
        doTest("annotationTargets.kt")
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
}
