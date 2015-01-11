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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.AsJavaPackage;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.diagnostics.*;
import org.jetbrains.kotlin.psi.JetDeclaration;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics;
import org.jetbrains.kotlin.types.Flexibility;
import org.junit.Assert;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BaseDiagnosticsTest extends
                                          KotlinMultiFileTestWithWithJava<BaseDiagnosticsTest.TestModule, BaseDiagnosticsTest.TestFile> {

    public static final String DIAGNOSTICS_DIRECTIVE = "DIAGNOSTICS";
    public static final Pattern DIAGNOSTICS_PATTERN = Pattern.compile("([\\+\\-!])(\\w+)\\s*");
    public static final ImmutableSet<DiagnosticFactory<?>> DIAGNOSTICS_TO_INCLUDE_ANYWAY =
            ImmutableSet.of(
                    Errors.UNRESOLVED_REFERENCE,
                    Errors.UNRESOLVED_REFERENCE_WRONG_RECEIVER,
                    CheckerTestUtil.SyntaxErrorDiagnosticFactory.INSTANCE,
                    CheckerTestUtil.DebugInfoDiagnosticFactory.ELEMENT_WITH_ERROR_TYPE,
                    CheckerTestUtil.DebugInfoDiagnosticFactory.MISSING_UNRESOLVED,
                    CheckerTestUtil.DebugInfoDiagnosticFactory.UNRESOLVED_WITH_TARGET
            );

    public static final String CHECK_TYPE_DIRECTIVE = "CHECK_TYPE";
    public static final String CHECK_TYPE_PACKAGE = "tests._checkType";
    private static final String CHECK_TYPE_DECLARATIONS = "\npackage " + CHECK_TYPE_PACKAGE +
                                                          "\nclass _<T>" +
                                                          "\nfun <T> T.checkType(f: (_<T>) -> Unit) = f";
    public static final String CHECK_TYPE_IMPORT = "import " + CHECK_TYPE_PACKAGE + ".*";

    public static final String EXPLICIT_FLEXIBLE_TYPES_DIRECTIVE = "EXPLICIT_FLEXIBLE_TYPES";
    public static final String EXPLICIT_FLEXIBLE_PACKAGE = Flexibility.FLEXIBLE_TYPE_CLASSIFIER.getPackageFqName().asString();
    public static final String EXPLICIT_FLEXIBLE_CLASS_NAME = Flexibility.FLEXIBLE_TYPE_CLASSIFIER.getRelativeClassName().asString();
    private static final String EXPLICIT_FLEXIBLE_TYPES_DECLARATIONS
            = "\npackage " + EXPLICIT_FLEXIBLE_PACKAGE +
              "\npublic class " + EXPLICIT_FLEXIBLE_CLASS_NAME + "<L, U>";
    private static final String EXPLICIT_FLEXIBLE_TYPES_IMPORT = "import " + EXPLICIT_FLEXIBLE_PACKAGE + "." + EXPLICIT_FLEXIBLE_CLASS_NAME;
    public static final String CHECK_LAZY_LOG_DIRECTIVE = "CHECK_LAZY_LOG";
    public static final boolean CHECK_LAZY_LOG_DEFAULT = "true".equals(System.getProperty("check.lazy.logs", "false"));

    public static final String MARK_DYNAMIC_CALLS_DIRECTIVE = "MARK_DYNAMIC_CALLS";

    @Override
    protected TestModule createTestModule(@NotNull String name) {
        return new TestModule(name);
    }

    @Override
    protected TestFile createTestFile(TestModule module, String fileName, String text, Map<String, String> directives) {
        return new TestFile(module, fileName, text, directives);
    }

    @Override
    protected void doMultiFileTest(File file, final Map<String, ModuleAndDependencies> modules, List<TestFile> testFiles) {
        for (final ModuleAndDependencies moduleAndDependencies : modules.values()) {
            List<TestModule> dependencies = KotlinPackage.map(
                    moduleAndDependencies.dependencies,
                    new Function1<String, TestModule>() {
                        @Override
                        public TestModule invoke(String name) {
                            ModuleAndDependencies dependency = modules.get(name);
                            assert dependency != null : "Dependency not found: " +
                                                        name +
                                                        " for module " +
                                                        moduleAndDependencies.module.getName();
                            return dependency.module;
                        }
                    }
            );
            moduleAndDependencies.module.getDependencies().addAll(dependencies);
        }

        analyzeAndCheck(file, testFiles);
    }

    protected abstract void analyzeAndCheck(
            File testDataFile,
            List<TestFile> files
    );

    protected List<JetFile> getJetFiles(List<? extends TestFile> testFiles, boolean includeExtras) {
        boolean declareFlexibleType = false;
        boolean declareCheckType = false;
        List<JetFile> jetFiles = Lists.newArrayList();
        for (TestFile testFile : testFiles) {
            if (testFile.getJetFile() != null) {
                jetFiles.add(testFile.getJetFile());
            }
            declareFlexibleType |= testFile.declareFlexibleType;
            declareCheckType |= testFile.declareCheckType;
        }

        if (includeExtras) {
            if (declareFlexibleType) {
                jetFiles.add(createPsiFile(null, "EXPLICIT_FLEXIBLE_TYPES.kt", EXPLICIT_FLEXIBLE_TYPES_DECLARATIONS));
            }
            if (declareCheckType) {
                jetFiles.add(createPsiFile(null, "CHECK_TYPE.kt", CHECK_TYPE_DECLARATIONS));
            }
        }

        return jetFiles;
    }

    public static Condition<Diagnostic> parseDiagnosticFilterDirective(Map<String, String> directiveMap) {
        String directives = directiveMap.get(DIAGNOSTICS_DIRECTIVE);
        if (directives == null) {
            return Conditions.alwaysTrue();
        }
        Condition<Diagnostic> condition = Conditions.alwaysTrue();
        Matcher matcher = DIAGNOSTICS_PATTERN.matcher(directives);
        if (!matcher.find()) {
            Assert.fail("Wrong syntax in the '// !DIAGNOSTICS: ...' directive:\n" +
                        "found: '" + directives + "'\n" +
                        "Must be '([+-!]DIAGNOSTIC_FACTORY_NAME|ERROR|WARNING|INFO)+'\n" +
                        "where '+' means 'include'\n" +
                        "      '-' means 'exclude'\n" +
                        "      '!' means 'exclude everything but this'\n" +
                        "directives are applied in the order of appearance, i.e. !FOO +BAR means include only FOO and BAR");
        }
        boolean first = true;
        do {
            String operation = matcher.group(1);
            final String name = matcher.group(2);

            Condition<Diagnostic> newCondition;
            if (ImmutableSet.of("ERROR", "WARNING", "INFO").contains(name)) {
                final Severity severity = Severity.valueOf(name);
                newCondition = new Condition<Diagnostic>() {
                    @Override
                    public boolean value(Diagnostic diagnostic) {
                        return diagnostic.getSeverity() == severity;
                    }
                };
            }
            else {
                newCondition = new Condition<Diagnostic>() {
                    @Override
                    public boolean value(Diagnostic diagnostic) {
                        return name.equals(diagnostic.getFactory().getName());
                    }
                };
            }
            if ("!".equals(operation)) {
                if (!first) {
                    Assert.fail("'" + operation + name + "' appears in a position rather than the first one, " +
                                "which effectively cancels all the previous filters in this directive");
                }
                condition = newCondition;
            }
            else if ("+".equals(operation)) {
                condition = Conditions.or(condition, newCondition);
            }
            else if ("-".equals(operation)) {
                condition = Conditions.and(condition, Conditions.not(newCondition));
            }
            first = false;
        }
        while (matcher.find());
        // We always include UNRESOLVED_REFERENCE and SYNTAX_ERROR because they are too likely to indicate erroneous test data
        return Conditions.or(
                condition,
                new Condition<Diagnostic>() {
                    @Override
                    public boolean value(Diagnostic diagnostic) {
                        return DIAGNOSTICS_TO_INCLUDE_ANYWAY.contains(diagnostic.getFactory());
                    }
                });
    }

    protected static class TestModule implements Comparable<TestModule> {
        private final String name;
        private final List<TestModule> dependencies = new ArrayList<TestModule>();

        public TestModule(@NotNull String name) {
            this.name = name;
        }

        @NotNull
        public String getName() {
            return name;
        }

        @NotNull
        public List<TestModule> getDependencies() {
            return dependencies;
        }

        @Override
        public int compareTo(@NotNull TestModule module) {
            return name.compareTo(module.getName());
        }
    }

    protected class TestFile {
        private final List<CheckerTestUtil.DiagnosedRange> diagnosedRanges = Lists.newArrayList();
        private final String expectedText;
        private final TestModule module;
        private final String clearText;
        private final JetFile jetFile;
        private final Condition<Diagnostic> whatDiagnosticsToConsider;
        private final boolean declareCheckType;
        private final boolean declareFlexibleType;
        public final boolean checkLazyLog;
        private final boolean markDynamicCalls;
        private final List<DeclarationDescriptor> dynamicCallDescriptors = new ArrayList<DeclarationDescriptor>();

        public TestFile(
                @Nullable TestModule module,
                String fileName,
                String textWithMarkers,
                Map<String, String> directives
        ) {
            this.module = module;
            this.whatDiagnosticsToConsider = parseDiagnosticFilterDirective(directives);
            this.checkLazyLog = directives.containsKey(CHECK_LAZY_LOG_DIRECTIVE) || CHECK_LAZY_LOG_DEFAULT;
            this.declareCheckType = directives.containsKey(CHECK_TYPE_DIRECTIVE);
            this.declareFlexibleType = directives.containsKey(EXPLICIT_FLEXIBLE_TYPES_DIRECTIVE);
            this.markDynamicCalls = directives.containsKey(MARK_DYNAMIC_CALLS_DIRECTIVE);
            if (fileName.endsWith(".java")) {
                PsiFileFactory.getInstance(getProject()).createFileFromText(fileName, JavaLanguage.INSTANCE, textWithMarkers);
                // TODO: check there's not syntax errors
                this.jetFile = null;
                this.expectedText = this.clearText = textWithMarkers;
            }
            else {
                this.expectedText = textWithMarkers;
                String textWithExtras = addExtras(expectedText);
                this.clearText = CheckerTestUtil.parseDiagnosedRanges(textWithExtras, diagnosedRanges);
                this.jetFile = createCheckAndReturnPsiFile(null, fileName, clearText);
                for (CheckerTestUtil.DiagnosedRange diagnosedRange : diagnosedRanges) {
                    diagnosedRange.setFile(jetFile);
                }
            }
        }

        @NotNull
        private String getImports() {
            String imports = "";
            if (declareCheckType) {
                imports += CHECK_TYPE_IMPORT + "\n";
            }
            if (declareFlexibleType) {
                imports += EXPLICIT_FLEXIBLE_TYPES_IMPORT + "\n";
            }
            return imports;
        }

        private String getExtras() {
            return "/*extras*/\n" + getImports() + "/*extras*/\n\n";
        }

        private String addExtras(String text) {
            return addImports(text, getExtras());
        }

        private void stripExtras(StringBuilder actualText) {
            String extras = getExtras();
            int start = actualText.indexOf(extras);
            if (start >= 0) {
                actualText.delete(start, start + extras.length());
            }
        }

        private String addImports(String text, String imports) {
            Pattern pattern = Pattern.compile("^package [\\.\\w\\d]*\n", Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                // add imports after the package directive
                text = text.substring(0, matcher.end()) + imports + text.substring(matcher.end());
            }
            else {
                // add imports at the beginning
                text = imports + text;
            }
            return text;
        }

        @Nullable
        public TestModule getModule() {
            return module;
        }

        @Nullable
        public JetFile getJetFile() {
            return jetFile;
        }

        @NotNull
        public List<DeclarationDescriptor> getDynamicCallDescriptors() {
            return dynamicCallDescriptors;
        }

        public boolean getActualText(BindingContext bindingContext, StringBuilder actualText, boolean skipJvmSignatureDiagnostics) {
            if (this.jetFile == null) {
                // TODO: check java files too
                actualText.append(this.clearText);
                return true;
            }

            Set<Diagnostic> jvmSignatureDiagnostics = skipJvmSignatureDiagnostics
                                                            ? Collections.<Diagnostic>emptySet()
                                                            : computeJvmSignatureDiagnostics(bindingContext);

            final boolean[] ok = { true };
            List<Diagnostic> diagnostics = ContainerUtil.filter(
                    KotlinPackage.plus(CheckerTestUtil.getDiagnosticsIncludingSyntaxErrors(bindingContext, jetFile, markDynamicCalls, dynamicCallDescriptors),
                                       jvmSignatureDiagnostics),
                    whatDiagnosticsToConsider
            );

            Map<Diagnostic, CheckerTestUtil.TextDiagnostic> diagnosticToExpectedDiagnostic = ContainerUtil.newHashMap();
            CheckerTestUtil.diagnosticsDiff(diagnosticToExpectedDiagnostic, diagnosedRanges, diagnostics, new CheckerTestUtil.DiagnosticDiffCallbacks() {

                @Override
                public void missingDiagnostic(CheckerTestUtil.TextDiagnostic diagnostic, int expectedStart, int expectedEnd) {
                    String message = "Missing " + diagnostic.getName() + DiagnosticUtils.atLocation(jetFile, new TextRange(expectedStart, expectedEnd));
                    System.err.println(message);
                    ok[0] = false;
                }

                @Override
                public void wrongParametersDiagnostic(
                        CheckerTestUtil.TextDiagnostic expectedDiagnostic,
                        CheckerTestUtil.TextDiagnostic actualDiagnostic,
                        int start,
                        int end
                ) {
                    String message = "Parameters of diagnostic not equal at position "
                                     + DiagnosticUtils.atLocation(jetFile, new TextRange(start, end))
                                     + ". Expected: " + expectedDiagnostic.asString() + ", actual: " + actualDiagnostic.asString();
                    System.err.println(message);
                    ok[0] = false;
                }

                @Override
                public void unexpectedDiagnostic(CheckerTestUtil.TextDiagnostic diagnostic, int actualStart, int actualEnd) {
                    String message = "Unexpected " + diagnostic.getName() + DiagnosticUtils.atLocation(jetFile, new TextRange(actualStart, actualEnd));
                    System.err.println(message);
                    ok[0] = false;
                }
            });

            actualText.append(CheckerTestUtil.addDiagnosticMarkersToText(jetFile, diagnostics, diagnosticToExpectedDiagnostic, new Function<PsiFile, String>() {
                @Override
                public String fun(PsiFile file) {
                    return file.getText();
                }
            }));

            stripExtras(actualText);

            return ok[0];
        }

        private Set<Diagnostic> computeJvmSignatureDiagnostics(BindingContext bindingContext) {
            Set<Diagnostic> jvmSignatureDiagnostics = new HashSet<Diagnostic>();
            Collection<JetDeclaration> declarations = PsiTreeUtil.findChildrenOfType(jetFile, JetDeclaration.class);
            for (JetDeclaration declaration : declarations) {
                Diagnostics diagnostics = AsJavaPackage.getJvmSignatureDiagnostics(declaration, bindingContext.getDiagnostics(),
                                                                                   GlobalSearchScope.allScope(getProject()));
                if (diagnostics == null) continue;
                jvmSignatureDiagnostics.addAll(diagnostics.forElement(declaration));
            }
            return jvmSignatureDiagnostics;
        }
    }
}
