/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.KotlinTestWithEnvironmentManagement
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KotlinClassFinderTest : KotlinTestWithEnvironmentManagement() {
    fun testAbsentClass() {
        val tmpdir = KotlinTestUtils.tmpDirForTest(this)

        val environment = createEnvironment(tmpdir)
        val project = environment.project

        val className = "test.A.B.D"
        val psiClass = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project))
        assertNull(psiClass, "Class is expected to be null, there should be no exceptions too.")
    }

    fun testNestedClass() {
        val tmpdir = KotlinTestUtils.tmpDirForTest(this)
        KotlinTestUtils.compileKotlinWithJava(
                listOf(), listOf(File("compiler/testData/kotlinClassFinder/nestedClass.kt")), tmpdir, testRootDisposable, null
        )

        val environment = createEnvironment(tmpdir)
        val project = environment.project

        val className = "test.A.B.C"
        val psiClass = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project))
        assertNotNull(psiClass, "Psi class not found for $className")
        assertTrue(psiClass !is KtLightClass, "Kotlin light classes are not not expected")

        val binaryClass = VirtualFileFinder.SERVICE.getInstance(project).findKotlinClass(JavaClassImpl(psiClass!!))
        assertNotNull(binaryClass, "No binary class for $className")

        assertEquals("test/A.B.C", binaryClass?.classId?.toString())
    }

    private fun createEnvironment(tmpdir: File?): KotlinCoreEnvironment {
        return KotlinCoreEnvironment.createForTests(
                testRootDisposable,
                KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, tmpdir),
                EnvironmentConfigFiles.JVM_CONFIG_FILES
        ).apply {
            // Activate Kotlin light class finder
            JvmResolveUtil.analyze(this)
        }
    }
}
