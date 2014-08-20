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
import org.jetbrains.jet.lang.resolve.kotlin.VirtualFileFinder
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession
import org.jetbrains.jet.lang.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices
import org.jetbrains.jet.di.*
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingComponents
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils
import org.jetbrains.jet.lang.resolve.calls.CallResolver
import org.jetbrains.jet.lang.resolve.java.structure.impl.JavaPropertyInitializerEvaluatorImpl
import org.jetbrains.jet.lang.resolve.kotlin.DeserializationGlobalContextForJava

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
                generatorForTopDownAnalyzerForJvm(),
                generatorForJavaDescriptorResolver(),
                generatorForLazyResolveWithJava(),
                generatorForTopDownAnalyzerForJs(),
                generatorForMacro(),
                generatorForTests(),
                generatorForLazyResolve(),
                generatorForBodyResolve()
        )

private fun DependencyInjectorGenerator.commonForTopDownAnalyzer() {
    parameter(javaClass<Project>())
    parameter(javaClass<GlobalContext>(), useAsContext = true)
    parameter(javaClass<BindingTrace>())
    publicParameter(javaClass<ModuleDescriptor>(), useAsContext = true)

    publicFields(
            javaClass<TopDownAnalyzer>(),
            javaClass<LazyTopDownAnalyzer>()
    )

    field(javaClass<MutablePackageFragmentProvider>())
}

private fun generatorForTopDownAnalyzerBasic() =
        generator("compiler/frontend/src", "org.jetbrains.jet.di", "InjectorForTopDownAnalyzerBasic") {
            commonForTopDownAnalyzer()
        }

private fun generatorForTopDownAnalyzerForJs() =
        generator("js/js.frontend/src", "org.jetbrains.jet.di", "InjectorForTopDownAnalyzerForJs") {
            commonForTopDownAnalyzer()
        }

private fun generatorForTopDownAnalyzerForJvm() =
        generator("compiler/frontend.java/src", "org.jetbrains.jet.di", "InjectorForTopDownAnalyzerForJvm") {
            implementInterface(javaClass<InjectorForTopDownAnalyzer>())
            commonForTopDownAnalyzer()

            publicField(javaClass<JavaDescriptorResolver>())
            publicField(javaClass<DeserializationGlobalContextForJava>())

            fields(
                    javaClass<JavaClassFinderImpl>(),
                    javaClass<TraceBasedExternalSignatureResolver>(),
                    javaClass<TraceBasedJavaResolverCache>(),
                    javaClass<TraceBasedErrorReporter>(),
                    javaClass<PsiBasedMethodSignatureChecker>(),
                    javaClass<PsiBasedExternalAnnotationResolver>(),
                    javaClass<MutablePackageFragmentProvider>(),
                    javaClass<JavaPropertyInitializerEvaluatorImpl>(),
                    javaClass<JavaSourceElementFactoryImpl>()
            )
            field(javaClass<VirtualFileFinder>(), init = GivenExpression(javaClass<VirtualFileFinder>().getName() + ".SERVICE.getInstance(project)"))
        }

private fun generatorForJavaDescriptorResolver() =
        generator("compiler/frontend.java/src", "org.jetbrains.jet.di", "InjectorForJavaDescriptorResolver") {
            parameters(
                    javaClass<Project>(),
                    javaClass<BindingTrace>()
            )

            publicField(javaClass<GlobalContextImpl>(), useAsContext = true,
                        init = GivenExpression("org.jetbrains.jet.context.ContextPackage.GlobalContext()"))
            publicField(javaClass<ModuleDescriptorImpl>(), name = "module",
                        init = GivenExpression("org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM.createJavaModule(\"<fake-jdr-module>\")"))
            publicField(javaClass<JavaDescriptorResolver>())
            publicField(javaClass<JavaClassFinderImpl>())

            fields(
                    javaClass<TraceBasedExternalSignatureResolver>(),
                    javaClass<TraceBasedJavaResolverCache>(),
                    javaClass<TraceBasedErrorReporter>(),
                    javaClass<PsiBasedMethodSignatureChecker>(),
                    javaClass<PsiBasedExternalAnnotationResolver>(),
                    javaClass<JavaPropertyInitializerEvaluatorImpl>(),
                    javaClass<JavaSourceElementFactoryImpl>()
            )
            field(javaClass<VirtualFileFinder>(),
                  init = GivenExpression(javaClass<VirtualFileFinder>().getName() + ".SERVICE.getInstance(project)"))
        }

