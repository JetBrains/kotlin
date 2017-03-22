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

import org.jetbrains.kotlin.builtins.BuiltInsPackageFragment
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.*
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.renderer.AnnotationArgumentsRenderingPolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier
import org.jetbrains.kotlin.renderer.OverrideRenderingPolicy
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator
import java.io.File

abstract class AbstractBuiltInsWithJDKMembersTest : KotlinTestWithEnvironment() {
    private val configuration = RecursiveDescriptorComparator.RECURSIVE_ALL.includeMethodsOfKotlinAny(false).withRenderer(
            DescriptorRenderer.withOptions {
                withDefinedIn = false
                overrideRenderingPolicy = OverrideRenderingPolicy.RENDER_OPEN_OVERRIDE
                verbose = true
                annotationArgumentsRenderingPolicy = AnnotationArgumentsRenderingPolicy.UNLESS_EMPTY
                modifiers = DescriptorRendererModifier.ALL
            })

    override fun createEnvironment(): KotlinCoreEnvironment =
            createEnvironmentWithJdk(ConfigurationKind.JDK_ONLY, testJdkKind)

    protected open val testJdkKind: TestJdkKind
        get() = TestJdkKind.FULL_JDK

    protected fun doTest(builtinVersionName: String) {
        val module = JvmResolveUtil.analyze(environment).moduleDescriptor as ModuleDescriptorImpl

        for (packageFqName in listOf(BUILT_INS_PACKAGE_FQ_NAME, COLLECTIONS_PACKAGE_FQ_NAME, RANGES_PACKAGE_FQ_NAME)) {
            val loaded =
                    module.packageFragmentProvider.getPackageFragments(packageFqName).filterIsInstance<BuiltInsPackageFragment>().single()
            RecursiveDescriptorComparator.validateAndCompareDescriptorWithFile(
                    loaded, configuration,
                    File("compiler/testData/builtin-classes/$builtinVersionName/" + packageFqName.asString().replace('.', '-') + ".txt")
            )
        }
    }
}
