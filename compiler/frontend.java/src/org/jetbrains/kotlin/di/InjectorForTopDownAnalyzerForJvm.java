/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.di;

import com.intellij.openapi.project.Project;
import org.jetbrains.kotlin.context.GlobalContext;
import org.jetbrains.kotlin.storage.StorageManager;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.platform.PlatformToKotlinClassMap;
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory;
import org.jetbrains.kotlin.resolve.lazy.ResolveSession;
import org.jetbrains.kotlin.resolve.lazy.ScopeProvider;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer;
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzerForTopLevel;
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver;
import org.jetbrains.kotlin.load.kotlin.DeserializationComponentsForJava;
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder;
import org.jetbrains.kotlin.load.java.JavaClassFinderImpl;
import org.jetbrains.kotlin.load.java.components.TraceBasedExternalSignatureResolver;
import org.jetbrains.kotlin.load.java.components.LazyResolveBasedCache;
import org.jetbrains.kotlin.load.java.components.TraceBasedErrorReporter;
import org.jetbrains.kotlin.load.java.components.PsiBasedMethodSignatureChecker;
import org.jetbrains.kotlin.load.java.components.PsiBasedExternalAnnotationResolver;
import org.jetbrains.kotlin.load.java.structure.impl.JavaPropertyInitializerEvaluatorImpl;
import org.jetbrains.kotlin.load.java.sam.SamConversionResolverImpl;
import org.jetbrains.kotlin.load.java.components.JavaSourceElementFactoryImpl;
import org.jetbrains.kotlin.load.java.lazy.SingleModuleClassResolver;
import org.jetbrains.kotlin.resolve.jvm.JavaLazyAnalyzerPostConstruct;
import org.jetbrains.kotlin.load.java.JavaFlexibleTypeCapabilitiesProvider;
import org.jetbrains.kotlin.load.kotlin.KotlinJvmCheckerProvider;
import org.jetbrains.kotlin.resolve.AnnotationResolver;
import org.jetbrains.kotlin.resolve.calls.CallResolver;
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingComponents;
import org.jetbrains.kotlin.types.expressions.ControlStructureTypingUtils;
import org.jetbrains.kotlin.types.DynamicTypesSettings;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils;
import org.jetbrains.kotlin.types.expressions.ForLoopConventionsChecker;
import org.jetbrains.kotlin.types.expressions.LocalClassifierAnalyzer;
import org.jetbrains.kotlin.resolve.DescriptorResolver;
import org.jetbrains.kotlin.resolve.DelegatedPropertyResolver;
import org.jetbrains.kotlin.resolve.TypeResolver;
import org.jetbrains.kotlin.resolve.QualifiedExpressionResolver;
import org.jetbrains.kotlin.context.TypeLazinessToken;
import org.jetbrains.kotlin.resolve.FunctionDescriptorResolver;
import org.jetbrains.kotlin.types.reflect.ReflectionTypes;
import org.jetbrains.kotlin.resolve.calls.CallExpressionResolver;
import org.jetbrains.kotlin.resolve.StatementFilter;
import org.jetbrains.kotlin.resolve.calls.CallCompleter;
import org.jetbrains.kotlin.resolve.calls.CandidateResolver;
import org.jetbrains.kotlin.resolve.calls.tasks.TaskPrioritizer;
import org.jetbrains.kotlin.psi.JetImportsFactory;
import org.jetbrains.kotlin.resolve.lazy.LazyDeclarationResolver;
import org.jetbrains.kotlin.resolve.lazy.DeclarationScopeProviderImpl;
import org.jetbrains.kotlin.resolve.ScriptBodyResolver;
import org.jetbrains.kotlin.resolve.lazy.ScopeProvider.AdditionalFileScopeProvider;
import org.jetbrains.kotlin.resolve.BodyResolver;
import org.jetbrains.kotlin.resolve.ControlFlowAnalyzer;
import org.jetbrains.kotlin.resolve.DeclarationsChecker;
import org.jetbrains.kotlin.resolve.ModifiersChecker;
import org.jetbrains.kotlin.resolve.FunctionAnalyzerExtension;
import org.jetbrains.kotlin.resolve.DeclarationResolver;
import org.jetbrains.kotlin.resolve.OverloadResolver;
import org.jetbrains.kotlin.resolve.OverrideResolver;
import org.jetbrains.kotlin.resolve.varianceChecker.VarianceChecker;
import org.jetbrains.kotlin.load.java.lazy.LazyJavaPackageFragmentProvider;
import org.jetbrains.kotlin.load.java.lazy.GlobalJavaResolverContext;
import org.jetbrains.kotlin.load.kotlin.DeserializedDescriptorResolver;
import org.jetbrains.kotlin.load.kotlin.JavaClassDataFinder;
import org.jetbrains.kotlin.load.kotlin.BinaryClassAnnotationAndConstantLoaderImpl;
import org.jetbrains.annotations.NotNull;
import javax.annotation.PreDestroy;

