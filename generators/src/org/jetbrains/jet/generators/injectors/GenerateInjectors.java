/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.generators.injectors;

import com.intellij.openapi.project.Project;
import org.jetbrains.jet.codegen.*;
import org.jetbrains.jet.codegen.intrinsics.IntrinsicMethods;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.di.DependencyInjectorGenerator;
import org.jetbrains.jet.di.GivenExpression;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzer;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.psi.JetImportsFactory;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.CallResolver;
import org.jetbrains.jet.lang.resolve.calls.CallResolverExtensionProvider;
import org.jetbrains.jet.lang.resolve.java.JavaClassFinderImpl;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.mapping.JavaToKotlinClassMap;
import org.jetbrains.jet.lang.resolve.java.resolver.*;
import org.jetbrains.jet.lang.resolve.kotlin.VirtualFileFinder;
import org.jetbrains.jet.lang.resolve.kotlin.VirtualFileKotlinClassFinder;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.lazy.ScopeProvider;
import org.jetbrains.jet.lang.types.DependencyClassByQualifiedNameResolverDummyImpl;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

// NOTE: After making changes, you need to re-generate the injectors.
//       To do that, you can run either this class
public class GenerateInjectors {

    private GenerateInjectors() {
    }

    public static void main(String[] args) throws Throwable {
        for (DependencyInjectorGenerator generator : createGenerators()) {
            try {
                generator.generate();
            }
            catch (Throwable e) {
                System.err.println(generator.getOutputFile());
                throw e;
            }
        }
    }

    public static List<DependencyInjectorGenerator> createGenerators() throws IOException {
        return Arrays.asList(
                generateInjectorForTopDownAnalyzerBasic(),
                generateInjectorForTopDownAnalyzerForJvm(),
                generateInjectorForJavaDescriptorResolver(),
                generateInjectorForTopDownAnalyzerForJs(),
                generateMacroInjector(),
                generateTestInjector(),
                generateInjectorForJvmCodegen(),
                generateInjectorForLazyResolve(),
                generateInjectorForBodyResolve()
        );
    }

    private static DependencyInjectorGenerator generateInjectorForLazyResolve() throws IOException {
        DependencyInjectorGenerator generator = new DependencyInjectorGenerator();
        generator.addParameter(Project.class);
        generator.addParameter(ResolveSession.class);
        generator.addParameter(ModuleDescriptor.class);
        generator.addPublicField(DescriptorResolver.class);
        generator.addPublicField(ExpressionTypingServices.class);
        generator.addPublicField(TypeResolver.class);
        generator.addPublicField(ScopeProvider.class);
        generator.addPublicField(AnnotationResolver.class);
        generator.addPublicField(QualifiedExpressionResolver.class);
        generator.addPublicField(JetImportsFactory.class);
        generator.addField(CallResolverExtensionProvider.class);
        generator.addField(false, PlatformToKotlinClassMap.class, null, new GivenExpression("moduleDescriptor.getPlatformToKotlinClassMap()"));
        generator.configure("compiler/frontend/src", "org.jetbrains.jet.di", "InjectorForLazyResolve", GenerateInjectors.class);
        return generator;
    }

    private static DependencyInjectorGenerator generateInjectorForTopDownAnalyzerBasic() throws IOException {
        DependencyInjectorGenerator generator = new DependencyInjectorGenerator();
        generateInjectorForTopDownAnalyzerCommon(generator);
        generator.addField(DependencyClassByQualifiedNameResolverDummyImpl.class);
        generator.addField(MutablePackageFragmentProvider.class);
        generator.addField(NamespaceFactoryImpl.class);
        generator.addParameter(PlatformToKotlinClassMap.class);
        generator.configure("compiler/frontend/src", "org.jetbrains.jet.di", "InjectorForTopDownAnalyzerBasic", GenerateInjectors.class);
        return generator;
    }

