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

package org.jetbrains.kotlin.serialization.builtins

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.*
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.PackagePartProvider
import org.jetbrains.kotlin.frontend.java.di.createContainerForTopDownSingleModuleAnalyzerForJvm
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.JvmBuiltIns
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier
import org.jetbrains.kotlin.renderer.OverrideRenderingPolicy
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator
import java.io.File

abstract class AbstractBuiltInsWithJDKMembersTest : KotlinTestWithEnvironment() {
    override fun createEnvironment(): KotlinCoreEnvironment {
        return createEnvironmentWithJdk(ConfigurationKind.JDK_ONLY, TestJdkKind.FULL_JDK)
    }

    protected fun doTest(builtinVersionName: String) {
        val configuration = RecursiveDescriptorComparator.RECURSIVE_ALL.includeMethodsOfKotlinAny(false).withRenderer(
                DescriptorRenderer.withOptions {
                    withDefinedIn = false
                    overrideRenderingPolicy = OverrideRenderingPolicy.RENDER_OPEN_OVERRIDE
                    verbose = true
                    includeAnnotationArguments = true
                    modifiers = DescriptorRendererModifier.ALL
                })

        val jvmBuiltIns = JvmBuiltIns(LockBasedStorageManager.NO_LOCKS)
        val emptyModule = KotlinTestUtils.createEmptyModule("<empty>", JvmPlatform, jvmBuiltIns)

        val container = createContainerForTopDownSingleModuleAnalyzerForJvm(
                ModuleContext(emptyModule, environment.project), CliLightClassGenerationSupport.CliBindingTrace(),
                DeclarationProviderFactory.EMPTY, GlobalSearchScope.allScope(environment.project), PackagePartProvider.Empty
        )

        emptyModule.initialize(container.get<JavaDescriptorResolver>().packageFragmentProvider)
        emptyModule.setDependencies(emptyModule)

        val packageFragmentProvider = emptyModule.builtIns.builtInsModule.packageFragmentProvider

        for (packageFqName in listOf<FqName>(BUILT_INS_PACKAGE_FQ_NAME, COLLECTIONS_PACKAGE_FQ_NAME, RANGES_PACKAGE_FQ_NAME)) {
            val loaded = packageFragmentProvider.getPackageFragments(packageFqName).single()
            RecursiveDescriptorComparator.validateAndCompareDescriptorWithFile(
                    loaded, configuration,
                    File("compiler/testData/builtin-classes/$builtinVersionName/" + packageFqName.asString().replace('.', '-') + ".txt"))
        }
    }
}
