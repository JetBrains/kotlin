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

package org.jetbrains.jet.checkers;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentProvider;
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.diagnostics.*;
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.model.MutableResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.java.TopDownAnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.lazy.LazyResolveTestUtil;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.test.util.DescriptorValidator;
import org.jetbrains.jet.test.util.RecursiveDescriptorComparator;
import org.jetbrains.jet.utils.UtilsPackage;

import java.io.File;
import java.util.*;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.test.util.RecursiveDescriptorComparator.RECURSIVE;

public abstract class AbstractJetDiagnosticsTest extends BaseDiagnosticsTest {
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

        CliLightClassGenerationSupport support = CliLightClassGenerationSupport.getInstanceForCli(getProject());
        BindingTrace supportTrace = support.getTrace();

        List<JetFile> allJetFiles = new ArrayList<JetFile>();
        Map<TestModule, ModuleDescriptorImpl> modules = createModules(groupedByModule);
        Map<TestModule, BindingContext> moduleBindings = new HashMap<TestModule, BindingContext>();

        for (Map.Entry<TestModule, List<TestFile>> entry : groupedByModule.entrySet()) {
            TestModule testModule = entry.getKey();
            List<? extends TestFile> testFilesInModule = entry.getValue();

            List<JetFile> jetFiles = getJetFiles(testFilesInModule, true);
            allJetFiles.addAll(jetFiles);

            ModuleDescriptorImpl module = modules.get(testModule);
            BindingTrace moduleTrace = groupedByModule.size() > 1
                                           ? new DelegatingBindingTrace(supportTrace.getBindingContext(), "Trace for module " + module)
                                           : supportTrace;
            moduleBindings.put(testModule, moduleTrace.getBindingContext());

            if (module == null) {
                module = support.newModule();
                modules.put(entry.getKey(), module);
            }
            else {
                module.addDependencyOnModule(KotlinBuiltIns.getInstance().getBuiltInsModule());
                module.seal();
            }

            // New JavaDescriptorResolver is created for each module, which is good because it emulates different Java libraries for each module,
            // albeit with same class names
            TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                    getProject(),
                    jetFiles,
                    moduleTrace,
                    Predicates.<PsiFile>alwaysTrue(),
                    module,
                    null,
                    null
            );

            checkAllResolvedCallsAreCompleted(jetFiles, moduleTrace.getBindingContext());
        }

        // We want to always create a test data file (txt) if it was missing,
        // but don't want to skip the following checks in case this one fails
        Throwable exceptionFromDescriptorValidation = null;
        try {
            File expectedFile = new File(FileUtil.getNameWithoutExtension(testDataFile.getAbsolutePath()) + ".txt");
            validateAndCompareDescriptorWithFile(expectedFile, testFiles, support, modules);
        }
        catch (Throwable e) {
            exceptionFromDescriptorValidation = e;
        }

        // main checks
        boolean ok = true;

        StringBuilder actualText = new StringBuilder();
        for (TestFile testFile : testFiles) {
            ok &= testFile.getActualText(moduleBindings.get(testFile.getModule()), actualText, groupedByModule.size() > 1);
        }

        JetTestUtils.assertEqualsToFile(testDataFile, actualText.toString());

        assertTrue("Diagnostics mismatch. See the output above", ok);

        checkAllResolvedCallsAreCompleted(allJetFiles, supportTrace.getBindingContext());

        // now we throw a previously found error, if any
        if (exceptionFromDescriptorValidation != null) {
            throw UtilsPackage.rethrow(exceptionFromDescriptorValidation);
        }
    }

    private void validateAndCompareDescriptorWithFile(
            File expectedFile,
            List<TestFile> testFiles,
            CliLightClassGenerationSupport support,
            Map<TestModule, ModuleDescriptorImpl> modules
    ) {
        ModuleDescriptorImpl lightClassModule = support.getLightClassModule();
        if (lightClassModule == null) {
            ModuleDescriptorImpl cliModule = support.newModule();
            cliModule.initialize(new PackageFragmentProvider() {
                @NotNull
                @Override
                public List<PackageFragmentDescriptor> getPackageFragments(@NotNull FqName fqName) {
                    return Collections.emptyList();
                }

                @NotNull
                @Override
                public Collection<FqName> getSubPackagesOf(@NotNull FqName fqName) {
                    return Collections.emptyList();
                }
            });
        }

        RecursiveDescriptorComparator comparator = new RecursiveDescriptorComparator(createdAffectedPackagesConfiguration(testFiles));

        boolean isMultiModuleTest = modules.size() != 1;
        StringBuilder rootPackageText = new StringBuilder();

        for (TestModule module : KotlinPackage.sort(modules.keySet())) {
            ModuleDescriptorImpl moduleDescriptor = modules.get(module);
            if (isMultiModuleTest) {
                rootPackageText.append(String.format("// -- Module: %s --\n", moduleDescriptor.getName()));
            }

            DeclarationDescriptor aPackage = moduleDescriptor.getPackage(FqName.ROOT);
            assertNotNull(aPackage);

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

        return RECURSIVE.filterRecursion(stepIntoFilter).withValidationStrategy(DescriptorValidator.ValidationVisitor.ALLOW_ERROR_TYPES);
    }

    public static Map<TestModule, ModuleDescriptorImpl> createModules(Map<TestModule, List<TestFile>> groupedByModule) {
        Map<TestModule, ModuleDescriptorImpl> modules = new HashMap<TestModule, ModuleDescriptorImpl>();

        for (TestModule testModule : groupedByModule.keySet()) {
            if (testModule == null) continue;
            ModuleDescriptorImpl module = TopDownAnalyzerFacadeForJVM.createJavaModule("<" + testModule.getName() + ">");
            modules.put(testModule, module);
        }

        for (TestModule testModule : groupedByModule.keySet()) {
            if (testModule == null) continue;

            ModuleDescriptorImpl module = modules.get(testModule);
            module.addDependencyOnModule(module);
            for (TestModule dependency : testModule.getDependencies()) {
                module.addDependencyOnModule(modules.get(dependency));
            }
        }
        return modules;
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
