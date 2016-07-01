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

package org.jetbrains.kotlin.checkers;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import kotlin.TuplesKt;
import kotlin.collections.CollectionsKt;
import kotlin.collections.MapsKt;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.analyzer.ModuleContent;
import org.jetbrains.kotlin.analyzer.ModuleInfo;
import org.jetbrains.kotlin.analyzer.common.DefaultAnalyzerFacade;
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider;
import org.jetbrains.kotlin.config.CommonConfigurationKeys;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.LanguageVersionSettings;
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl;
import org.jetbrains.kotlin.container.ComponentProvider;
import org.jetbrains.kotlin.container.DslKt;
import org.jetbrains.kotlin.context.ContextKt;
import org.jetbrains.kotlin.context.GlobalContext;
import org.jetbrains.kotlin.context.ModuleContext;
import org.jetbrains.kotlin.context.SimpleGlobalContext;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.descriptors.PackagePartProvider;
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor;
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.diagnostics.*;
import org.jetbrains.kotlin.frontend.java.di.InjectionKt;
import org.jetbrains.kotlin.incremental.components.LookupTracker;
import org.jetbrains.kotlin.load.java.lazy.SingleModuleClassResolver;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.name.SpecialNames;
import org.jetbrains.kotlin.platform.JvmBuiltIns;
import org.jetbrains.kotlin.psi.Call;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.*;
import org.jetbrains.kotlin.resolve.calls.model.MutableResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics;
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver;
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM;
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer;
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory;
import org.jetbrains.kotlin.storage.ExceptionTracker;
import org.jetbrains.kotlin.storage.LockBasedStorageManager;
import org.jetbrains.kotlin.storage.StorageManager;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.util.DescriptorValidator;
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;
import org.junit.Assert;

import java.io.File;
import java.util.*;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator.RECURSIVE;
import static org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator.RECURSIVE_ALL;

public abstract class AbstractDiagnosticsTest extends BaseDiagnosticsTest {

    private static final Function1<String, String> HASH_SANITIZER = new Function1<String, String>() {
        @Override
        public String invoke(String s) {
            return s.replaceAll("@(\\d)+", "");
        }
    };

    private static final ModuleDescriptor.Capability<List<KtFile>> MODULE_FILES = new ModuleDescriptor.Capability<List<KtFile>>("");

