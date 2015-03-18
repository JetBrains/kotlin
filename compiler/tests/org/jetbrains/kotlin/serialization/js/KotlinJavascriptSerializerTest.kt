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

import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.JetCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS
import org.jetbrains.kotlin.js.config.EcmaVersion
import org.jetbrains.kotlin.js.config.LibrarySourcesConfigWithCaching
import org.jetbrains.kotlin.jvm.compiler.LoadDescriptorUtil.TEST_PACKAGE_FQNAME
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil
import org.jetbrains.kotlin.test.JetTestUtils
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import org.jetbrains.kotlin.utils.serializer.KotlinJavaScriptSerializer
import java.io.File

public class KotlinJavascriptSerializerTest : TestCaseWithTmpdir() {
    private final val MODULE_NAME = "module"
    private final val BASE_DIR = "compiler/testData/serialization"

    private fun doTest(fileName: String, metaFileDir: File = tmpdir) {
        val source = "$BASE_DIR/$fileName"
        val metaFile = File(metaFileDir, "${FileUtil.getNameWithoutExtension(fileName)}.meta.js")

        val srcDirs = listOf(File(source))

        val configuration = CompilerConfiguration()
        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)

        val sourceRoots = srcDirs map { it.path }
        configuration.put(CommonConfigurationKeys.SOURCE_ROOTS_KEY, sourceRoots)

        serialize(configuration, metaFile)
        val module = deserialize(metaFile)

        RecursiveDescriptorComparator.validateAndCompareDescriptorWithFile(
                module.getPackage(TEST_PACKAGE_FQNAME)!!,
                RecursiveDescriptorComparator.DONT_INCLUDE_METHODS_OF_OBJECT,
                File(source.replace(".kt", ".txt"))
        )
    }

    private fun serialize(configuration: CompilerConfiguration, metaFile: File) {
        val rootDisposable = Disposer.newDisposable()
        try {
            val environment = JetCoreEnvironment.createForTests(rootDisposable, configuration, EnvironmentConfigFiles.JS_CONFIG_FILES)
            val files = environment.getSourceFiles()
            val config = LibrarySourcesConfigWithCaching(environment.getProject(), MODULE_NAME, EcmaVersion.defaultVersion(), false, true, false)
            val analysisResult = TopDownAnalyzerFacadeForJS.analyzeFiles(files, config)
            KotlinJavaScriptSerializer().serialize(MODULE_NAME, analysisResult.moduleDescriptor, metaFile)
        }
        finally {
            Disposer.dispose(rootDisposable)
        }
    }

    private fun deserialize(metaFile: File): ModuleDescriptorImpl {
        val module = JetTestUtils.createEmptyModule("<$MODULE_NAME>")
        val metadata = KotlinJavascriptMetadataUtils.loadMetadata(metaFile)
        assert(metadata.size() == 1)

        val provider = CompositePackageFragmentProvider(KotlinJavascriptSerializationUtil.getPackageFragmentProviders(module, metadata[0].body))

        module.initialize(provider)
        module.addDependencyOnModule(module)
        module.addDependencyOnModule(KotlinBuiltIns.getInstance().getBuiltInsModule())
        module.seal()

        return module
    }

    fun testDynamicConstants() {
        doTest("js/dynamicConstants.kt")
    }

    fun testSimple() {
        doTest("builtinsSerializer/simple.kt")
    }

    fun testCompileTimeConstants() {
        doTest("builtinsSerializer/compileTimeConstants.kt")
    }

    fun testAnnotationTargets() {
        doTest("builtinsSerializer/annotationTargets.kt")
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
}