/* This file is generated by org.jetbrains.kotlin.generators.injectors.InjectorsPackage. DO NOT EDIT! */
@SuppressWarnings("all")
public class InjectorForTopDownAnalyzerForJvm {

    private final Project project;
    private final GlobalContext globalContext;
    private final StorageManager storageManager;
    private final BindingTrace bindingTrace;
    private final ModuleDescriptorImpl module;
    private final KotlinBuiltIns kotlinBuiltIns;
    private final PlatformToKotlinClassMap platformToKotlinClassMap;
    private final DeclarationProviderFactory declarationProviderFactory;
    private final ResolveSession resolveSession;
    private final ScopeProvider scopeProvider;
    private final GlobalSearchScope moduleContentScope;
    private final LazyTopDownAnalyzer lazyTopDownAnalyzer;
    private final LazyTopDownAnalyzerForTopLevel lazyTopDownAnalyzerForTopLevel;
    private final JavaDescriptorResolver javaDescriptorResolver;
    private final DeserializationComponentsForJava deserializationComponentsForJava;
    private final VirtualFileFinder virtualFileFinder;
    private final JavaClassFinderImpl javaClassFinder;
    private final TraceBasedExternalSignatureResolver traceBasedExternalSignatureResolver;
    private final LazyResolveBasedCache lazyResolveBasedCache;
    private final TraceBasedErrorReporter traceBasedErrorReporter;
    private final PsiBasedMethodSignatureChecker psiBasedMethodSignatureChecker;
    private final PsiBasedExternalAnnotationResolver psiBasedExternalAnnotationResolver;
    private final JavaPropertyInitializerEvaluatorImpl javaPropertyInitializerEvaluator;
    private final SamConversionResolverImpl samConversionResolver;
    private final JavaSourceElementFactoryImpl javaSourceElementFactory;
    private final SingleModuleClassResolver singleModuleClassResolver;
    private final JavaLazyAnalyzerPostConstruct javaLazyAnalyzerPostConstruct;
    private final JavaFlexibleTypeCapabilitiesProvider javaFlexibleTypeCapabilitiesProvider;
    private final KotlinJvmCheckerProvider kotlinJvmCheckerProvider;
    private final AnnotationResolver annotationResolver;
    private final CallResolver callResolver;
    private final ArgumentTypeResolver argumentTypeResolver;
    private final ExpressionTypingServices expressionTypingServices;
    private final ExpressionTypingComponents expressionTypingComponents;
    private final ControlStructureTypingUtils controlStructureTypingUtils;
    private final DynamicTypesSettings dynamicTypesSettings;
    private final ExpressionTypingUtils expressionTypingUtils;
    private final ForLoopConventionsChecker forLoopConventionsChecker;
    private final LocalClassifierAnalyzer localClassifierAnalyzer;
    private final DescriptorResolver descriptorResolver;
    private final DelegatedPropertyResolver delegatedPropertyResolver;
    private final TypeResolver typeResolver;
    private final QualifiedExpressionResolver qualifiedExpressionResolver;
    private final TypeLazinessToken typeLazinessToken;
    private final FunctionDescriptorResolver functionDescriptorResolver;
    private final ReflectionTypes reflectionTypes;
    private final CallExpressionResolver callExpressionResolver;
    private final StatementFilter statementFilter;
    private final CallCompleter callCompleter;
    private final CandidateResolver candidateResolver;
    private final TaskPrioritizer taskPrioritizer;
    private final JetImportsFactory jetImportsFactory;
    private final LazyDeclarationResolver lazyDeclarationResolver;
    private final DeclarationScopeProviderImpl declarationScopeProvider;
    private final ScriptBodyResolver scriptBodyResolver;
    private final AdditionalFileScopeProvider additionalFileScopeProvider;
    private final BodyResolver bodyResolver;
    private final ControlFlowAnalyzer controlFlowAnalyzer;
    private final DeclarationsChecker declarationsChecker;
    private final ModifiersChecker modifiersChecker;
    private final FunctionAnalyzerExtension functionAnalyzerExtension;
    private final DeclarationResolver declarationResolver;
    private final OverloadResolver overloadResolver;
    private final OverrideResolver overrideResolver;
    private final VarianceChecker varianceChecker;
    private final LazyJavaPackageFragmentProvider lazyJavaPackageFragmentProvider;
    private final GlobalJavaResolverContext globalJavaResolverContext;
    private final DeserializedDescriptorResolver deserializedDescriptorResolver;
    private final JavaClassDataFinder javaClassDataFinder;
    private final BinaryClassAnnotationAndConstantLoaderImpl binaryClassAnnotationAndConstantLoader;