    @Override
    protected void analyzeAndCheck(File testDataFile, List<TestFile> testFiles) {
        Map<TestModule, List<TestFile>> groupedByModule = CollectionsKt.groupByTo(
                testFiles,
                new LinkedHashMap<TestModule, List<TestFile>>(),
                new Function1<TestFile, TestModule>() {
                    @Override
                    public TestModule invoke(TestFile file) {
                        return file.getModule();
                    }
                }
        );

        boolean checkLazyResolveLog = CollectionsKt.any(testFiles, new Function1<TestFile, Boolean>() {
            @Override
            public Boolean invoke(TestFile file) {
                return file.checkLazyLog;
            }
        });

        LazyOperationsLog lazyOperationsLog = null;
        GlobalContext context;

        ExceptionTracker tracker = new ExceptionTracker();
        if (checkLazyResolveLog) {
            lazyOperationsLog = new LazyOperationsLog(HASH_SANITIZER);
            context = new SimpleGlobalContext(
                    new LoggingStorageManager(
                            LockBasedStorageManager.createWithExceptionHandling(tracker),
                            lazyOperationsLog.getAddRecordFunction()
                    ),
                    tracker
            );
        }
        else {
            context = new SimpleGlobalContext(LockBasedStorageManager.createWithExceptionHandling(tracker), tracker);
        }

        Map<TestModule, ModuleDescriptorImpl> modules = createModules(groupedByModule, context.getStorageManager());
        Map<TestModule, BindingContext> moduleBindings = new HashMap<TestModule, BindingContext>();

        for (Map.Entry<TestModule, List<TestFile>> entry : groupedByModule.entrySet()) {
            TestModule testModule = entry.getKey();
            List<? extends TestFile> testFilesInModule = entry.getValue();

            List<KtFile> jetFiles = getJetFiles(testFilesInModule, true);

            ModuleDescriptorImpl oldModule = modules.get(testModule);

            LanguageVersionSettings languageVersionSettings = loadLanguageVersionSettings(testFilesInModule);
            ModuleContext moduleContext = ContextKt.withModule(ContextKt.withProject(context, getProject()), oldModule);

            boolean separateModules = groupedByModule.size() == 1 && groupedByModule.keySet().iterator().next() == null;
            AnalysisResult result = analyzeModuleContents(
                    moduleContext, jetFiles, new CliLightClassGenerationSupport.NoScopeRecordCliBindingTrace(),
                    languageVersionSettings, separateModules
            );
            ModuleDescriptorImpl newModule = (ModuleDescriptorImpl) result.getModuleDescriptor();
            if (oldModule != newModule) {
                // For common modules, we use DefaultAnalyzerFacade who creates ModuleDescriptor instances by itself
                // (its API does not support working with a module created beforehand).
                // So, we should replace the old (effectively discarded) module with the new one everywhere in dependencies.
                // TODO: dirty hack, refactor this test so that it doesn't create ModuleDescriptor instances
                modules.put(testModule, newModule);
                for (ModuleDescriptorImpl module : modules.values()) {
                    @SuppressWarnings("deprecation")
                    ListIterator<ModuleDescriptorImpl> it = module.getTestOnly_AllDependentModules().listIterator();
                    while (it.hasNext()) {
                        if (it.next() == oldModule) {
                            it.set(newModule);
                        }
                    }
                }
            }

            moduleBindings.put(testModule, result.getBindingContext());
            checkAllResolvedCallsAreCompleted(jetFiles, result.getBindingContext());
        }

        // We want to always create a test data file (txt) if it was missing,
        // but don't want to skip the following checks in case this one fails
        Throwable exceptionFromLazyResolveLogValidation = null;
        if (checkLazyResolveLog) {
            exceptionFromLazyResolveLogValidation = checkLazyResolveLog(lazyOperationsLog, testDataFile);
        }
        else {
            File lazyLogFile = getLazyLogFile(testDataFile);
            assertFalse("No lazy log expected, but found: " + lazyLogFile.getAbsolutePath(), lazyLogFile.exists());
        }

        Throwable exceptionFromDescriptorValidation = null;
        try {
            File expectedFile = new File(FileUtil.getNameWithoutExtension(testDataFile.getAbsolutePath()) + ".txt");
            validateAndCompareDescriptorWithFile(expectedFile, testFiles, modules);
        }
        catch (Throwable e) {
            exceptionFromDescriptorValidation = e;
        }

        // main checks
        boolean ok = true;

        StringBuilder actualText = new StringBuilder();
        for (TestFile testFile : testFiles) {
            TestModule module = testFile.getModule();
            boolean isCommonModule = MultiTargetPlatformKt.getMultiTargetPlatform(modules.get(module)) == MultiTargetPlatform.Common.INSTANCE;
            ok &= testFile.getActualText(
                    moduleBindings.get(module), actualText,
                    shouldSkipJvmSignatureDiagnostics(groupedByModule) || isCommonModule
            );
        }

        Throwable exceptionFromDynamicCallDescriptorsValidation = null;
        try {
            File expectedFile = new File(FileUtil.getNameWithoutExtension(testDataFile.getAbsolutePath()) + ".dynamic.txt");
            checkDynamicCallDescriptors(expectedFile, testFiles);
        }
        catch (Throwable e) {
            exceptionFromDynamicCallDescriptorsValidation = e;
        }

        KotlinTestUtils.assertEqualsToFile(testDataFile, actualText.toString());

        assertTrue("Diagnostics mismatch. See the output above", ok);

        // now we throw a previously found error, if any
        if (exceptionFromDescriptorValidation != null) {
            throw ExceptionUtilsKt.rethrow(exceptionFromDescriptorValidation);
        }
        if (exceptionFromLazyResolveLogValidation != null) {
            throw ExceptionUtilsKt.rethrow(exceptionFromLazyResolveLogValidation);
        }
        if (exceptionFromDynamicCallDescriptorsValidation != null) {
            throw ExceptionUtilsKt.rethrow(exceptionFromDynamicCallDescriptorsValidation);
        }

        performAdditionalChecksAfterDiagnostics(testDataFile, testFiles, groupedByModule, modules, moduleBindings);
    }

    protected void performAdditionalChecksAfterDiagnostics(
            File testDataFile,
            List<TestFile> testFiles,
            Map<TestModule, List<TestFile>> moduleFiles,
            Map<TestModule, ModuleDescriptorImpl> moduleDescriptors,
            Map<TestModule, BindingContext> moduleBindings
    ) {
        // To be overridden by diagnostic-like tests.
    }

