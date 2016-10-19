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

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.jvm.compiler.LoadDescriptorUtil
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.structure.reflect.classId
import org.jetbrains.kotlin.load.kotlin.DeserializationComponentsForJava
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator
import java.io.File
import java.net.URLClassLoader

abstract class AbstractLocalClassProtoTest : TestCaseWithTmpdir() {
    protected fun doTest(filename: String) {
        val source = File(filename)

        KotlinTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(testRootDisposable).let { environment ->
            LoadDescriptorUtil.compileKotlinToDirAndGetModule(listOf(source), tmpdir, environment)
        }

        val classNameSuffix = InTextDirectivesUtils.findStringWithPrefixes(source.readText(), "// CLASS_NAME_SUFFIX: ")
                              ?: error("CLASS_NAME_SUFFIX directive not found in test data")

        val classLoader = URLClassLoader(arrayOf(tmpdir.toURI().toURL()), ForTestCompileRuntime.runtimeAndReflectJarClassLoader())

        val classFile = tmpdir.walkTopDown().singleOrNull { it.path.endsWith("$classNameSuffix.class") }
                        ?: error("Local class with suffix `$classNameSuffix` is not found in: ${tmpdir.listFiles().toList()}")
        val clazz = classLoader.loadClass(classFile.toRelativeString(tmpdir).substringBeforeLast(".class").replace('/', '.').replace('\\', '.'))
        assertHasAnnotationData(clazz)

        val configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, tmpdir).apply {
            // This is needed because otherwise local classes, being binary classes, are not resolved via source module's container.
            // They could only be resolved via the container of the _dependency_ module.
            // This can be improved as soon as there's an API to get the container of the dependency module.
            put(JVMConfigurationKeys.USE_SINGLE_MODULE, true)
        }
        val environment = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

        val components = JvmResolveUtil.createContainer(environment).get<DeserializationComponentsForJava>().components

        val classDescriptor = components.classDeserializer.deserializeClass(clazz.classId)
                              ?: error("Class is not resolved: $clazz (classId = ${clazz.classId})")

        RecursiveDescriptorComparator.validateAndCompareDescriptorWithFile(
                classDescriptor,
                RecursiveDescriptorComparator.DONT_INCLUDE_METHODS_OF_OBJECT,
                KotlinTestUtils.replaceExtension(source, "txt")
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun assertHasAnnotationData(clazz: Class<*>) {
        checkNotNull(clazz.getAnnotation(
                clazz.classLoader.loadClass(JvmAnnotationNames.METADATA_FQ_NAME.asString()) as Class<Annotation>
        )) { "Metadata annotation is not found for class $clazz" }
    }
}
