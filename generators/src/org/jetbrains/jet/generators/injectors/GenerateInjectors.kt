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

package org.jetbrains.jet.generators.injectors

import com.intellij.openapi.project.Project
import org.jetbrains.jet.context.GlobalContext
import org.jetbrains.jet.context.GlobalContextImpl
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import org.jetbrains.jet.lang.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.jet.lang.resolve.*
import org.jetbrains.jet.lang.resolve.java.JavaClassFinderImpl
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver
import org.jetbrains.jet.lang.resolve.java.resolver.*
import org.jetbrains.jet.lang.resolve.java.sam.SamConversionResolverImpl
import org.jetbrains.jet.lang.resolve.kotlin.VirtualFileFinder
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession
import org.jetbrains.jet.lang.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices
import org.jetbrains.jet.di.*
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingComponents
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils
import org.jetbrains.jet.lang.resolve.calls.CallResolver
import org.jetbrains.jet.lang.resolve.java.structure.impl.JavaPropertyInitializerEvaluatorImpl
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.jet.lang.resolve.java.lazy.ModuleClassResolver
import org.jetbrains.jet.lang.resolve.kotlin.DeserializationComponentsForJava
import org.jetbrains.jet.lang.resolve.java.lazy.SingleModuleClassResolver
import org.jetbrains.jet.lang.resolve.kotlin.VirtualFileFinderFactory
import org.jetbrains.jet.lang.resolve.java.TopDownAnalyzerFacadeForJVM
import org.jetbrains.jet.lang.resolve.kotlin.JavaDeclarationCheckerProvider
import org.jetbrains.jet.lang.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.jet.lang.resolve.java.JavaFlexibleTypeCapabilitiesProvider
import org.jetbrains.jet.context.LazyResolveToken
import org.jetbrains.jet.lang.resolve.java.JavaLazyAnalyzerPostConstruct
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolverPostConstruct
import org.jetbrains.jet.lang.resolve.lazy.ScopeProvider
import org.jetbrains.k2js.resolve.KotlinJsDeclarationCheckerProvider
import org.jetbrains.jet.lang.types.DynamicTypesAllowed
import org.jetbrains.jet.lang.types.DynamicTypesSettings

// NOTE: After making changes, you need to re-generate the injectors.
//       To do that, you can run main in this file.
public fun main(args: Array<String>) {
    for (generator in createInjectorGenerators()) {
        try {
            generator.generate()
        }
        catch (e: Throwable) {
            System.err.println(generator.getOutputFile())
            throw e
        }
    }
}

public fun createInjectorGenerators(): List<DependencyInjectorGenerator> =
        listOf(
                generatorForTopDownAnalyzerBasic(),
                generatorForLazyTopDownAnalyzerBasic(),
                generatorForTopDownAnalyzerForJvm(),
                generatorForJavaDescriptorResolver(),
                generatorForLazyResolveWithJava(),
                generatorForTopDownAnalyzerForJs(),
                generatorForMacro(),
                generatorForTests(),
                generatorForLazyResolve(),
                generatorForBodyResolve(),
                generatorForLazyBodyResolve(),
                generatorForReplWithJava()
        )

private fun generatorForTopDownAnalyzerBasic() =
        generator("compiler/frontend/src", "org.jetbrains.jet.di", "InjectorForTopDownAnalyzerBasic") {
            parameter<Project>()
            parameter<GlobalContext>(useAsContext = true)
            parameter<BindingTrace>()
            publicParameter<ModuleDescriptor>(useAsContext = true)

            publicField<TopDownAnalyzer>()

            field<MutablePackageFragmentProvider>()

            parameter<AdditionalCheckerProvider>()
            parameter<DynamicTypesSettings>()
        }

private fun generatorForLazyTopDownAnalyzerBasic() =
        generator("compiler/frontend/src", "org.jetbrains.jet.di", "InjectorForLazyTopDownAnalyzerBasic") {
            commonForResolveSessionBased()

            publicField<LazyTopDownAnalyzer>()

            field<AdditionalCheckerProvider.Empty>()
        }