    @Nullable
    private LanguageVersionSettings loadLanguageVersionSettings(List<? extends TestFile> module) {
        LanguageVersionSettings result = null;
        for (TestFile file : module) {
            LanguageVersionSettings current = file.customLanguageVersionSettings;
            if (current != null) {
                if (result != null && !result.equals(current)) {
                    Assert.fail(
                            "More than one file in the module has " + BaseDiagnosticsTest.LANGUAGE_DIRECTIVE + " or " +
                            BaseDiagnosticsTest.API_VERSION_DIRECTIVE + " directive specified. " +
                            "This is not supported. Please move all directives into one file"
                    );
                }
                result = current;
            }
        }

        return result;
    }

    private void checkDynamicCallDescriptors(File expectedFile, List<TestFile> testFiles) {
        RecursiveDescriptorComparator serializer = new RecursiveDescriptorComparator(RECURSIVE_ALL);

        StringBuilder actualText = new StringBuilder();

        for (TestFile testFile : testFiles) {
            List<DeclarationDescriptor> dynamicCallDescriptors = testFile.getDynamicCallDescriptors();

            for (DeclarationDescriptor descriptor : dynamicCallDescriptors) {
                String actualSerialized = serializer.serializeRecursively(descriptor);
                actualText.append(actualSerialized);
            }
        }

        if (actualText.length() != 0 || expectedFile.exists()) {
            KotlinTestUtils.assertEqualsToFile(expectedFile, actualText.toString());
        }
    }

    public boolean shouldSkipJvmSignatureDiagnostics(Map<TestModule, List<TestFile>> groupedByModule) {
        return groupedByModule.size() > 1;
    }

    @Nullable
    private static Throwable checkLazyResolveLog(LazyOperationsLog lazyOperationsLog, File testDataFile) {
        Throwable exceptionFromLazyResolveLogValidation = null;
        try {
            File expectedFile = getLazyLogFile(testDataFile);

            KotlinTestUtils.assertEqualsToFile(
                    expectedFile,
                    lazyOperationsLog.getText(),
                    HASH_SANITIZER
            );
        }
        catch (Throwable e) {
            exceptionFromLazyResolveLogValidation = e;
        }
        return exceptionFromLazyResolveLogValidation;
    }

    private static File getLazyLogFile(File testDataFile) {
        return new File(FileUtil.getNameWithoutExtension(testDataFile.getAbsolutePath()) + ".lazy.log");
    }

    @NotNull
    protected AnalysisResult analyzeModuleContents(
            @NotNull ModuleContext moduleContext,
            @NotNull List<KtFile> files,
            @NotNull BindingTrace moduleTrace,
            @Nullable LanguageVersionSettings languageVersionSettings,
            boolean separateModules
    ) {
        CompilerConfiguration configuration;
        if (languageVersionSettings != null) {
            configuration = getEnvironment().getConfiguration().copy();
            configuration.put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, languageVersionSettings);
        }
        else {
            configuration = getEnvironment().getConfiguration();
            languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT;
        }

        // New JavaDescriptorResolver is created for each module, which is good because it emulates different Java libraries for each module,
        // albeit with same class names
        // See TopDownAnalyzerFacadeForJVM#analyzeFilesWithJavaIntegration

