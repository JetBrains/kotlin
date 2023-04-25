/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.jvm.compiler

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.javac.components.JavacBasedClassFinder
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.load.kotlin.findKotlinClass
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.KotlinTestWithEnvironmentManagement
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class KotlinJavacBasedClassFinderTest : KotlinTestWithEnvironmentManagement() {
    fun testAbsentClass() {
        val tmpdir = KotlinTestUtils.tmpDirForTest(this)

        val environment = createEnvironment(tmpdir)
        val project = environment.project

        val classFinder = createClassFinder(project)

        val className = "test.A.B.D"

        val found = classFinder.findClass(ClassId.topLevel(FqName(className)))
        assertNull(found, "Class is expected to be null, there should be no exceptions too.")
    }

    fun testNestedClass() {
        val tmpdir = KotlinTestUtils.tmpDirForTest(this)
        KotlinTestUtils.compileKotlinWithJava(
                listOf(), listOf(File("compiler/testData/kotlinClassFinder/nestedClass.kt")), tmpdir, testRootDisposable, null
        )

        val environment = createEnvironment(tmpdir)
        val project = environment.project

        val classFinder = createClassFinder(project)

        val className = "test.A.B.C"
        val classId = ClassId(FqName("test"), FqName("A.B.C"), false)
        val found = classFinder.findClass(classId)
        assertNotNull(found, "Class not found for $className")

        val binaryClass = VirtualFileFinder.SERVICE.getInstance(project).findKotlinClass(found, JvmMetadataVersion.INSTANCE)
        assertNotNull(binaryClass, "No binary class for $className")

        assertEquals("test/A.B.C", binaryClass.classId.toString())
    }

    private fun createClassFinder(project: Project) = JavacBasedClassFinder().apply {
        this::class.java.superclass.getDeclaredField("project")?.let {
            it.isAccessible = true
            it.set(this, project)
        }
        setScope(GlobalSearchScope.allScope(project))

        val javacField = this::class.java.getDeclaredField("javac")
        javacField.isAccessible = true
        javacField.set(this, JavacWrapper.getInstance(project))

        val javaSearchScopeField = this::class.java.superclass.getDeclaredField("javaSearchScope")
        javaSearchScopeField.isAccessible = true
        javaSearchScopeField.set(this, GlobalSearchScope.allScope(project))
    }

    private fun createEnvironment(tmpdir: File, files: List<File> = emptyList()): KotlinCoreEnvironment {
        return KotlinCoreEnvironment.createForTests(
            testRootDisposable,
            KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, tmpdir),
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        ).apply {
            registerJavac(files)
            // Activate Kotlin light class finder
            JvmResolveUtil.analyze(this)
        }
    }
}
