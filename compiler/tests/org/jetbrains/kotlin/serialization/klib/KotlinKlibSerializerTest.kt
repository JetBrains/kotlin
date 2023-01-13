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

import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.resolve.JsPlatformAnalyzerServices
import org.jetbrains.kotlin.jvm.compiler.LoadDescriptorUtil.TEST_PACKAGE_FQNAME
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.serialization.NonStableParameterNamesSerializationTest
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil.readModuleAsProto
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.KlibTestUtil
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparatorAdaptor
import org.jetbrains.kotlin.utils.JsMetadataVersion
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import org.jetbrains.kotlin.utils.sure
import java.io.File

class KotlinKlibSerializerTest : TestCaseWithTmpdir() {
    private val BASE_DIR = "compiler/testData/serialization"

    private fun doTest(fileName: String) {
        val source = "$BASE_DIR/$fileName"
        val klibName = File(source).nameWithoutExtension
        val klibFile = File(tmpdir, "$klibName.klib")
        KlibTestUtil.compileCommonSourcesToKlib(listOf(File(source)), klibName, klibFile)

        val module = KlibTestUtil.deserializeKlibToCommonModule(klibFile)

        RecursiveDescriptorComparatorAdaptor.validateAndCompareDescriptorWithFile(
            module.getPackage(TEST_PACKAGE_FQNAME),
            RecursiveDescriptorComparator.DONT_INCLUDE_METHODS_OF_OBJECT,
            File(source.replace(".kt", ".txt"))
        )
    }

    fun testSimple() {
        doTest("builtinsSerializer/simple.kt")
    }

    fun testNestedClassesAndObjects() {
        doTest("builtinsSerializer/nestedClassesAndObjects.kt")
    }

    fun testCompileTimeConstants() {
        doTest("builtinsSerializer/compileTimeConstants.kt")
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
