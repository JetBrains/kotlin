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

package org.jetbrains.kotlin.serialization

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.frontend.java.di.createContainerForTopDownAnalyzerForJvm
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.jvm.compiler.LoadDescriptorUtil
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.structure.reflect.classId
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator
import java.io.File
import java.net.URLClassLoader

public abstract class AbstractLocalClassProtoTest : TestCaseWithTmpdir() {
    protected fun doTest(filename: String) {
        val source = File(filename)
        LoadDescriptorUtil.compileKotlinToDirAndGetAnalysisResult(listOf(source), tmpdir, getTestRootDisposable(), ConfigurationKind.ALL)

        val classNameSuffix = InTextDirectivesUtils.findStringWithPrefixes(source.readText(), "// CLASS_NAME_SUFFIX: ")
                              ?: error("CLASS_NAME_SUFFIX directive not found in test data")

        val classLoader = URLClassLoader(arrayOf(tmpdir.toURI().toURL()), ForTestCompileRuntime.runtimeJarClassLoader())

        val classFile = tmpdir.listFiles().singleOrNull { it.getPath().endsWith("$classNameSuffix.class") }
                        ?: error("Local class with suffix `$classNameSuffix` is not found in: ${tmpdir.listFiles().toList()}")
        val clazz = classLoader.loadClass(classFile.name.substringBeforeLast(".class"))
        assertHasAnnotationData(clazz)

        val environment = KotlinCoreEnvironment.createForTests(
                getTestRootDisposable(),
                JetTestUtils.compilerConfigurationForTests(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, tmpdir),
                EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
        val moduleContext = TopDownAnalyzerFacadeForJVM.createContextWithSealedModule(environment.project)
        val providerFactory = FileBasedDeclarationProviderFactory(moduleContext.storageManager, emptyList())

        val container = createContainerForTopDownAnalyzerForJvm(
                moduleContext, CliLightClassGenerationSupport.NoScopeRecordCliBindingTrace(),
                providerFactory, GlobalSearchScope.allScope(environment.project), LookupTracker.DO_NOTHING
        )
        moduleContext.initializeModuleContents(container.javaDescriptorResolver.packageFragmentProvider)

        val components = container.deserializationComponentsForJava.components

        val classDescriptor = components.classDeserializer.deserializeClass(clazz.classId)
                              ?: error("Class is not resolved: $clazz (classId = ${clazz.classId})")

        RecursiveDescriptorComparator.validateAndCompareDescriptorWithFile(
                classDescriptor,
                RecursiveDescriptorComparator.DONT_INCLUDE_METHODS_OF_OBJECT,
                JetTestUtils.replaceExtension(source, "txt")
        )
    }

    private fun assertHasAnnotationData(clazz: Class<*>) {
        @suppress("UNCHECKED_CAST")
        val annotation = clazz.getAnnotation(
                clazz.getClassLoader().loadClass(JvmAnnotationNames.KOTLIN_CLASS.asString()) as Class<Annotation>
        )
        assert(annotation != null) { "KotlinClass annotation is not found for class $clazz" }

        val kindMethod = annotation.annotationType().getDeclaredMethod("kind")
        val kind = kindMethod(annotation)
        assert(kind.toString() != JvmAnnotationNames.KotlinClass.Kind.CLASS.toString()) {
            "'kind' should not be CLASS: $clazz (was $kind)"
        }
    }
}
