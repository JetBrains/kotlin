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

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.K1_DEPRECATION_WARNING
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsPackageFragmentProvider
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsSignatures
import org.jetbrains.kotlin.cli.jvm.compiler.AllJavaSourcesInProjectScope
import org.jetbrains.kotlin.cli.jvm.compiler.CliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ContextForNewModule
import org.jetbrains.kotlin.context.MutableModuleContext
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.resolveClassByFqName
import org.jetbrains.kotlin.frontend.java.di.createContainerForLazyResolveWithJava
import org.jetbrains.kotlin.frontend.java.di.initJvmBuiltInsForTopDownAnalysis
import org.jetbrains.kotlin.frontend.java.di.initialize
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.load.java.lazy.ModuleClassResolver
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.impl.VirtualFileBoundJavaClass
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.computeJvmDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyPublicApi
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.resolve.jvm.multiplatform.OptionalAnnotationPackageFragmentProvider
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class AdditionalBuiltInsMembersSignatureListsTest {
    private val testRootDisposable: Disposable = Disposer.newDisposable()
    private val environment: KotlinCoreEnvironment = createEnvironment()

    private fun createEnvironment(): KotlinCoreEnvironment {
        @OptIn(CoreEnvironmentDeprecation::class)
        return KotlinCoreEnvironment.createForTests(
            testRootDisposable,
            KotlinTestUtils.newConfiguration(ConfigurationKind.JDK_ONLY, TestJdkKind.FULL_JDK_21, KtTestUtil.getAnnotationsJar()),
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
    }

    @Test
    fun testAllListedSignaturesExistInJdk() {
        val module = TopDownAnalyzerFacadeForJVM.analyze(environment).moduleDescriptor as ModuleDescriptorImpl

        val hiddenSignatures =
            JvmBuiltInsSignatures.HIDDEN_METHOD_SIGNATURES +
                    JvmBuiltInsSignatures.MUTABLE_METHOD_SIGNATURES +
                    JvmBuiltInsSignatures.HIDDEN_CONSTRUCTOR_SIGNATURES +
                    JvmBuiltInsSignatures.VISIBLE_METHOD_SIGNATURES +
                    JvmBuiltInsSignatures.VISIBLE_CONSTRUCTOR_SIGNATURES

        val groupedByInternalName = hiddenSignatures.groupBy({ it.split(".")[0] }) { it.split(".")[1] }

        groupedByInternalName.entries.forEach { it ->
            val [internalName, jvmDescriptors] = it
            val classDescriptor =
                module.resolveClassByFqName(
                    JvmClassName.byInternalName(internalName).fqNameForClassNameWithoutDollars, NoLookupLocation.FROM_TEST
                )!!

            val scope = classDescriptor.unsubstitutedMemberScope

            val signaturesFromLaterJdkReleases = SIGNATURES_FROM_LATER_JDK_RELEASES[internalName] ?: emptySet()

            for (jvmDescriptor in jvmDescriptors) {
                if (jvmDescriptor in signaturesFromLaterJdkReleases) continue

                val stringName = jvmDescriptor.split("(")[0]
                val functions =
                    if (stringName == "<init>")
                        classDescriptor.constructors
                    else
                        scope.getContributedFunctions(Name.identifier(stringName), NoLookupLocation.FROM_TEST)

                val matchingFunction = functions.singleOrNull {
                    it.isEffectivelyPublicApi && it.computeJvmDescriptor() == jvmDescriptor
                }
                if (matchingFunction == null) fail("Expected single function with signature $jvmDescriptor in $internalName")
            }
        }
    }

    /**
     * This property can be used to exclude particular signatures
     * from the [testAllListedSignaturesExistInJdk] test
     * if they were introduced in later JDK releases than the test is currently configured to use.
     * It might be helpful in case the test can't be updated to these later JDK releases just yet
     * (for example, if the JDK releases in question are not released as stable yet).
     *
     * initialization example: `mapOf("java/util/List" to setOf("addFirst(Ljava/lang/Object;)V"))`
     */
    private val SIGNATURES_FROM_LATER_JDK_RELEASES: Map<String, Set<String>> = emptyMap()
}

@K1Deprecation
private object TopDownAnalyzerFacadeForJVM {
    fun analyze(environment: KotlinCoreEnvironment): AnalysisResult {
        return analyzeFilesWithJavaIntegration(
            project = environment.project,
            trace = CliBindingTrace(environment.project),
            configuration = environment.configuration,
            packagePartProvider = environment::createPackagePartProvider
        )
    }

    fun analyzeFilesWithJavaIntegration(
        project: Project,
        trace: BindingTrace,
        configuration: CompilerConfiguration,
        packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
    ): AnalysisResult {
        val explicitCompilerEnvironment: TargetEnvironment = CompilerEnvironment

        @Suppress("DEPRECATION_ERROR")
        val container = createContainer(
            project, trace, configuration, packagePartProvider, explicitCompilerEnvironment,
        )

        val module = container.get<ModuleDescriptor>()
        return AnalysisResult.success(trace.bindingContext, module)
    }

    @Deprecated(K1_DEPRECATION_WARNING, level = ERROR)
    private fun createContainer(
        project: Project,
        trace: BindingTrace,
        configuration: CompilerConfiguration,
        packagePartProvider: (GlobalSearchScope) -> PackagePartProvider,
        targetEnvironment: TargetEnvironment = CompilerEnvironment,
    ): ComponentProvider {
        val sourceModuleSearchScope = AllJavaSourcesInProjectScope(project)
        val jvmTarget = configuration.get(JVMConfigurationKeys.JVM_TARGET, JvmTarget.DEFAULT)
        val languageVersionSettings = configuration.languageVersionSettings
        val jvmPlatform = JvmPlatforms.jvmPlatformByTargetVersion(jvmTarget)

        val moduleContext = createModuleContext(project, configuration, jvmPlatform)

        val storageManager = moduleContext.storageManager
        val module = moduleContext.module
        trace.record(BindingContext.COMPILER_CONFIGURATION, module, configuration)

        val lookupTracker = configuration.get(CommonConfigurationKeys.LOOKUP_TRACKER) ?: LookupTracker.DO_NOTHING
        val expectActualTracker = configuration.get(CommonConfigurationKeys.EXPECT_ACTUAL_TRACKER) ?: ExpectActualTracker.DoNothing
        val inlineConstTracker = configuration.get(CommonConfigurationKeys.INLINE_CONST_TRACKER) ?: InlineConstTracker.DoNothing
        val enumWhenTracker = configuration.get(CommonConfigurationKeys.ENUM_WHEN_TRACKER) ?: EnumWhenTracker.DoNothing

        val moduleClassResolver = SourceOrBinaryModuleClassResolver(sourceModuleSearchScope)

        val fallbackBuiltIns = JvmBuiltIns(storageManager, FALLBACK).apply {
            initialize(builtInsModule, languageVersionSettings)
        }.builtInsModule

        val dependencyModule = run {
            val dependenciesContext = ContextForNewModule(
                moduleContext, Name.special("<dependencies of ${configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME)}>"),
                module.builtIns, jvmPlatform
            )

            // Scope for the dependency module contains everything except files present in the scope for the source module
            val dependencyScope = GlobalSearchScope.notScope(sourceModuleSearchScope)

            val dependenciesContainer = createContainerForLazyResolveWithJava(
                jvmPlatform,
                dependenciesContext, trace, EMPTY, dependencyScope, moduleClassResolver,
                targetEnvironment, lookupTracker, expectActualTracker, inlineConstTracker, enumWhenTracker,
                packagePartProvider(dependencyScope), languageVersionSettings,
                useBuiltInsProvider = true,
                configureJavaClassFinder = null,
                implicitsResolutionFilter = null,
            )

            moduleClassResolver.compiledCodeResolver = dependenciesContainer.get()

            dependenciesContext.setDependencies(listOf(dependenciesContext.module, fallbackBuiltIns))
            dependenciesContext.initializeModuleContents(
                CompositePackageFragmentProvider(
                    listOf(
                        moduleClassResolver.compiledCodeResolver.packageFragmentProvider,
                        dependenciesContainer.get<JvmBuiltInsPackageFragmentProvider>(),
                        dependenciesContainer.get<OptionalAnnotationPackageFragmentProvider>()
                    ),
                    "CompositeProvider@TopDownAnalyzerForJvm for dependencies ${dependenciesContext.module}"
                )
            )
            dependenciesContext.module
        }

        val partProvider = packagePartProvider(sourceModuleSearchScope)

        // Note that it's necessary to create container for sources _after_ creation of container for dependencies because
        // CliLightClassGenerationSupport#initialize is invoked when container is created, so only the last module descriptor is going
        // to be stored in CliLightClassGenerationSupport, and it better be the source one (otherwise light classes would not be found)
        // TODO: get rid of duplicate invocation of CodeAnalyzerInitializer#initialize, or refactor CliLightClassGenerationSupport
        val container = createContainerForLazyResolveWithJava(
            jvmPlatform,
            moduleContext, trace, FileBasedDeclarationProviderFactory(storageManager, emptyList()), sourceModuleSearchScope, moduleClassResolver,
            targetEnvironment, lookupTracker, expectActualTracker, inlineConstTracker, enumWhenTracker,
            partProvider, languageVersionSettings,
            useBuiltInsProvider = true,
            configureJavaClassFinder = null,
            javaClassTracker = null,
            implicitsResolutionFilter = null,
        ).apply {
            initJvmBuiltInsForTopDownAnalysis()
        }

        moduleClassResolver.sourceCodeResolver = container.get()
        val additionalProviders = ArrayList<PackageFragmentProvider>()

        additionalProviders.add(container.get<JavaDescriptorResolver>().packageFragmentProvider)

        // TODO: consider putting extension package fragment providers into the dependency module
        PackageFragmentProviderExtension.getInstances(project).mapNotNullTo(additionalProviders) { extension ->
            extension.getPackageFragmentProvider(project, module, storageManager, trace, null, lookupTracker)
        }

        // TODO: remove dependencyModule from friends
        module.setDependencies(
            listOf(module, dependencyModule, fallbackBuiltIns) ,
            setOf(dependencyModule),
        )
        module.initialize(
            CompositePackageFragmentProvider(
                listOf(
                    container.get<KotlinCodeAnalyzer>().packageFragmentProvider,
                    container.get<OptionalAnnotationPackageFragmentProvider>()
                ) + additionalProviders,
                "CompositeProvider@TopDownAnalzyerForJvm for $module"
            )
        )

        return container
    }

    private class SourceOrBinaryModuleClassResolver(private val sourceScope: GlobalSearchScope) : ModuleClassResolver {
        lateinit var compiledCodeResolver: JavaDescriptorResolver
        lateinit var sourceCodeResolver: JavaDescriptorResolver

        override fun resolveClass(javaClass: JavaClass): ClassDescriptor? {
            val resolver = if (javaClass is VirtualFileBoundJavaClass && javaClass.isFromSourceCodeInScope(sourceScope))
                sourceCodeResolver
            else
                compiledCodeResolver
            return resolver.resolveClass(javaClass)
        }
    }

    private fun createModuleContext(
        project: Project,
        configuration: CompilerConfiguration,
        platform: TargetPlatform?,
    ): MutableModuleContext {
        val projectContext = ProjectContext(project, "TopDownAnalyzer for JVM")
        val builtIns = JvmBuiltIns(projectContext.storageManager, FROM_DEPENDENCIES)
        return ContextForNewModule(
            projectContext, Name.special("<${configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME)}>"),
            builtIns, platform, emptyMap()
        ).apply {
            builtIns.builtInsModule = module
        }
    }
}