private fun generatorForLazyResolveWithJava() =
        generator("compiler/frontend.java/src", "org.jetbrains.jet.di", "InjectorForLazyResolveWithJava") {
            parameter(javaClass<Project>())
            parameter(javaClass<GlobalContextImpl>(), useAsContext = true)
            parameters(
                    javaClass<DeclarationProviderFactory>(),
                    javaClass<BindingTrace>()
            )

            publicField(javaClass<ModuleDescriptorImpl>(), name = "module", useAsContext = true,
                        init = GivenExpression("org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM.createJavaModule(\"<fake-jdr-module>\")"))
            publicFields(
                    javaClass<ResolveSession>(),
                    javaClass<JavaDescriptorResolver>()
            )

            field(javaClass<VirtualFileFinder>(),
                  init = GivenExpression(javaClass<VirtualFileFinder>().getName() + ".SERVICE.getInstance(project)"))
            fields(
                    javaClass<JavaClassFinderImpl>(),
                    javaClass<TraceBasedExternalSignatureResolver>(),
                    javaClass<LazyResolveBasedCache>(),
                    javaClass<TraceBasedErrorReporter>(),
                    javaClass<PsiBasedMethodSignatureChecker>(),
                    javaClass<PsiBasedExternalAnnotationResolver>(),
                    javaClass<JavaPropertyInitializerEvaluatorImpl>(),
                    javaClass<JavaSourceElementFactoryImpl>()
            )
        }

private fun generatorForMacro() =
        generator("compiler/frontend/src", "org.jetbrains.jet.di", "InjectorForMacros") {
            parameter(javaClass<Project>())
            parameter(javaClass<ModuleDescriptor>(), useAsContext = true)

            publicField(javaClass<ExpressionTypingServices>())
            publicField(javaClass<ExpressionTypingComponents>())
            publicField(javaClass<CallResolver>())

            field(javaClass<GlobalContext>(), useAsContext = true,
                  init = GivenExpression("org.jetbrains.jet.context.ContextPackage.GlobalContext()"))
        }

private fun generatorForTests() =
        generator("compiler/tests", "org.jetbrains.jet.di", "InjectorForTests") {
            parameter(javaClass<Project>())
            parameter(javaClass<ModuleDescriptor>(), useAsContext = true)

            publicFields(
                    javaClass<DescriptorResolver>(),
                    javaClass<ExpressionTypingServices>(),
                    javaClass<ExpressionTypingUtils>(),
                    javaClass<TypeResolver>()
            )

            field(javaClass<GlobalContext>(), init = GivenExpression("org.jetbrains.jet.context.ContextPackage.GlobalContext()"),
                  useAsContext = true)
        }

private fun generatorForBodyResolve() =
        generator("compiler/frontend/src", "org.jetbrains.jet.di", "InjectorForBodyResolve") {
            parameter(javaClass<Project>())
            parameter(javaClass<GlobalContext>(), useAsContext = true)
            parameter(javaClass<BindingTrace>())
            parameter(javaClass<ModuleDescriptor>(), useAsContext = true)

            publicField(javaClass<BodyResolver>())
        }

private fun generatorForLazyResolve() =
        generator("compiler/frontend/src", "org.jetbrains.jet.di", "InjectorForLazyResolve") {
            parameter(javaClass<Project>())
            parameter(javaClass<GlobalContextImpl>(), useAsContext = true)
            parameter(javaClass<ModuleDescriptorImpl>(), useAsContext = true)
            parameter(javaClass<DeclarationProviderFactory>())
            parameter(javaClass<BindingTrace>())

            publicField(javaClass<ResolveSession>())
        }

private fun generator(
        targetSourceRoot: String,
        injectorPackageName: String,
        injectorClassName: String,
        body: DependencyInjectorGenerator.() -> Unit
) = generator(targetSourceRoot, injectorPackageName, injectorClassName, "org.jetbrains.jet.generators.injectors.InjectorsPackage", body)
