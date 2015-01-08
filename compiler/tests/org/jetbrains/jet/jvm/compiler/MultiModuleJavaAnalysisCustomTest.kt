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

import org.jetbrains.jet.lang.resolve.java.JvmAnalyzerFacade
import org.jetbrains.jet.context.GlobalContext
import org.jetbrains.jet.JetTestUtils
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.jet.lang.resolve.java.JvmPlatformParameters
import org.jetbrains.jet.lang.psi.JetFile
import java.io.File
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.descriptors.*
import org.junit.Assert
import org.jetbrains.kotlin.cli.jvm.compiler.JetCoreEnvironment
import org.jetbrains.jet.config.CompilerConfiguration
import org.jetbrains.kotlin.cli.jvm.JVMConfigurationKeys
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.resolve.java.JvmResolverForModule
import org.jetbrains.jet.analyzer.ResolverForProject
import org.jetbrains.jet.analyzer.ModuleInfo
import java.util.HashMap
import org.jetbrains.jet.analyzer.ModuleContent
import org.jetbrains.jet.lang.types.ErrorUtils
import org.jetbrains.jet.lang.resolve.descriptorUtil.module
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles

public class MultiModuleJavaAnalysisCustomTest : UsefulTestCase() {

    private class TestModule(val _name: String, val kotlinFiles: List<JetFile>, val javaFilesScope: GlobalSearchScope,
                             val _dependencies: TestModule.() -> List<TestModule>) :
            ModuleInfo {
        override fun dependencies() = _dependencies()
        override val name = Name.special("<$_name>")
    }

    fun testJavaEntitiesBelongToCorrectModule() {
        val moduleDirs = File(PATH_TO_TEST_ROOT_DIR).listFiles { it.isDirectory() }!!
        val environment = createEnvironment(moduleDirs)
        val modules = setupModules(environment, moduleDirs)
        val resolverForProject = JvmAnalyzerFacade.setupResolverForProject(
                GlobalContext(), environment.getProject(), modules,
                { m -> ModuleContent(m.kotlinFiles, m.javaFilesScope) },
                JvmPlatformParameters {
                    javaClass ->
                    val moduleName = javaClass.getName().asString().toLowerCase().first().toString()
                    modules.first { it._name == moduleName }
                }
        )

        performChecks(resolverForProject, modules)
    }

    private fun createEnvironment(moduleDirs: Array<File>): JetCoreEnvironment {
        val configuration = CompilerConfiguration()
        configuration.addAll(JVMConfigurationKeys.CLASSPATH_KEY, moduleDirs.toList())
        return JetCoreEnvironment.createForTests(getTestRootDisposable()!!, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    }

    private fun setupModules(environment: JetCoreEnvironment, moduleDirs: Array<File>): List<TestModule> {
        val project = environment.getProject()
        val modules = HashMap<String, TestModule>()
        for (dir in moduleDirs) {
            val name = dir.getName()
            val kotlinFiles = JetTestUtils.loadToJetFiles(environment, dir.listFiles { it.extension == "kt" }?.toList().orEmpty())
            val javaFilesScope = object : DelegatingGlobalSearchScope(GlobalSearchScope.allScope(project)) {
                override fun contains(file: VirtualFile): Boolean {
                    if (file !in myBaseScope!!) return false
                    if (file.isDirectory()) return true
                    return file.getParent()!!.getParent()!!.getName() == name
                }
            }
            modules[name] = TestModule(name, kotlinFiles, javaFilesScope) {
                when (this._name) {
                    "a" -> listOf(this)
                    "b" -> listOf(this, modules["a"]!!)
                    "c" -> listOf(this, modules["b"]!!, modules["a"]!!)
                    else -> throw IllegalStateException("$_name")
                }
            }
        }
        return modules.values().toList()
    }

    private fun performChecks(resolverForProject: ResolverForProject<TestModule, JvmResolverForModule>, modules: List<TestModule>) {
        modules.forEach {
            module ->
            val moduleDescriptor = resolverForProject.descriptorForModule(module)

            checkClassInPackage(moduleDescriptor, "test", "Kotlin${module._name.toUpperCase()}")
            checkClassInPackage(moduleDescriptor, "custom", "${module._name.toUpperCase()}Class")
        }
    }

    private fun checkClassInPackage(moduleDescriptor: ModuleDescriptor, packageName: String, className: String) {
        val kotlinPackage = moduleDescriptor.getPackage(FqName(packageName))!!
        val kotlinClassName = Name.identifier(className)
        val kotlinClass = kotlinPackage.getMemberScope().getClassifier(kotlinClassName) as ClassDescriptor
        checkClass(kotlinClass)
    }

    private fun checkClass(classDescriptor: ClassDescriptor) {
        classDescriptor.getDefaultType().getMemberScope().getDescriptors().filterIsInstance<CallableDescriptor>().forEach {
            checkCallable(it)
        }

        checkSupertypes(classDescriptor)
    }

    private fun checkCallable(callable: CallableDescriptor) {
        val name = callable.getName().asString()
        if (name in setOf("equals", "hashCode", "toString")) return

        val returnType = callable.getReturnType()!!
        if (!KotlinBuiltIns.isUnit(returnType)) {
            checkDescriptor(returnType.getConstructor().getDeclarationDescriptor()!!, callable)
        }

        callable.getValueParameters().map {
            it.getType().getConstructor().getDeclarationDescriptor()!!
        }.forEach { checkDescriptor(it, callable) }

        callable.getAnnotations().map {
            it.getType().getConstructor().getDeclarationDescriptor()!!
        }.forEach { checkDescriptor(it, callable) }
    }

    private fun checkSupertypes(classDescriptor: ClassDescriptor) {
        classDescriptor.getDefaultType().getConstructor().getSupertypes().filter {
            !KotlinBuiltIns.isAnyOrNullableAny(it)
        }.map {
            it.getConstructor().getDeclarationDescriptor()!!
        }.forEach {
            checkDescriptor(it, classDescriptor)
        }
    }

    private fun checkDescriptor(referencedDescriptor: ClassifierDescriptor, context: DeclarationDescriptor) {
        assert(!ErrorUtils.isError(referencedDescriptor), "Error descriptor: $referencedDescriptor")

        val descriptorName = referencedDescriptor.getName().asString()
        val expectedModuleName = "<${descriptorName.toLowerCase().first().toString()}>"
        val moduleName = referencedDescriptor.module.getName().asString()
        Assert.assertEquals(
                "Java class $descriptorName in $context should be in module $expectedModuleName, but instead was in $moduleName",
                expectedModuleName, moduleName
        )
    }

    class object {
        val PATH_TO_TEST_ROOT_DIR = "compiler/testData/multiModule/java/custom"
    }
}
