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

package org.jetbrains.jet.jvm.compiler

import org.jetbrains.jet.KotlinTestWithEnvironmentManagement
import org.jetbrains.jet.JetTestUtils
import java.io.File
import org.jetbrains.kotlin.cli.jvm.compiler.JetCoreEnvironment
import org.jetbrains.jet.ConfigurationKind
import org.jetbrains.jet.TestJdkKind
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles

public class KotlinClassFinderTest : KotlinTestWithEnvironmentManagement() {
    fun testNestedClass() {
        val tmpdir = JetTestUtils.tmpDirForTest(this)
        JetTestUtils.compileKotlinWithJava(
                listOf(), listOf(File("compiler/testData/kotlinClassFinder/nestedClass.kt")), tmpdir, getTestRootDisposable()!!
        )

        val environment = JetCoreEnvironment.createForTests(getTestRootDisposable()!!,
                                                            JetTestUtils.compilerConfigurationForTests(
                                                                    ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, tmpdir),
                                                            EnvironmentConfigFiles.JVM_CONFIG_FILES)

        val project = environment.getProject()
        val className = "test.A.B.C"
        val psiClass = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project))
        assertNotNull(psiClass, "Psi class not found for $className")

        val binaryClass = VirtualFileFinder.SERVICE.getInstance(project).findKotlinClass(JavaClassImpl(psiClass!!))
        assertNotNull(binaryClass, "No binary class for $className")

        assertEquals("test/A.B.C", binaryClass?.getClassId()?.toString())
    }
}