        // Temporary solution: only use separate module mode in single-module tests because analyzeFilesWithJavaIntegration
        // only supports creating two modules, whereas there can be more than two in multi-module diagnostic tests
        // TODO: always use separate module mode, once analyzeFilesWithJavaIntegration can create multiple modules
        if (separateModules) {
            return TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                    moduleContext.getProject(),
                    files,
                    moduleTrace,
                    configuration,
                    new Function1<GlobalSearchScope, PackagePartProvider>() {
                        @Override
                        public PackagePartProvider invoke(GlobalSearchScope scope) {
                            return new JvmPackagePartProvider(getEnvironment(), scope);
                        }
                    }
            );
        }

        ModuleDescriptorImpl moduleDescriptor = (ModuleDescriptorImpl) moduleContext.getModule();

        MultiTargetPlatform platform = MultiTargetPlatformKt.getMultiTargetPlatform(moduleDescriptor);
        if (platform == MultiTargetPlatform.Common.INSTANCE) {
            //noinspection unchecked
            return DefaultAnalyzerFacade.INSTANCE.analyzeFiles(
                    files, moduleDescriptor.getName(), true,
                    MapsKt.mapOf(
                            TuplesKt.to(MultiTargetPlatform.CAPABILITY, MultiTargetPlatform.Common.INSTANCE),
                            TuplesKt.to(MODULE_FILES, files)
                    ),
                    new Function2<ModuleInfo, ModuleContent, PackagePartProvider>() {
                        @Override
                        public PackagePartProvider invoke(ModuleInfo info, ModuleContent content) {
                            // TODO
                            return PackagePartProvider.Empty.INSTANCE;
                        }
                    }
            );
        }
        else if (platform != null) {
            // TODO: analyze with the correct platform, not always JVM
            files = CollectionsKt.plus(files, getCommonCodeFilesForPlatformSpecificModule(moduleDescriptor));
        }

        GlobalSearchScope moduleContentScope = GlobalSearchScope.allScope(moduleContext.getProject());
        SingleModuleClassResolver moduleClassResolver = new SingleModuleClassResolver();
        ComponentProvider container = InjectionKt.createContainerForTopDownAnalyzerForJvm(
                moduleContext,
                moduleTrace,
                new FileBasedDeclarationProviderFactory(moduleContext.getStorageManager(), files),
                moduleContentScope,
                LookupTracker.Companion.getDO_NOTHING(),
                new JvmPackagePartProvider(getEnvironment(), moduleContentScope),
                languageVersionSettings,
                moduleClassResolver
        );
        InjectionKt.initJvmBuiltInsForTopDownAnalysis(container, moduleDescriptor, languageVersionSettings);
        moduleClassResolver.setResolver(DslKt.getService(container, JavaDescriptorResolver.class));

        moduleDescriptor.initialize(new CompositePackageFragmentProvider(Arrays.asList(
                DslKt.getService(container, KotlinCodeAnalyzer.class).getPackageFragmentProvider(),
                DslKt.getService(container, JavaDescriptorResolver.class).getPackageFragmentProvider()
        )));

        DslKt.getService(container, LazyTopDownAnalyzer.class).analyzeDeclarations(
                TopDownAnalysisMode.TopLevelDeclarations, files, DataFlowInfo.Companion.getEMPTY()
        );

        return AnalysisResult.success(moduleTrace.getBindingContext(), moduleDescriptor);
    }

    @NotNull
    private static List<KtFile> getCommonCodeFilesForPlatformSpecificModule(@NotNull ModuleDescriptorImpl moduleDescriptor) {
        // We assume that a platform-specific module _implements_ all declarations from common modules which are immediate dependencies.
        // So we collect all sources from such modules to analyze in the platform-specific module as well
        @SuppressWarnings("deprecation")
        List<ModuleDescriptorImpl> dependencies = moduleDescriptor.getTestOnly_AllDependentModules();

        // TODO: diagnostics on common code reported during the platform module analysis should be distinguished somehow
        // E.g. "<!JVM:PLATFORM_DEFINITION_WITHOUT_DECLARATION!>...<!>
        List<KtFile> result = new ArrayList<KtFile>(0);
        for (ModuleDescriptorImpl dependency : dependencies) {
            if (dependency.getCapability(MultiTargetPlatform.CAPABILITY) == MultiTargetPlatform.Common.INSTANCE) {
                List<KtFile> files = dependency.getCapability(MODULE_FILES);
                assert files != null : "MODULE_FILES should have been set for the common module: " + dependency;
                result.addAll(files);
            }
        }

        return result;
    }

    private void validateAndCompareDescriptorWithFile(
            File expectedFile,
            List<TestFile> testFiles,
            Map<TestModule, ModuleDescriptorImpl> modules
    ) {
        if (CollectionsKt.any(testFiles, new Function1<TestFile, Boolean>() {
            @Override
            public Boolean invoke(TestFile file) {
                return InTextDirectivesUtils.isDirectiveDefined(file.expectedText, "// SKIP_TXT");
            }
        })) {
            assertFalse(".txt file should not exist if SKIP_TXT directive is used: " + expectedFile, expectedFile.exists());
            return;
        }

        RecursiveDescriptorComparator comparator = new RecursiveDescriptorComparator(createdAffectedPackagesConfiguration(testFiles, modules.values()));

        boolean isMultiModuleTest = modules.size() != 1;
        StringBuilder rootPackageText = new StringBuilder();

        for (Iterator<TestModule> module = CollectionsKt.sorted(modules.keySet()).iterator(); module.hasNext(); ) {
            ModuleDescriptorImpl moduleDescriptor = modules.get(module.next());
            PackageViewDescriptor aPackage = moduleDescriptor.getPackage(FqName.ROOT);
            assertFalse(aPackage.isEmpty());

            if (isMultiModuleTest) {
                rootPackageText.append(String.format("// -- Module: %s --\n", moduleDescriptor.getName()));
            }

            String actualSerialized = comparator.serializeRecursively(aPackage);
            rootPackageText.append(actualSerialized);

            if (isMultiModuleTest && module.hasNext()) {
                rootPackageText.append("\n\n");
            }
        }

        int lineCount = StringUtil.getLineBreakCount(rootPackageText);
        assert lineCount < 1000 :
                "Rendered descriptors of this test take up " + lineCount + " lines. " +
                "Please ensure you don't render JRE contents to the .txt file. " +
                "Such tests are hard to maintain, take long time to execute and are subject to sudden unreviewed changes anyway.";

        KotlinTestUtils.assertEqualsToFile(expectedFile, rootPackageText.toString());
    }

    private RecursiveDescriptorComparator.Configuration createdAffectedPackagesConfiguration(List<TestFile> testFiles, final Collection<? extends ModuleDescriptor> modules) {
        final Set<Name> packagesNames = getTopLevelPackagesFromFileList(getJetFiles(testFiles, false));

        Predicate<DeclarationDescriptor> stepIntoFilter = new Predicate<DeclarationDescriptor>() {
            @Override
            public boolean apply(DeclarationDescriptor descriptor) {
                ModuleDescriptor module = DescriptorUtils.getContainingModuleOrNull(descriptor);
                if (!modules.contains(module)) return false;

                if (descriptor instanceof PackageViewDescriptor) {
                    FqName fqName = ((PackageViewDescriptor) descriptor).getFqName();

                    if (fqName.isRoot()) return true;

                    Name firstName = fqName.pathSegments().get(0);
                    return packagesNames.contains(firstName);
                }

                return true;
            }
        };

        return RECURSIVE.filterRecursion(stepIntoFilter).withValidationStrategy(DescriptorValidator.ValidationVisitor.errorTypesAllowed());
    }

    @NotNull
    private static Set<Name> getTopLevelPackagesFromFileList(@NotNull List<KtFile> files) {
        Set<Name> shortNames = new LinkedHashSet<Name>();
        for (KtFile file : files) {
            List<Name> packageFqNameSegments = file.getPackageFqName().pathSegments();
            Name name = packageFqNameSegments.isEmpty() ? SpecialNames.ROOT_PACKAGE : packageFqNameSegments.get(0);
            shortNames.add(name);
        }
        return shortNames;
    }

    private Map<TestModule, ModuleDescriptorImpl> createModules(
            @NotNull Map<TestModule, List<TestFile>> groupedByModule,
            @NotNull StorageManager storageManager
    ) {
        Map<TestModule, ModuleDescriptorImpl> modules = new HashMap<TestModule, ModuleDescriptorImpl>();

        for (TestModule testModule : groupedByModule.keySet()) {
            ModuleDescriptorImpl module =
                    testModule == null ?
                    createSealedModule(storageManager) :
                    createModule(testModule.getName(), storageManager);

            modules.put(testModule, module);
        }

        for (TestModule testModule : groupedByModule.keySet()) {
            if (testModule == null) continue;

            ModuleDescriptorImpl module = modules.get(testModule);
            List<ModuleDescriptorImpl> dependencies = new ArrayList<ModuleDescriptorImpl>();
            dependencies.add(module);
            for (TestModule dependency : testModule.getDependencies()) {
                dependencies.add(modules.get(dependency));
            }

            dependencies.add(module.getBuiltIns().getBuiltInsModule());
            dependencies.addAll(getAdditionalDependencies(module));
            module.setDependencies(dependencies);
        }

        return modules;
    }

    @NotNull
    protected List<ModuleDescriptorImpl> getAdditionalDependencies(@NotNull ModuleDescriptorImpl module) {
        return Collections.emptyList();
    }

    @NotNull
    @SuppressWarnings("unchecked")
    protected ModuleDescriptorImpl createModule(@NotNull String moduleName, @NotNull StorageManager storageManager) {
        String nameSuffix = StringsKt.substringAfterLast(moduleName, "-", "");
        MultiTargetPlatform platform =
                nameSuffix.isEmpty() ? null :
                nameSuffix.equals("common") ? MultiTargetPlatform.Common.INSTANCE : new MultiTargetPlatform.Specific(nameSuffix);
        Map capabilities =
                platform == null
                ? Collections.emptyMap()
                : Collections.singletonMap(MultiTargetPlatform.CAPABILITY, platform);
        return new ModuleDescriptorImpl(
                Name.special("<" + moduleName + ">"), storageManager, new JvmBuiltIns(storageManager), capabilities
        );
    }

    @NotNull
    protected ModuleDescriptorImpl createSealedModule(@NotNull StorageManager storageManager) {
        ModuleDescriptorImpl moduleDescriptor = createModule("test-module", storageManager);
        moduleDescriptor.setDependencies(moduleDescriptor, moduleDescriptor.getBuiltIns().getBuiltInsModule());
        return moduleDescriptor;
    }

    private static void checkAllResolvedCallsAreCompleted(@NotNull List<KtFile> jetFiles, @NotNull BindingContext bindingContext) {
        for (KtFile file : jetFiles) {
            if (!AnalyzingUtils.getSyntaxErrorRanges(file).isEmpty()) {
                return;
            }
        }

        ImmutableMap<Call, ResolvedCall<?>> resolvedCallsEntries = bindingContext.getSliceContents(BindingContext.RESOLVED_CALL);
        for (Map.Entry<Call, ResolvedCall<?>> entry : resolvedCallsEntries.entrySet()) {
            KtElement element = entry.getKey().getCallElement();
            ResolvedCall<?> resolvedCall = entry.getValue();

            DiagnosticUtils.LineAndColumn lineAndColumn =
                    DiagnosticUtils.getLineAndColumnInPsiFile(element.getContainingFile(), element.getTextRange());

            assertTrue("Resolved call for '" + element.getText() + "'" + lineAndColumn + " is not completed",
                       ((MutableResolvedCall<?>) resolvedCall).isCompleted());
        }

        checkResolvedCallsInDiagnostics(bindingContext);
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    private static void checkResolvedCallsInDiagnostics(BindingContext bindingContext) {
        Set<DiagnosticFactory1<PsiElement, Collection<? extends ResolvedCall<?>>>> diagnosticsStoringResolvedCalls1 = Sets.newHashSet(
                OVERLOAD_RESOLUTION_AMBIGUITY, NONE_APPLICABLE, CANNOT_COMPLETE_RESOLVE, UNRESOLVED_REFERENCE_WRONG_RECEIVER,
                ASSIGN_OPERATOR_AMBIGUITY, ITERATOR_AMBIGUITY);
        Set<DiagnosticFactory2<KtExpression, ? extends Comparable<? extends Comparable<?>>, Collection<? extends ResolvedCall<?>>>>
                diagnosticsStoringResolvedCalls2 = Sets.newHashSet(
                COMPONENT_FUNCTION_AMBIGUITY, DELEGATE_SPECIAL_FUNCTION_AMBIGUITY, DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE);
        Diagnostics diagnostics = bindingContext.getDiagnostics();
        for (Diagnostic diagnostic : diagnostics) {
            DiagnosticFactory<?> factory = diagnostic.getFactory();
            //noinspection SuspiciousMethodCalls
            if (diagnosticsStoringResolvedCalls1.contains(factory)) {
                assertResolvedCallsAreCompleted(
                        diagnostic, DiagnosticFactory.cast(diagnostic, diagnosticsStoringResolvedCalls1).getA());
            }
            //noinspection SuspiciousMethodCalls
            if (diagnosticsStoringResolvedCalls2.contains(factory)) {
                assertResolvedCallsAreCompleted(
                        diagnostic,
                        DiagnosticFactory.cast(diagnostic, diagnosticsStoringResolvedCalls2).getB());
            }
        }
    }

    private static void assertResolvedCallsAreCompleted(
            @NotNull Diagnostic diagnostic, @NotNull Collection<? extends ResolvedCall<?>> resolvedCalls
    ) {
        boolean allCallsAreCompleted = true;
        for (ResolvedCall<?> resolvedCall : resolvedCalls) {
            if (!((MutableResolvedCall<?>) resolvedCall).isCompleted()) {
                allCallsAreCompleted = false;
            }
        }

        PsiElement element = diagnostic.getPsiElement();
        DiagnosticUtils.LineAndColumn lineAndColumn =
                DiagnosticUtils.getLineAndColumnInPsiFile(element.getContainingFile(), element.getTextRange());

        assertTrue("Resolved calls stored in " + diagnostic.getFactory().getName() + "\n" +
                   "for '" + element.getText() + "'" + lineAndColumn + " are not completed",
                   allCallsAreCompleted);
    }
}
