/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.jvm.compiler.AbstractLoadJavaTest
import org.jetbrains.kotlin.jvm.compiler.LoadDescriptorUtil.compileKotlinToDirAndGetModule
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.KotlinTestUtils.newConfiguration
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.File

abstract class AbstractFirLoadCompiledKotlin : AbstractFirLoadBinariesTest() {
    protected lateinit var tmpdir: File

    override fun setUp() {
        super.setUp()
        tmpdir = KotlinTestUtils.tmpDirForTest(this)
    }

    override fun createEnvironment(): KotlinCoreEnvironment {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_NO_RUNTIME)
    }

    @OptIn(ObsoleteTestInfrastructure::class)
    fun doTest(path: String) {
        val moduleDescriptor = compileKtFileToTmpDir(path)

        val packageFqName = FqName("test")

        val configuration = newConfiguration(ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK, listOf(tmpdir), emptyList<File>())
        val environment = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

        prepareProjectExtensions(environment.project)
        val sessionWithDependency = createSessionForTests(environment, GlobalSearchScope.EMPTY_SCOPE)

        val testDataDirectoryPath =
            "compiler/fir/analysis-tests/testData/loadCompiledKotlin/" +
                    path
                        .removePrefix("compiler/testData/loadJava/compiledKotlin/")
                        .removeSuffix(File(path).name)
        File(testDataDirectoryPath).mkdirs()

        checkPackageContent(sessionWithDependency, packageFqName, moduleDescriptor, "$testDataDirectoryPath${getTestName(false)}.txt")
    }

    private fun compileKtFileToTmpDir(path: String): ModuleDescriptor {
        val file = File(path)

        val configuration = newConfiguration(ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK, emptyList(), emptyList<File>())
        AbstractLoadJavaTest.updateConfigurationWithDirectives(file.readText(), configuration)
        val environment = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

        return compileKotlinToDirAndGetModule(listOf(file), tmpdir, environment)
    }
}