    private static DependencyInjectorGenerator generateInjectorForTopDownAnalyzerForJs() throws IOException {
        DependencyInjectorGenerator generator = new DependencyInjectorGenerator();
        generateInjectorForTopDownAnalyzerCommon(generator);
        generator.addField(DependencyClassByQualifiedNameResolverDummyImpl.class);
        generator.addField(MutablePackageFragmentProvider.class);
        generator.addField(NamespaceFactoryImpl.class);
        generator.addField(false, PlatformToKotlinClassMap.class, null, new GivenExpression("org.jetbrains.jet.lang.PlatformToKotlinClassMap.EMPTY"));
        generator.configure("js/js.translator/src", "org.jetbrains.jet.di", "InjectorForTopDownAnalyzerForJs", GenerateInjectors.class);
        return generator;
    }

    private static DependencyInjectorGenerator generateInjectorForTopDownAnalyzerForJvm() throws IOException {
        DependencyInjectorGenerator generator = new DependencyInjectorGenerator();
        generator.implementInterface(InjectorForTopDownAnalyzer.class);
        generateInjectorForTopDownAnalyzerCommon(generator);
        generator.addField(JavaDescriptorResolver.class);
        generator.addField(false, JavaToKotlinClassMap.class, null, new GivenExpression("org.jetbrains.jet.lang.resolve.java.mapping.JavaToKotlinClassMap.getInstance()"));
        generator.addField(JavaClassFinderImpl.class);
        generator.addField(TraceBasedExternalSignatureResolver.class);
        generator.addField(TraceBasedJavaResolverCache.class);
        generator.addField(TraceBasedErrorReporter.class);
        generator.addField(PsiBasedMethodSignatureChecker.class);
        generator.addField(PsiBasedExternalAnnotationResolver.class);
        generator.addField(VirtualFileKotlinClassFinder.class);
        generator.addField(MutablePackageFragmentProvider.class);
        generator.addField(NamespaceFactoryImpl.class);
        generator.addPublicField(JavaPackageFragmentProvider.class);
        generator.addField(false, VirtualFileFinder.class, "virtualFileFinder",
                           new GivenExpression(
                                   "com.intellij.openapi.components.ServiceManager.getService(project, VirtualFileFinder.class)"));
        generator.configure("compiler/frontend.java/src", "org.jetbrains.jet.di", "InjectorForTopDownAnalyzerForJvm",
                           GenerateInjectors.class);
        return generator;
    }

    private static DependencyInjectorGenerator generateInjectorForJavaDescriptorResolver() throws IOException {
        DependencyInjectorGenerator generator = new DependencyInjectorGenerator();

        // Parameters
        generator.addParameter(Project.class);
        generator.addParameter(BindingTrace.class);

        // Fields
        generator.addPublicField(JavaClassFinderImpl.class);
        generator.addField(TraceBasedExternalSignatureResolver.class);
        generator.addField(TraceBasedJavaResolverCache.class);
        generator.addField(TraceBasedErrorReporter.class);
        generator.addField(PsiBasedMethodSignatureChecker.class);
        generator.addField(PsiBasedExternalAnnotationResolver.class);
        generator.addPublicField(JavaDescriptorResolver.class);
        generator.addField(VirtualFileKotlinClassFinder.class);
        generator.addField(false, VirtualFileFinder.class, "virtualFileFinder",
                           new GivenExpression("com.intellij.openapi.components.ServiceManager.getService(project, VirtualFileFinder.class)"));
        generator.addField(true, ModuleDescriptorImpl.class, "module",
                           new GivenExpression("org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM.createJavaModule(\"<fake-jdr-module>\")"));

        generator.configure("compiler/frontend.java/src", "org.jetbrains.jet.di", "InjectorForJavaDescriptorResolver",
                            GenerateInjectors.class);
        return generator;
    }

    private static DependencyInjectorGenerator generateInjectorForTopDownAnalyzerCommon(DependencyInjectorGenerator generator) {
        // Fields
        generator.addPublicField(TopDownAnalyzer.class);
        generator.addPublicField(TopDownAnalysisContext.class);
        generator.addPublicField(BodyResolver.class);
        generator.addPublicField(ControlFlowAnalyzer.class);
        generator.addPublicField(DeclarationsChecker.class);
        generator.addPublicField(DescriptorResolver.class);
        generator.addField(CallResolverExtensionProvider.class);

        // Parameters
        generator.addPublicParameter(Project.class);
        generator.addPublicParameter(TopDownAnalysisParameters.class);
        generator.addPublicParameter(BindingTrace.class);
        generator.addPublicParameter(ModuleDescriptorImpl.class);
        return generator;
    }