private fun generatorForLazyBodyResolve() =
        generator("compiler/frontend/src", "org.jetbrains.jet.di", "InjectorForLazyBodyResolve") {
            parameter<Project>()
            parameter<GlobalContext>(useAsContext = true)
            parameter<KotlinCodeAnalyzer>(name = "analyzer")
            parameter<BindingTrace>()
            parameter<AdditionalCheckerProvider>()
            parameter<DynamicTypesSettings>()

            field<ModuleDescriptor>(init = GivenExpression("analyzer.getModuleDescriptor()"), useAsContext = true)

            publicField<LazyTopDownAnalyzer>()
        }

private fun generatorForTopDownAnalyzerForJs() =
        generator("js/js.frontend/src", "org.jetbrains.jet.di", "InjectorForTopDownAnalyzerForJs") {
            commonForResolveSessionBased()

            publicField<LazyTopDownAnalyzer>()

            field<MutablePackageFragmentProvider>()
            field<KotlinJsDeclarationCheckerProvider>()
            field<DynamicTypesAllowed>()
        }

private fun generatorForTopDownAnalyzerForJvm() =
        generator("compiler/frontend.java/src", "org.jetbrains.jet.di", "InjectorForTopDownAnalyzerForJvm") {
            commonForJavaTopDownAnalyzer()
        }

private fun generatorForJavaDescriptorResolver() =
        generator("compiler/frontend.java/src", "org.jetbrains.jet.di", "InjectorForJavaDescriptorResolver") {
            parameter<Project>()
            parameter<BindingTrace>()

            publicField<GlobalContextImpl>(useAsContext = true,
                        init = GivenExpression("org.jetbrains.jet.context.ContextPackage.GlobalContext()"))
            publicField<ModuleDescriptorImpl>(name = "module",
                        init = GivenExpression(javaClass<TopDownAnalyzerFacadeForJVM>().getName() + ".createJavaModule(\"<fake-jdr-module>\")"))
            publicField<JavaDescriptorResolver>()
            publicField<JavaClassFinderImpl>()

            field<GlobalSearchScope>(
                  init = GivenExpression(javaClass<GlobalSearchScope>().getName() + ".allScope(project)"))

            field<TraceBasedExternalSignatureResolver>()
            field<TraceBasedJavaResolverCache>()
            field<TraceBasedErrorReporter>()
            field<PsiBasedMethodSignatureChecker>()
            field<PsiBasedExternalAnnotationResolver>()
            field<JavaPropertyInitializerEvaluatorImpl>()
            field<SamConversionResolverImpl>()
            field<JavaSourceElementFactoryImpl>()
            field<SingleModuleClassResolver>()
            field<JavaDescriptorResolverPostConstruct>()

            field<VirtualFileFinder>(
                  init = GivenExpression(javaClass<VirtualFileFinder>().getName() + ".SERVICE.getInstance(project)"))
        }

private fun generatorForLazyResolveWithJava() =
        generator("compiler/frontend.java/src", "org.jetbrains.jet.di", "InjectorForLazyResolveWithJava") {
            commonForResolveSessionBased()

            parameter<GlobalSearchScope>(name = "moduleContentScope")

            parameter<ModuleClassResolver>()

            publicField<JavaDescriptorResolver>()

            field<VirtualFileFinder>(
                  init = GivenExpression(javaClass<VirtualFileFinderFactory>().getName()
                                         + ".SERVICE.getInstance(project).create(moduleContentScope)")
            )

            field<JavaClassFinderImpl>()
            field<TraceBasedExternalSignatureResolver>()
            field<LazyResolveBasedCache>()
            field<TraceBasedErrorReporter>()
            field<PsiBasedMethodSignatureChecker>()
            field<PsiBasedExternalAnnotationResolver>()
            field<JavaPropertyInitializerEvaluatorImpl>()
            field<SamConversionResolverImpl>()
            field<JavaSourceElementFactoryImpl>()
            field<JavaFlexibleTypeCapabilitiesProvider>()
            field<LazyResolveToken>()
            field<JavaLazyAnalyzerPostConstruct>()

            field<JavaDeclarationCheckerProvider>()
        }

private fun generatorForReplWithJava() =
        generator("compiler/frontend.java/src", "org.jetbrains.jet.di", "InjectorForReplWithJava") {
            commonForJavaTopDownAnalyzer()
            parameter<ScopeProvider.AdditionalFileScopeProvider>()
        }