    public InjectorForTopDownAnalyzerForJvm(
        @NotNull Project project,
        @NotNull GlobalContext globalContext,
        @NotNull BindingTrace bindingTrace,
        @NotNull ModuleDescriptorImpl module,
        @NotNull DeclarationProviderFactory declarationProviderFactory,
        @NotNull GlobalSearchScope moduleContentScope
    ) {
        this.project = project;
        this.globalContext = globalContext;
        this.storageManager = globalContext.getStorageManager();
        this.bindingTrace = bindingTrace;
        this.module = module;
        this.kotlinBuiltIns = module.getBuiltIns();
        this.platformToKotlinClassMap = module.getPlatformToKotlinClassMap();
        this.declarationProviderFactory = declarationProviderFactory;
        this.resolveSession = new ResolveSession(project, globalContext, getModule(), declarationProviderFactory, bindingTrace);
        this.scopeProvider = new ScopeProvider(getResolveSession());
        this.moduleContentScope = moduleContentScope;
        this.lazyTopDownAnalyzer = new LazyTopDownAnalyzer();
        this.lazyTopDownAnalyzerForTopLevel = new LazyTopDownAnalyzerForTopLevel();
        this.javaClassFinder = new JavaClassFinderImpl();
        this.virtualFileFinder = org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory.SERVICE.getInstance(project).create(moduleContentScope);
        this.traceBasedErrorReporter = new TraceBasedErrorReporter();
        this.deserializedDescriptorResolver = new DeserializedDescriptorResolver(traceBasedErrorReporter);
        this.psiBasedExternalAnnotationResolver = new PsiBasedExternalAnnotationResolver();
        this.traceBasedExternalSignatureResolver = new TraceBasedExternalSignatureResolver();
        this.psiBasedMethodSignatureChecker = new PsiBasedMethodSignatureChecker();
        this.lazyResolveBasedCache = new LazyResolveBasedCache();
        this.javaPropertyInitializerEvaluator = new JavaPropertyInitializerEvaluatorImpl();
        this.samConversionResolver = SamConversionResolverImpl.INSTANCE$;
        this.javaSourceElementFactory = new JavaSourceElementFactoryImpl();
        this.singleModuleClassResolver = new SingleModuleClassResolver();
        this.globalJavaResolverContext = new GlobalJavaResolverContext(storageManager, javaClassFinder, virtualFileFinder, deserializedDescriptorResolver, psiBasedExternalAnnotationResolver, traceBasedExternalSignatureResolver, traceBasedErrorReporter, psiBasedMethodSignatureChecker, lazyResolveBasedCache, javaPropertyInitializerEvaluator, samConversionResolver, javaSourceElementFactory, singleModuleClassResolver);
        this.lazyJavaPackageFragmentProvider = new LazyJavaPackageFragmentProvider(globalJavaResolverContext, getModule());
        this.javaDescriptorResolver = new JavaDescriptorResolver(lazyJavaPackageFragmentProvider, getModule());
        this.javaClassDataFinder = new JavaClassDataFinder(virtualFileFinder, deserializedDescriptorResolver);
        this.binaryClassAnnotationAndConstantLoader = new BinaryClassAnnotationAndConstantLoaderImpl(getModule(), storageManager, virtualFileFinder, traceBasedErrorReporter);
        this.deserializationComponentsForJava = new DeserializationComponentsForJava(storageManager, getModule(), javaClassDataFinder, binaryClassAnnotationAndConstantLoader, lazyJavaPackageFragmentProvider);
        this.javaLazyAnalyzerPostConstruct = new JavaLazyAnalyzerPostConstruct();
        this.javaFlexibleTypeCapabilitiesProvider = new JavaFlexibleTypeCapabilitiesProvider();
        this.kotlinJvmCheckerProvider = KotlinJvmCheckerProvider.INSTANCE$;
        this.annotationResolver = new AnnotationResolver();
        this.callResolver = new CallResolver();
        this.argumentTypeResolver = new ArgumentTypeResolver();
        this.expressionTypingComponents = new ExpressionTypingComponents();
        this.expressionTypingServices = new ExpressionTypingServices(expressionTypingComponents);
        this.controlStructureTypingUtils = new ControlStructureTypingUtils(expressionTypingServices);
        this.dynamicTypesSettings = new DynamicTypesSettings();
        this.expressionTypingUtils = new ExpressionTypingUtils(expressionTypingServices, callResolver, kotlinBuiltIns);
        this.forLoopConventionsChecker = new ForLoopConventionsChecker();
        this.descriptorResolver = new DescriptorResolver();
        this.qualifiedExpressionResolver = new QualifiedExpressionResolver();
        this.typeLazinessToken = new TypeLazinessToken();
        this.typeResolver = new TypeResolver(annotationResolver, qualifiedExpressionResolver, getModule(), javaFlexibleTypeCapabilitiesProvider, storageManager, typeLazinessToken, dynamicTypesSettings);
        this.functionDescriptorResolver = new FunctionDescriptorResolver(typeResolver, descriptorResolver, annotationResolver, storageManager, expressionTypingServices, kotlinBuiltIns);
        this.localClassifierAnalyzer = new LocalClassifierAnalyzer(descriptorResolver, functionDescriptorResolver, typeResolver, annotationResolver);
        this.delegatedPropertyResolver = new DelegatedPropertyResolver();
        this.reflectionTypes = new ReflectionTypes(getModule());
        this.callExpressionResolver = new CallExpressionResolver();
        this.statementFilter = new StatementFilter();
        this.candidateResolver = new CandidateResolver();
        this.callCompleter = new CallCompleter(argumentTypeResolver, candidateResolver);
        this.taskPrioritizer = new TaskPrioritizer(storageManager);
        this.jetImportsFactory = new JetImportsFactory();
        this.lazyDeclarationResolver = new LazyDeclarationResolver(globalContext, bindingTrace);
        this.declarationScopeProvider = new DeclarationScopeProviderImpl(lazyDeclarationResolver);
        this.scriptBodyResolver = new ScriptBodyResolver();
        this.additionalFileScopeProvider = new AdditionalFileScopeProvider();
        this.bodyResolver = new BodyResolver();
        this.controlFlowAnalyzer = new ControlFlowAnalyzer();
        this.declarationsChecker = new DeclarationsChecker();
        this.modifiersChecker = new ModifiersChecker(bindingTrace, kotlinJvmCheckerProvider);
        this.functionAnalyzerExtension = new FunctionAnalyzerExtension();
        this.declarationResolver = new DeclarationResolver();
        this.overloadResolver = new OverloadResolver();
        this.overrideResolver = new OverrideResolver();
        this.varianceChecker = new VarianceChecker(bindingTrace);

        this.resolveSession.setAnnotationResolve(annotationResolver);
        this.resolveSession.setDescriptorResolver(descriptorResolver);
        this.resolveSession.setFunctionDescriptorResolver(functionDescriptorResolver);
        this.resolveSession.setJetImportFactory(jetImportsFactory);
        this.resolveSession.setLazyDeclarationResolver(lazyDeclarationResolver);
        this.resolveSession.setQualifiedExpressionResolver(qualifiedExpressionResolver);
        this.resolveSession.setScopeProvider(scopeProvider);
        this.resolveSession.setScriptBodyResolver(scriptBodyResolver);
        this.resolveSession.setTypeResolver(typeResolver);

        scopeProvider.setAdditionalFileScopesProvider(additionalFileScopeProvider);
        scopeProvider.setDeclarationScopeProvider(declarationScopeProvider);

        this.lazyTopDownAnalyzer.setBodyResolver(bodyResolver);
        this.lazyTopDownAnalyzer.setDeclarationResolver(declarationResolver);
        this.lazyTopDownAnalyzer.setDeclarationScopeProvider(declarationScopeProvider);
        this.lazyTopDownAnalyzer.setFileScopeProvider(scopeProvider);
        this.lazyTopDownAnalyzer.setLazyDeclarationResolver(lazyDeclarationResolver);
        this.lazyTopDownAnalyzer.setModuleDescriptor(module);
        this.lazyTopDownAnalyzer.setOverloadResolver(overloadResolver);
        this.lazyTopDownAnalyzer.setOverrideResolver(overrideResolver);
        this.lazyTopDownAnalyzer.setTopLevelDescriptorProvider(resolveSession);
        this.lazyTopDownAnalyzer.setTrace(bindingTrace);
        this.lazyTopDownAnalyzer.setVarianceChecker(varianceChecker);

        this.lazyTopDownAnalyzerForTopLevel.setKotlinCodeAnalyzer(resolveSession);
        this.lazyTopDownAnalyzerForTopLevel.setLazyTopDownAnalyzer(lazyTopDownAnalyzer);

        javaClassFinder.setComponentPostConstruct(javaLazyAnalyzerPostConstruct);
        javaClassFinder.setProject(project);
        javaClassFinder.setScope(moduleContentScope);

        traceBasedExternalSignatureResolver.setExternalAnnotationResolver(psiBasedExternalAnnotationResolver);
        traceBasedExternalSignatureResolver.setProject(project);
        traceBasedExternalSignatureResolver.setTrace(bindingTrace);

        lazyResolveBasedCache.setSession(resolveSession);

        traceBasedErrorReporter.setTrace(bindingTrace);

        psiBasedMethodSignatureChecker.setExternalAnnotationResolver(psiBasedExternalAnnotationResolver);
        psiBasedMethodSignatureChecker.setExternalSignatureResolver(traceBasedExternalSignatureResolver);

        singleModuleClassResolver.setResolver(javaDescriptorResolver);

        javaLazyAnalyzerPostConstruct.setCodeAnalyzer(resolveSession);
        javaLazyAnalyzerPostConstruct.setProject(project);
        javaLazyAnalyzerPostConstruct.setTrace(bindingTrace);

        annotationResolver.setCallResolver(callResolver);
        annotationResolver.setStorageManager(storageManager);
        annotationResolver.setTypeResolver(typeResolver);

        callResolver.setArgumentTypeResolver(argumentTypeResolver);
        callResolver.setCallCompleter(callCompleter);
        callResolver.setCandidateResolver(candidateResolver);
        callResolver.setExpressionTypingServices(expressionTypingServices);
        callResolver.setTaskPrioritizer(taskPrioritizer);
        callResolver.setTypeResolver(typeResolver);

        argumentTypeResolver.setBuiltIns(kotlinBuiltIns);
        argumentTypeResolver.setExpressionTypingServices(expressionTypingServices);
        argumentTypeResolver.setTypeResolver(typeResolver);

        expressionTypingServices.setAnnotationResolver(annotationResolver);
        expressionTypingServices.setBuiltIns(kotlinBuiltIns);
        expressionTypingServices.setCallExpressionResolver(callExpressionResolver);
        expressionTypingServices.setCallResolver(callResolver);
        expressionTypingServices.setDescriptorResolver(descriptorResolver);
        expressionTypingServices.setFunctionDescriptorResolver(functionDescriptorResolver);
        expressionTypingServices.setProject(project);
        expressionTypingServices.setStatementFilter(statementFilter);
        expressionTypingServices.setTypeResolver(typeResolver);

        expressionTypingComponents.setAdditionalCheckerProvider(kotlinJvmCheckerProvider);
        expressionTypingComponents.setBuiltIns(kotlinBuiltIns);
        expressionTypingComponents.setCallResolver(callResolver);
        expressionTypingComponents.setControlStructureTypingUtils(controlStructureTypingUtils);
        expressionTypingComponents.setDynamicTypesSettings(dynamicTypesSettings);
        expressionTypingComponents.setExpressionTypingServices(expressionTypingServices);
        expressionTypingComponents.setExpressionTypingUtils(expressionTypingUtils);
        expressionTypingComponents.setForLoopConventionsChecker(forLoopConventionsChecker);
        expressionTypingComponents.setGlobalContext(globalContext);
        expressionTypingComponents.setLocalClassifierAnalyzer(localClassifierAnalyzer);
        expressionTypingComponents.setPlatformToKotlinClassMap(platformToKotlinClassMap);
        expressionTypingComponents.setReflectionTypes(reflectionTypes);

        forLoopConventionsChecker.setBuiltIns(kotlinBuiltIns);
        forLoopConventionsChecker.setExpressionTypingServices(expressionTypingServices);
        forLoopConventionsChecker.setExpressionTypingUtils(expressionTypingUtils);
        forLoopConventionsChecker.setProject(project);

        descriptorResolver.setAnnotationResolver(annotationResolver);
        descriptorResolver.setBuiltIns(kotlinBuiltIns);
        descriptorResolver.setDelegatedPropertyResolver(delegatedPropertyResolver);
        descriptorResolver.setExpressionTypingServices(expressionTypingServices);
        descriptorResolver.setStorageManager(storageManager);
        descriptorResolver.setTypeResolver(typeResolver);

        delegatedPropertyResolver.setBuiltIns(kotlinBuiltIns);
        delegatedPropertyResolver.setCallResolver(callResolver);
        delegatedPropertyResolver.setExpressionTypingServices(expressionTypingServices);

        callExpressionResolver.setExpressionTypingServices(expressionTypingServices);

        candidateResolver.setArgumentTypeResolver(argumentTypeResolver);

        jetImportsFactory.setProject(project);

        lazyDeclarationResolver.setDeclarationScopeProvider(declarationScopeProvider);
        lazyDeclarationResolver.setTopLevelDescriptorProvider(resolveSession);

        declarationScopeProvider.setFileScopeProvider(scopeProvider);

        scriptBodyResolver.setExpressionTypingServices(expressionTypingServices);

        bodyResolver.setAnnotationResolver(annotationResolver);
        bodyResolver.setCallResolver(callResolver);
        bodyResolver.setControlFlowAnalyzer(controlFlowAnalyzer);
        bodyResolver.setDeclarationsChecker(declarationsChecker);
        bodyResolver.setDelegatedPropertyResolver(delegatedPropertyResolver);
        bodyResolver.setExpressionTypingServices(expressionTypingServices);
        bodyResolver.setFunctionAnalyzerExtension(functionAnalyzerExtension);
        bodyResolver.setScriptBodyResolverResolver(scriptBodyResolver);
        bodyResolver.setTrace(bindingTrace);

        controlFlowAnalyzer.setTrace(bindingTrace);

        declarationsChecker.setDescriptorResolver(descriptorResolver);
        declarationsChecker.setModifiersChecker(modifiersChecker);
        declarationsChecker.setTrace(bindingTrace);

        functionAnalyzerExtension.setTrace(bindingTrace);

        declarationResolver.setAnnotationResolver(annotationResolver);
        declarationResolver.setTrace(bindingTrace);

        overloadResolver.setTrace(bindingTrace);

        overrideResolver.setTrace(bindingTrace);

        deserializedDescriptorResolver.setComponents(deserializationComponentsForJava);

        javaClassFinder.initialize();

        javaLazyAnalyzerPostConstruct.postCreate();

    }

    @PreDestroy
    public void destroy() {
    }

    public ModuleDescriptorImpl getModule() {
        return this.module;
    }

    public ResolveSession getResolveSession() {
        return this.resolveSession;
    }

    public LazyTopDownAnalyzer getLazyTopDownAnalyzer() {
        return this.lazyTopDownAnalyzer;
    }

    public LazyTopDownAnalyzerForTopLevel getLazyTopDownAnalyzerForTopLevel() {
        return this.lazyTopDownAnalyzerForTopLevel;
    }

    public JavaDescriptorResolver getJavaDescriptorResolver() {
        return this.javaDescriptorResolver;
    }

    public DeserializationComponentsForJava getDeserializationComponentsForJava() {
        return this.deserializationComponentsForJava;
    }

}