    private static DependencyInjectorGenerator generateMacroInjector() throws IOException {
        DependencyInjectorGenerator generator = new DependencyInjectorGenerator();

        // Fields
        generator.addPublicField(ExpressionTypingServices.class);
        generator.addField(CallResolverExtensionProvider.class);
        generator.addField(false, PlatformToKotlinClassMap.class, null, new GivenExpression("moduleDescriptor.getPlatformToKotlinClassMap()"));

        // Parameters
        generator.addPublicParameter(Project.class);
        generator.addParameter(ModuleDescriptor.class);

        generator.configure("compiler/frontend/src", "org.jetbrains.jet.di", "InjectorForMacros", GenerateInjectors.class);
        return generator;
    }

    private static DependencyInjectorGenerator generateTestInjector() throws IOException {
        DependencyInjectorGenerator generator = new DependencyInjectorGenerator();

        // Fields
        generator.addPublicField(DescriptorResolver.class);
        generator.addPublicField(ExpressionTypingServices.class);
        generator.addPublicField(TypeResolver.class);
        generator.addPublicField(CallResolver.class);
        generator.addField(CallResolverExtensionProvider.class);
        generator.addField(true, KotlinBuiltIns.class, null, new GivenExpression("KotlinBuiltIns.getInstance()"));
        generator.addField(false, PlatformToKotlinClassMap.class, null, new GivenExpression("moduleDescriptor.getPlatformToKotlinClassMap()"));

        // Parameters
        generator.addPublicParameter(Project.class);
        generator.addParameter(ModuleDescriptor.class);

        generator.configure("compiler/tests", "org.jetbrains.jet.di", "InjectorForTests", GenerateInjectors.class);
        return generator;
    }

    private static DependencyInjectorGenerator generateInjectorForJvmCodegen() throws IOException {
        DependencyInjectorGenerator generator = new DependencyInjectorGenerator();

        // Parameters
        generator.addPublicParameter(JetTypeMapper.class);
        generator.addPublicParameter(GenerationState.class);
        generator.addParameter(ClassBuilderFactory.class);
        generator.addPublicParameter(Project.class);

        // Fields
        generator.addField(false, BindingTrace.class, "bindingTrace",
                           new GivenExpression("jetTypeMapper.getBindingTrace()"));
        generator.addField(false, BindingContext.class, "bindingContext",
                           new GivenExpression("bindingTrace.getBindingContext()"));
        generator.addField(false, ClassBuilderMode.class, "classBuilderMode",
                           new GivenExpression("classBuilderFactory.getClassBuilderMode()"));
        generator.addField(true, IntrinsicMethods.class, "intrinsics", null);
        generator.addPublicField(ClassFileFactory.class);

        generator.configure("compiler/backend/src", "org.jetbrains.jet.di", "InjectorForJvmCodegen", GenerateInjectors.class);
        return generator;
    }

    private static DependencyInjectorGenerator generateInjectorForBodyResolve() throws IOException {
        DependencyInjectorGenerator generator = new DependencyInjectorGenerator();
        // Fields
        generator.addPublicField(BodyResolver.class);
        generator.addField(CallResolverExtensionProvider.class);
        generator.addField(false, PlatformToKotlinClassMap.class, null, new GivenExpression("moduleDescriptor.getPlatformToKotlinClassMap()"));
        generator.addField(FunctionAnalyzerExtension.class);

        // Parameters
        generator.addPublicParameter(Project.class);
        generator.addPublicParameter(TopDownAnalysisParameters.class);
        generator.addPublicParameter(BindingTrace.class);
        generator.addPublicParameter(BodiesResolveContext.class);
        generator.addParameter(ModuleDescriptor.class);
        generator.configure("compiler/frontend/src", "org.jetbrains.jet.di", "InjectorForBodyResolve", GenerateInjectors.class);
        return generator;
    }
}
