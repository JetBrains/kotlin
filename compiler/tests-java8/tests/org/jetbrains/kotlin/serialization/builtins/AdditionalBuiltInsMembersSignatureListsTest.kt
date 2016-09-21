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
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.PackagePartProvider
import org.jetbrains.kotlin.descriptors.resolveClassByFqName
import org.jetbrains.kotlin.frontend.java.di.createContainerForTopDownAnalyzerForJvm
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.kotlin.JvmBuiltInsSettings
import org.jetbrains.kotlin.load.kotlin.computeJvmDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.JvmBuiltIns
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyPublicApi
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment
import org.jetbrains.kotlin.test.TestJdkKind

class AdditionalBuiltInsMembersSignatureListsTest : KotlinTestWithEnvironment() {
    override fun createEnvironment(): KotlinCoreEnvironment {
        return createEnvironmentWithJdk(ConfigurationKind.JDK_ONLY, TestJdkKind.FULL_JDK)
    }

    fun testAllListedSignaturesExistInJdk() {
        val jvmBuiltIns = JvmBuiltIns(LockBasedStorageManager.NO_LOCKS)
        val emptyModule = KotlinTestUtils.createEmptyModule("<empty>", JvmPlatform, jvmBuiltIns)

        val moduleContext = ModuleContext(emptyModule, environment.project)
        val providerFactory = FileBasedDeclarationProviderFactory(moduleContext.storageManager, emptyList())

        val container = createContainerForTopDownAnalyzerForJvm(
                moduleContext, CliLightClassGenerationSupport.CliBindingTrace(), providerFactory,
                GlobalSearchScope.allScope(environment.project), LookupTracker.DO_NOTHING, PackagePartProvider.EMPTY,
                LanguageVersionSettingsImpl.DEFAULT
        )

        emptyModule.initialize(container.javaDescriptorResolver.packageFragmentProvider)
        emptyModule.setDependencies(emptyModule)

        val blackList =
                JvmBuiltInsSettings.BLACK_LIST_METHOD_SIGNATURES +
                JvmBuiltInsSettings.MUTABLE_METHOD_SIGNATURES +
                JvmBuiltInsSettings.BLACK_LIST_CONSTRUCTOR_SIGNATURES +
                JvmBuiltInsSettings.WHITE_LIST_METHOD_SIGNATURES +
                JvmBuiltInsSettings.WHITE_LIST_CONSTRUCTOR_SIGNATURES

        val groupedByInternalName = blackList.groupBy({ it.split(".")[0] }) { it.split(".")[1] }

        groupedByInternalName.entries.forEach {
            it ->
            val (internalName, jvmDescriptors) = it
            val classDescriptor =
                    emptyModule.resolveClassByFqName(
                            JvmClassName.byInternalName(internalName).fqNameForClassNameWithoutDollars, NoLookupLocation.FROM_TEST
                    )!!

            val scope = classDescriptor.unsubstitutedMemberScope

            jvmDescriptors.forEach {
                jvmDescriptor ->
                val stringName = jvmDescriptor.split("(")[0]
                val functions =
                        if (stringName == "<init>")
                            classDescriptor.constructors
                        else
                            scope.getContributedFunctions(Name.identifier(stringName), NoLookupLocation.FROM_TEST)

                functions.singleOrNull {
                    it.isEffectivelyPublicApi && it.computeJvmDescriptor() == jvmDescriptor
                } ?: fail("Expected single function with signature $jvmDescriptor in $internalName")
            }
        }
    }
}