private fun generatorForMacro() =
        generator("compiler/frontend/src", "org.jetbrains.jet.di", "InjectorForMacros") {
            parameter<Project>()
            parameter<ModuleDescriptor>(useAsContext = true)

            publicField<ExpressionTypingServices>()
            publicField<ExpressionTypingComponents>()
            publicField<CallResolver>()

            field<GlobalContext>(useAsContext = true,
                  init = GivenExpression("org.jetbrains.jet.context.ContextPackage.GlobalContext()"))

            field<AdditionalCheckerProvider.Empty>()
        }

private fun generatorForTests() =
        generator("compiler/tests", "org.jetbrains.jet.di", "InjectorForTests") {
            parameter<Project>()
            parameter<ModuleDescriptor>(useAsContext = true)

            publicField<DescriptorResolver>()
            publicField<ExpressionTypingServices>()
            publicField<ExpressionTypingUtils>()
            publicField<TypeResolver>()

            field<GlobalContext>(init = GivenExpression("org.jetbrains.jet.context.ContextPackage.GlobalContext()"),
                  useAsContext = true)

            field<JavaDeclarationCheckerProvider>()
        }

private fun generatorForBodyResolve() =
        generator("compiler/frontend/src", "org.jetbrains.jet.di", "InjectorForBodyResolve") {
            parameter<Project>()
            parameter<GlobalContext>(useAsContext = true)
            parameter<BindingTrace>()
            parameter<ModuleDescriptor>(useAsContext = true)
            parameter<AdditionalCheckerProvider>()
            parameter<PartialBodyResolveProvider>()

            publicField<BodyResolver>()
        }

private fun generatorForLazyResolve() =
        generator("compiler/frontend/src", "org.jetbrains.jet.di", "InjectorForLazyResolve") {
            parameter<Project>()
            parameter<GlobalContext>(useAsContext = true)
            parameter<ModuleDescriptorImpl>(useAsContext = true)
            parameter<DeclarationProviderFactory>()
            parameter<BindingTrace>()
            parameter<AdditionalCheckerProvider>()
            parameter<DynamicTypesSettings>()

            publicField<ResolveSession>()

            field<LazyResolveToken>()
        }

private fun DependencyInjectorGenerator.commonForResolveSessionBased() {
    parameter<Project>()
    parameter<GlobalContext>(useAsContext = true)
    parameter<BindingTrace>()
    parameter<ModuleDescriptorImpl>(name = "module", useAsContext = true)
    parameter<DeclarationProviderFactory>()

    publicField<ResolveSession>()
}

private fun DependencyInjectorGenerator.commonForJavaTopDownAnalyzer() {
    commonForResolveSessionBased()

    parameter<GlobalSearchScope>(name = "moduleContentScope")

    publicField<LazyTopDownAnalyzer>()
    publicField<JavaDescriptorResolver>()
    publicField<DeserializationComponentsForJava>()

    field<VirtualFileFinder>(
          init = GivenExpression(javaClass<VirtualFileFinderFactory>().getName()
                                 + ".SERVICE.getInstance(project).create(moduleContentScope)")
    )

    field<JavaClassFinderImpl>()
    field<TraceBasedExternalSignatureResolver>()
    field<LazyResolveBasedCache>()
    field<TraceBasedErrorReporter>()
    field<PsiBasedMethodSignatureChecker>()
    field<PsiBasedExternalAnnotationResolver>()
    field<JavaPropertyInitializerEvaluatorImpl>()
    field<SamConversionResolverImpl>()
    field<JavaSourceElementFactoryImpl>()
    field<MutablePackageFragmentProvider>()
    field<SingleModuleClassResolver>()
    field<JavaLazyAnalyzerPostConstruct>()
    field<JavaFlexibleTypeCapabilitiesProvider>()

    field<JavaDeclarationCheckerProvider>()

    field<VirtualFileFinder>(init = GivenExpression(javaClass<VirtualFileFinder>().getName() + ".SERVICE.getInstance(project)"))
}


private fun generator(
        targetSourceRoot: String,
        injectorPackageName: String,
        injectorClassName: String,
        body: DependencyInjectorGenerator.() -> Unit
) = generator(targetSourceRoot, injectorPackageName, injectorClassName, "org.jetbrains.jet.generators.injectors.InjectorsPackage", body)
