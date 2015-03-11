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

package org.jetbrains.kotlin.checkers;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiElement;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.kotlin.context.GlobalContext;
import org.jetbrains.kotlin.context.SimpleGlobalContext;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.diagnostics.*;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.Call;
import org.jetbrains.kotlin.psi.JetElement;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.AnalyzingUtils;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.calls.model.MutableResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics;
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM;
import org.jetbrains.kotlin.resolve.lazy.LazyResolveTestUtil;
import org.jetbrains.kotlin.storage.ExceptionTracker;
import org.jetbrains.kotlin.storage.LockBasedStorageManager;
import org.jetbrains.kotlin.test.JetTestUtils;
import org.jetbrains.kotlin.test.util.DescriptorValidator;
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator;
import org.jetbrains.kotlin.utils.UtilsPackage;

import java.io.File;
import java.util.*;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator.RECURSIVE;
import static org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator.RECURSIVE_ALL;

public abstract class AbstractJetDiagnosticsTest extends BaseDiagnosticsTest {

    public static final Function1<String, String> HASH_SANITIZER = new Function1<String, String>() {
        @Override
        public String invoke(String s) {
            return s.replaceAll("@(\\d)+", "");
        }
    };

    @Override
    protected void analyzeAndCheck(File testDataFile, List<TestFile> testFiles) {
        Map<TestModule, List<TestFile>> groupedByModule = KotlinPackage.groupByTo(
                testFiles,
                new LinkedHashMap<TestModule, List<TestFile>>(),
                new Function1<TestFile, TestModule>() {
                    @Override
                    public TestModule invoke(TestFile file) {
                        return file.getModule();
                    }
                }
        );

        Map<TestModule, ModuleDescriptorImpl> modules = createModules(groupedByModule);
        Map<TestModule, BindingContext> moduleBindings = new HashMap<TestModule, BindingContext>();

        LazyOperationsLog lazyOperationsLog = new LazyOperationsLog(HASH_SANITIZER);

        for (Map.Entry<TestModule, List<TestFile>> entry : groupedByModule.entrySet()) {
            TestModule testModule = entry.getKey();
            List<? extends TestFile> testFilesInModule = entry.getValue();

            List<JetFile> jetFiles = getJetFiles(testFilesInModule, true);

            ModuleDescriptorImpl module = modules.get(testModule);
            BindingTrace moduleTrace = new CliLightClassGenerationSupport.NoScopeRecordCliBindingTrace();

            moduleBindings.put(testModule, moduleTrace.getBindingContext());

            ExceptionTracker tracker = new ExceptionTracker();
            GlobalContext context = new SimpleGlobalContext(
                    new LoggingStorageManager(
                            LockBasedStorageManager.createWithExceptionHandling(tracker),
                            lazyOperationsLog.getAddRecordFunction()
                    ),
                    tracker
            );
            analyzeModuleContents(context, jetFiles, module, moduleTrace);

            checkAllResolvedCallsAreCompleted(jetFiles, moduleTrace.getBindingContext());
        }

        // We want to always create a test data file (txt) if it was missing,
        // but don't want to skip the following checks in case this one fails
        Throwable exceptionFromLazyResolveLogValidation = null;
        if (KotlinPackage.any(testFiles, new Function1<TestFile, Boolean>() {
            @Override
            public Boolean invoke(TestFile file) {
                return file.checkLazyLog;
            }
        })) {
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
            ok &= testFile.getActualText(moduleBindings.get(testFile.getModule()), actualText, shouldSkipJvmSignatureDiagnostics(groupedByModule));
        }

        Throwable exceptionFromDynamicCallDescriptorsValidation = null;
        try {
            File expectedFile = new File(FileUtil.getNameWithoutExtension(testDataFile.getAbsolutePath()) + ".dynamic.txt");
            checkDynamicCallDescriptors(expectedFile, testFiles);
        }
        catch (Throwable e) {
            exceptionFromDynamicCallDescriptorsValidation = e;
        }

        JetTestUtils.assertEqualsToFile(testDataFile, actualText.toString());

        assertTrue("Diagnostics mismatch. See the output above", ok);

        // now we throw a previously found error, if any
        if (exceptionFromDescriptorValidation != null) {
            throw UtilsPackage.rethrow(exceptionFromDescriptorValidation);
        }
        if (exceptionFromLazyResolveLogValidation != null) {
            throw UtilsPackage.rethrow(exceptionFromLazyResolveLogValidation);
        }
        if (exceptionFromDynamicCallDescriptorsValidation != null) {
            throw UtilsPackage.rethrow(exceptionFromDynamicCallDescriptorsValidation);
        }
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
            JetTestUtils.assertEqualsToFile(expectedFile, actualText.toString());
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

            JetTestUtils.assertEqualsToFile(
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

    protected void analyzeModuleContents(
            GlobalContext context,
            List<JetFile> jetFiles,
            ModuleDescriptorImpl module,
            BindingTrace moduleTrace
    ) {
        // New JavaDescriptorResolver is created for each module, which is good because it emulates different Java libraries for each module,
        // albeit with same class names
        TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegrationWithCustomContext(
                getProject(),
                context,
                jetFiles,
                moduleTrace,
                module,
                null,
                null
        );
    }

    private void validateAndCompareDescriptorWithFile(
            File expectedFile,
            List<TestFile> testFiles,
            Map<TestModule, ModuleDescriptorImpl> modules
    ) {
        RecursiveDescriptorComparator comparator = new RecursiveDescriptorComparator(createdAffectedPackagesConfiguration(testFiles));

        boolean isMultiModuleTest = modules.size() != 1;
        StringBuilder rootPackageText = new StringBuilder();

        for (TestModule module : KotlinPackage.sort(modules.keySet())) {
            ModuleDescriptorImpl moduleDescriptor = modules.get(module);
            DeclarationDescriptor aPackage = moduleDescriptor.getPackage(FqName.ROOT);
            assertNotNull(aPackage);

            if (isMultiModuleTest) {
                rootPackageText.append(String.format("// -- Module: %s --\n", moduleDescriptor.getName()));
            }

            String actualSerialized = comparator.serializeRecursively(aPackage);
            rootPackageText.append(actualSerialized);

            if (isMultiModuleTest) {
                rootPackageText.append("\n\n");
            }
        }

        JetTestUtils.assertEqualsToFile(expectedFile, rootPackageText.toString());
    }

    public RecursiveDescriptorComparator.Configuration createdAffectedPackagesConfiguration(List<TestFile> testFiles) {
        final Set<Name> packagesNames = LazyResolveTestUtil.getTopLevelPackagesFromFileList(getJetFiles(testFiles, false));

        Predicate<DeclarationDescriptor> stepIntoFilter = new Predicate<DeclarationDescriptor>() {
            @Override
            public boolean apply(DeclarationDescriptor descriptor) {
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

    private Map<TestModule, ModuleDescriptorImpl> createModules(Map<TestModule, List<TestFile>> groupedByModule) {
        Map<TestModule, ModuleDescriptorImpl> modules = new HashMap<TestModule, ModuleDescriptorImpl>();

        for (TestModule testModule : groupedByModule.keySet()) {
            ModuleDescriptorImpl module =
                    testModule == null ?
                    createSealedModule() :
                    createModule("<" + testModule.getName() + ">");

            modules.put(testModule, module);
        }

        for (TestModule testModule : groupedByModule.keySet()) {
            if (testModule == null) continue;

            ModuleDescriptorImpl module = modules.get(testModule);
            module.addDependencyOnModule(module);
            for (TestModule dependency : testModule.getDependencies()) {
                module.addDependencyOnModule(modules.get(dependency));
            }

            module.addDependencyOnModule(KotlinBuiltIns.getInstance().getBuiltInsModule());
            module.seal();
        }

        return modules;
    }

    protected ModuleDescriptorImpl createModule(String moduleName) {
        return TopDownAnalyzerFacadeForJVM.createJavaModule(moduleName);
    }

    @NotNull
    protected ModuleDescriptorImpl createSealedModule() {
        return TopDownAnalyzerFacadeForJVM.createSealedJavaModule();
    }

    private static void checkAllResolvedCallsAreCompleted(@NotNull List<JetFile> jetFiles, @NotNull BindingContext bindingContext) {
        for (JetFile file : jetFiles) {
            if (!AnalyzingUtils.getSyntaxErrorRanges(file).isEmpty()) {
                return;
            }
        }

        ImmutableMap<Call, ResolvedCall<?>> resolvedCallsEntries = bindingContext.getSliceContents(BindingContext.RESOLVED_CALL);
        for (Map.Entry<Call, ResolvedCall<?>> entry : resolvedCallsEntries.entrySet()) {
            JetElement element = entry.getKey().getCallElement();
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
        Set<DiagnosticFactory2<JetExpression, ? extends Comparable<? extends Comparable<?>>, Collection<? extends ResolvedCall<?>>>>
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
