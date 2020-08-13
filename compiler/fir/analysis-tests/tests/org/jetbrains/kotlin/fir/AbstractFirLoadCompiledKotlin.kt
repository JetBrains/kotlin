/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.jvm.compiler.LoadDescriptorUtil.compileKotlinToDirAndGetModule
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.KotlinTestUtils.newConfiguration
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.File

abstract class AbstractFirLoadCompiledKotlin : AbstractFirResolveWithSessionTestCase() {
    protected lateinit var tmpdir: File

    override fun setUp() {
        super.setUp()
        tmpdir = KotlinTestUtils.tmpDirForTest(this)
    }

    override fun createEnvironment(): KotlinCoreEnvironment {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_NO_RUNTIME)
    }

    fun doTest(path: String) {
        val moduleDescriptor = compileKtFileToTmpDir(path)

        val packageFqName = FqName("test")
        val allCompiledNames = DescriptorUtils.getAllDescriptors(moduleDescriptor.getPackage(packageFqName).memberScope)
            .mapTo(sortedSetOf()) { it.name }

        val configuration = newConfiguration(ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK, listOf(tmpdir), emptyList<File>())
        val environment = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

        prepareProjectExtensions(environment.project)
        val sessionWithDependency = createSession(environment, GlobalSearchScope.EMPTY_SCOPE)

        checkPackageContent(sessionWithDependency, packageFqName, allCompiledNames, path)
    }

    private fun compileKtFileToTmpDir(path: String): ModuleDescriptor {
        val file = File(path)

        val configuration = newConfiguration(ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK, emptyList(), emptyList<File>())
        val environment = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

        return compileKotlinToDirAndGetModule(listOf(file), tmpdir, environment)
    }

    private fun checkPackageContent(
        session: FirSession,
        packageFqName: FqName,
        declarationNames: Set<Name>,
        testDataPath: String
    ) {
        val provider = session.firSymbolProvider

        val builder = StringBuilder()
        val firRenderer = FirRenderer(builder)

        for (name in declarationNames) {
            for (symbol in provider.getTopLevelCallableSymbols(packageFqName, name)) {
                symbol.fir.accept(firRenderer)
                builder.appendLine()
            }
        }

        for (name in declarationNames) {
            val classLikeSymbol = provider.getClassLikeSymbolByFqName(ClassId.topLevel(packageFqName.child(name))) ?: continue
            classLikeSymbol.fir.accept(firRenderer)
            builder.appendLine()
        }

        val testDataDirectoryPath =
            "compiler/fir/analysis-tests/testData/loadCompiledKotlin/" +
                    testDataPath
                        .removePrefix("compiler/testData/loadJava/compiledKotlin/")
                        .removeSuffix(File(testDataPath).name)
        File(testDataDirectoryPath).mkdirs()

        KotlinTestUtils.assertEqualsToFile(
            File(testDataDirectoryPath + getTestName(false) + ".txt"),
            builder.toString()
        )
    }

}
