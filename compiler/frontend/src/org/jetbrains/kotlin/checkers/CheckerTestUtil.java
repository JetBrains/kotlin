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
import com.google.common.collect.Collections2;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Function;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.Stack;
import kotlin.Pair;
import kotlin.TuplesKt;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory;
import org.jetbrains.kotlin.diagnostics.Severity;
import org.jetbrains.kotlin.diagnostics.rendering.AbstractDiagnosticWithParametersRenderer;
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticRenderer;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtReferenceExpression;
import org.jetbrains.kotlin.resolve.AnalyzingUtils;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.MultiTargetPlatform;
import org.jetbrains.kotlin.util.slicedMap.WritableSlice;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CheckerTestUtil {
    public static final Comparator<ActualDiagnostic> DIAGNOSTIC_COMPARATOR = new Comparator<ActualDiagnostic>() {
        @Override
        public int compare(@NotNull ActualDiagnostic o1, @NotNull ActualDiagnostic o2) {
            List<TextRange> ranges1 = o1.diagnostic.getTextRanges();
            List<TextRange> ranges2 = o2.diagnostic.getTextRanges();
            int minNumberOfRanges = ranges1.size() < ranges2.size() ? ranges1.size() : ranges2.size();
            for (int i = 0; i < minNumberOfRanges; i++) {
                TextRange range1 = ranges1.get(i);
                TextRange range2 = ranges2.get(i);
                int startOffset1 = range1.getStartOffset();
                int startOffset2 = range2.getStartOffset();
                if (startOffset1 != startOffset2) {
                    // Start early -- go first
                    return startOffset1 - range2.getStartOffset();
                }
                int endOffset1 = range1.getEndOffset();
                int endOffset2 = range2.getEndOffset();
                if (endOffset1 != endOffset2) {
                    // start at the same offset, the one who end later is the outer, i.e. goes first
                    return endOffset2 - endOffset1;
                }
            }
            return ranges1.size() - ranges2.size();
        }
    };

    private static final String IGNORE_DIAGNOSTIC_PARAMETER = "IGNORE";
    private static final String SHOULD_BE_ESCAPED = "\\)\\(;";
    private static final String DIAGNOSTIC_PARAMETER = "(?:(?:\\\\[" + SHOULD_BE_ESCAPED + "])|[^" + SHOULD_BE_ESCAPED + "])+";
    private static final String INDIVIDUAL_DIAGNOSTIC = "(\\w+:)?(\\w+)(\\(" + DIAGNOSTIC_PARAMETER + "(;\\s*" + DIAGNOSTIC_PARAMETER + ")*\\))?";
    private static final Pattern RANGE_START_OR_END_PATTERN = Pattern.compile("(<!" +
                                                                              INDIVIDUAL_DIAGNOSTIC + "(,\\s*" +
                                                                              INDIVIDUAL_DIAGNOSTIC + ")*!>)|(<!>)");
    private static final Pattern INDIVIDUAL_DIAGNOSTIC_PATTERN = Pattern.compile(INDIVIDUAL_DIAGNOSTIC);
    private static final Pattern INDIVIDUAL_PARAMETER_PATTERN = Pattern.compile(DIAGNOSTIC_PARAMETER);

    @NotNull
    public static List<ActualDiagnostic> getDiagnosticsIncludingSyntaxErrors(
            @NotNull BindingContext bindingContext,
            @NotNull List<Pair<MultiTargetPlatform, BindingContext>> implementingModulesBindings,
            @NotNull PsiElement root,
            boolean markDynamicCalls,
            @Nullable List<DeclarationDescriptor> dynamicCallDescriptors
    ) {
        List<ActualDiagnostic> result =
                getDiagnosticsIncludingSyntaxErrors(bindingContext, root, markDynamicCalls, dynamicCallDescriptors, null);

        List<Pair<MultiTargetPlatform, BindingContext>> sortedBindings = CollectionsKt.sortedWith(
                implementingModulesBindings,
                new Comparator<Pair<MultiTargetPlatform, BindingContext>>() {
                    @Override
                    public int compare(Pair<MultiTargetPlatform, BindingContext> o1, Pair<MultiTargetPlatform, BindingContext> o2) {
                        return o1.getFirst().compareTo(o2.getFirst());
                    }
                }
        );

        for (Pair<MultiTargetPlatform, BindingContext> binding : sortedBindings) {
            MultiTargetPlatform platform = binding.getFirst();
            assert platform instanceof MultiTargetPlatform.Specific : "Implementing module must have a specific platform: " + platform;

            result.addAll(getDiagnosticsIncludingSyntaxErrors(
                    binding.getSecond(), root, markDynamicCalls, dynamicCallDescriptors,
                    ((MultiTargetPlatform.Specific) platform).getPlatform()
            ));
        }

        return result;
    }

    @NotNull
    public static List<ActualDiagnostic> getDiagnosticsIncludingSyntaxErrors(
            @NotNull BindingContext bindingContext,
            @NotNull PsiElement root,
            boolean markDynamicCalls,
            @Nullable List<DeclarationDescriptor> dynamicCallDescriptors,
            @Nullable String platform
    ) {
        List<ActualDiagnostic> diagnostics = new ArrayList<ActualDiagnostic>();
        for (Diagnostic diagnostic : bindingContext.getDiagnostics().all()) {
            if (PsiTreeUtil.isAncestor(root, diagnostic.getPsiElement(), false)) {
                diagnostics.add(new ActualDiagnostic(diagnostic, platform));
            }
        }

        for (PsiErrorElement errorElement : AnalyzingUtils.getSyntaxErrorRanges(root)) {
            diagnostics.add(new ActualDiagnostic(new SyntaxErrorDiagnostic(errorElement), platform));
        }

        diagnostics.addAll(getDebugInfoDiagnostics(root, bindingContext, markDynamicCalls, dynamicCallDescriptors, platform));
        return diagnostics;
    }

    @SuppressWarnings("TestOnlyProblems")
    @NotNull
    private static List<ActualDiagnostic> getDebugInfoDiagnostics(
            @NotNull PsiElement root,
            @NotNull BindingContext bindingContext,
            final boolean markDynamicCalls,
            @Nullable final List<DeclarationDescriptor> dynamicCallDescriptors,
            @Nullable final String platform
    ) {
        final List<ActualDiagnostic> debugAnnotations = new ArrayList<ActualDiagnostic>();

        DebugInfoUtil.markDebugAnnotations(root, bindingContext, new DebugInfoUtil.DebugInfoReporter() {
            @Override
            public void reportElementWithErrorType(@NotNull KtReferenceExpression expression) {
                newDiagnostic(expression, DebugInfoDiagnosticFactory.ELEMENT_WITH_ERROR_TYPE);
            }

            @Override
            public void reportMissingUnresolved(@NotNull KtReferenceExpression expression) {
                newDiagnostic(expression, DebugInfoDiagnosticFactory.MISSING_UNRESOLVED);
            }

            @Override
            public void reportUnresolvedWithTarget(@NotNull KtReferenceExpression expression, @NotNull String target) {
                newDiagnostic(expression, DebugInfoDiagnosticFactory.UNRESOLVED_WITH_TARGET);
            }

            @Override
            public void reportDynamicCall(@NotNull KtElement element, DeclarationDescriptor declarationDescriptor) {
                if (dynamicCallDescriptors != null) {
                    dynamicCallDescriptors.add(declarationDescriptor);
                }

                if (markDynamicCalls) {
                    newDiagnostic(element, DebugInfoDiagnosticFactory.DYNAMIC);
                }
            }

            private void newDiagnostic(KtElement element, DebugInfoDiagnosticFactory factory) {
                debugAnnotations.add(new ActualDiagnostic(new DebugInfoDiagnostic(element, factory), platform));
            }
        });

        // this code is used in tests and in internal action 'copy current file as diagnostic test'
        //noinspection unchecked
        for (Pair<? extends WritableSlice<? extends KtExpression, ?>, DebugInfoDiagnosticFactory> factory : Arrays.asList(
                TuplesKt.to(BindingContext.SMARTCAST, DebugInfoDiagnosticFactory.SMARTCAST),
                TuplesKt.to(BindingContext.IMPLICIT_RECEIVER_SMARTCAST, DebugInfoDiagnosticFactory.IMPLICIT_RECEIVER_SMARTCAST),
                TuplesKt.to(BindingContext.SMARTCAST_NULL, DebugInfoDiagnosticFactory.CONSTANT),
                TuplesKt.to(BindingContext.LEAKING_THIS, DebugInfoDiagnosticFactory.LEAKING_THIS),
                TuplesKt.to(BindingContext.IMPLICIT_EXHAUSTIVE_WHEN, DebugInfoDiagnosticFactory.IMPLICIT_EXHAUSTIVE)
        )) {
            for (KtExpression expression : bindingContext.getSliceContents(factory.getFirst()).keySet()) {
                if (PsiTreeUtil.isAncestor(root, expression, false)) {
                    debugAnnotations.add(new ActualDiagnostic(new DebugInfoDiagnostic(expression, factory.getSecond()), platform));
                }
            }
        }

        return debugAnnotations;
    }

    public interface DiagnosticDiffCallbacks {
        void missingDiagnostic(TextDiagnostic diagnostic, int expectedStart, int expectedEnd);

        void wrongParametersDiagnostic(TextDiagnostic expectedDiagnostic, TextDiagnostic actualDiagnostic, int start, int end);

        void unexpectedDiagnostic(TextDiagnostic diagnostic, int actualStart, int actualEnd);
    }

    public static Map<ActualDiagnostic, TextDiagnostic> diagnosticsDiff(
            List<DiagnosedRange> expected,
            Collection<ActualDiagnostic> actual,
            DiagnosticDiffCallbacks callbacks
    ) {
        Map<ActualDiagnostic, TextDiagnostic> diagnosticToExpectedDiagnostic = new HashMap<ActualDiagnostic, TextDiagnostic>();

        assertSameFile(actual);

        Iterator<DiagnosedRange> expectedDiagnostics = expected.iterator();
        List<DiagnosticDescriptor> sortedDiagnosticDescriptors = getSortedDiagnosticDescriptors(actual);
        Iterator<DiagnosticDescriptor> actualDiagnostics = sortedDiagnosticDescriptors.iterator();

        DiagnosedRange currentExpected = safeAdvance(expectedDiagnostics);
        DiagnosticDescriptor currentActual = safeAdvance(actualDiagnostics);
        while (currentExpected != null || currentActual != null) {
            if (currentExpected != null) {
                if (currentActual == null) {
                    missingDiagnostics(callbacks, currentExpected);
                    currentExpected = safeAdvance(expectedDiagnostics);
                }
                else {
                    int expectedStart = currentExpected.getStart();
                    int actualStart = currentActual.getStart();
                    int expectedEnd = currentExpected.getEnd();
                    int actualEnd = currentActual.getEnd();
                    if (expectedStart < actualStart) {
                        missingDiagnostics(callbacks, currentExpected);
                        currentExpected = safeAdvance(expectedDiagnostics);
                    }
                    else if (expectedStart > actualStart) {
                        unexpectedDiagnostics(currentActual, callbacks);
                        currentActual = safeAdvance(actualDiagnostics);
                    }
                    else if (expectedEnd > actualEnd) {
                        assert expectedStart == actualStart;
                        missingDiagnostics(callbacks, currentExpected);
                        currentExpected = safeAdvance(expectedDiagnostics);
                    }
                    else if (expectedEnd < actualEnd) {
                        assert expectedStart == actualStart;
                        unexpectedDiagnostics(currentActual, callbacks);
                        currentActual = safeAdvance(actualDiagnostics);
                    }
                    else {
                        compareDiagnostics(callbacks, currentExpected, currentActual, diagnosticToExpectedDiagnostic);
                        currentExpected = safeAdvance(expectedDiagnostics);
                        currentActual = safeAdvance(actualDiagnostics);
                    }
                }
            }
            else {
                //noinspection ConstantConditions
                assert (currentActual != null);

                unexpectedDiagnostics(currentActual, callbacks);
                currentActual = safeAdvance(actualDiagnostics);
            }
        }

        return diagnosticToExpectedDiagnostic;
    }

    private static void compareDiagnostics(
            @NotNull DiagnosticDiffCallbacks callbacks,
            @NotNull DiagnosedRange currentExpected,
            @NotNull DiagnosticDescriptor currentActual,
            @NotNull Map<ActualDiagnostic, TextDiagnostic> diagnosticToInput
    ) {
        int expectedStart = currentExpected.getStart();
        int expectedEnd = currentExpected.getEnd();

        int actualStart = currentActual.getStart();
        int actualEnd = currentActual.getEnd();
        assert expectedStart == actualStart && expectedEnd == actualEnd;

        Map<ActualDiagnostic, TextDiagnostic> actualDiagnostics = currentActual.getTextDiagnosticsMap();
        List<TextDiagnostic> expectedDiagnostics = currentExpected.getDiagnostics();

        for (final TextDiagnostic expectedDiagnostic : expectedDiagnostics) {
            Map.Entry<ActualDiagnostic, TextDiagnostic> actualDiagnosticEntry = CollectionsKt.firstOrNull(
                    actualDiagnostics.entrySet(), new Function1<Map.Entry<ActualDiagnostic, TextDiagnostic>, Boolean>() {
                        @Override
                        public Boolean invoke(Map.Entry<ActualDiagnostic, TextDiagnostic> entry) {
                            return expectedDiagnostic.getDescription().equals(entry.getValue().getDescription());
                        }
                    }
            );

            if (actualDiagnosticEntry != null) {
                ActualDiagnostic actualDiagnostic = actualDiagnosticEntry.getKey();
                TextDiagnostic actualTextDiagnostic = actualDiagnosticEntry.getValue();

                if (!compareTextDiagnostic(expectedDiagnostic, actualTextDiagnostic)) {
                    callbacks.wrongParametersDiagnostic(expectedDiagnostic, actualTextDiagnostic, expectedStart, expectedEnd);
                }

                actualDiagnostics.remove(actualDiagnostic);
                diagnosticToInput.put(actualDiagnostic, expectedDiagnostic);
            }
            else {
                callbacks.missingDiagnostic(expectedDiagnostic, expectedStart, expectedEnd);
            }
        }

        for (TextDiagnostic unexpectedDiagnostic : actualDiagnostics.values()) {
            callbacks.unexpectedDiagnostic(unexpectedDiagnostic, actualStart, actualEnd);
        }
    }

    private static boolean compareTextDiagnostic(@NotNull TextDiagnostic expected, @NotNull TextDiagnostic actual) {
        if (!expected.getDescription().equals(actual.getDescription())) return false;

        if (expected.getParameters() == null) return true;
        if (actual.getParameters() == null || expected.getParameters().size() != actual.getParameters().size()) return false;

        for (int index = 0; index < expected.getParameters().size(); index++) {
            String expectedParameter = expected.getParameters().get(index);
            String actualParameter = actual.getParameters().get(index);
            if (!expectedParameter.equals(IGNORE_DIAGNOSTIC_PARAMETER) && !expectedParameter.equals(actualParameter)) {
                return false;
            }
        }
        return true;
    }

    private static void assertSameFile(Collection<ActualDiagnostic> actual) {
        if (actual.isEmpty()) return;
        PsiFile file = CollectionsKt.first(actual).getFile();
        for (ActualDiagnostic actualDiagnostic : actual) {
            assert actualDiagnostic.getFile().equals(file)
                    : "All diagnostics should come from the same file: " + actualDiagnostic.getFile() + ", " + file;
        }
    }

    private static void unexpectedDiagnostics(DiagnosticDescriptor descriptor, DiagnosticDiffCallbacks callbacks) {
        for (ActualDiagnostic diagnostic : descriptor.diagnostics) {
            callbacks.unexpectedDiagnostic(TextDiagnostic.asTextDiagnostic(diagnostic), descriptor.start, descriptor.end);
        }
    }

    private static void missingDiagnostics(DiagnosticDiffCallbacks callbacks, DiagnosedRange currentExpected) {
        for (TextDiagnostic diagnostic : currentExpected.getDiagnostics()) {
            callbacks.missingDiagnostic(diagnostic, currentExpected.getStart(), currentExpected.getEnd());
        }
    }

    private static <T> T safeAdvance(Iterator<T> iterator) {
        return iterator.hasNext() ? iterator.next() : null;
    }

    public static String parseDiagnosedRanges(String text, List<DiagnosedRange> result) {
        Matcher matcher = RANGE_START_OR_END_PATTERN.matcher(text);

        Stack<DiagnosedRange> opened = new Stack<DiagnosedRange>();

        int offsetCompensation = 0;

        while (matcher.find()) {
            int effectiveOffset = matcher.start() - offsetCompensation;
            String matchedText = matcher.group();
            if ("<!>".equals(matchedText)) {
                opened.pop().setEnd(effectiveOffset);
            }
            else {
                Matcher diagnosticTypeMatcher = INDIVIDUAL_DIAGNOSTIC_PATTERN.matcher(matchedText);
                DiagnosedRange range = new DiagnosedRange(effectiveOffset);
                while (diagnosticTypeMatcher.find()) {
                    range.addDiagnostic(diagnosticTypeMatcher.group());
                }
                opened.push(range);
                result.add(range);
            }
            offsetCompensation += matchedText.length();
        }

        assert opened.isEmpty() : "Stack is not empty";

        matcher.reset();
        return matcher.replaceAll("");
    }

    public static StringBuffer addDiagnosticMarkersToText(@NotNull PsiFile psiFile, @NotNull Collection<ActualDiagnostic> diagnostics) {
        return addDiagnosticMarkersToText(
                psiFile, diagnostics, Collections.<ActualDiagnostic, TextDiagnostic>emptyMap(),
                new Function<PsiFile, String>() {
                    @Override
                    public String fun(PsiFile file) {
                        return file.getText();
                    }
                }
        );
    }

    public static StringBuffer addDiagnosticMarkersToText(
            @NotNull final PsiFile psiFile,
            @NotNull Collection<ActualDiagnostic> diagnostics,
            @NotNull Map<ActualDiagnostic, TextDiagnostic> diagnosticToExpectedDiagnostic,
            @NotNull Function<PsiFile, String> getFileText
    ) {
        String text = getFileText.fun(psiFile);
        StringBuffer result = new StringBuffer();
        diagnostics = Collections2.filter(diagnostics, new Predicate<ActualDiagnostic>() {
            @Override
            public boolean apply(ActualDiagnostic actualDiagnostic) {
                return psiFile.equals(actualDiagnostic.getFile());
            }
        });
        if (!diagnostics.isEmpty()) {
            List<DiagnosticDescriptor> diagnosticDescriptors = getSortedDiagnosticDescriptors(diagnostics);

            Stack<DiagnosticDescriptor> opened = new Stack<DiagnosticDescriptor>();
            ListIterator<DiagnosticDescriptor> iterator = diagnosticDescriptors.listIterator();
            DiagnosticDescriptor currentDescriptor = iterator.next();

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                while (!opened.isEmpty() && i == opened.peek().end) {
                    closeDiagnosticString(result);
                    opened.pop();
                }
                while (currentDescriptor != null && i == currentDescriptor.start) {
                    openDiagnosticsString(result, currentDescriptor, diagnosticToExpectedDiagnostic);
                    if (currentDescriptor.getEnd() == i) {
                        closeDiagnosticString(result);
                    }
                    else {
                        opened.push(currentDescriptor);
                    }
                    if (iterator.hasNext()) {
                        currentDescriptor = iterator.next();
                    }
                    else {
                        currentDescriptor = null;
                    }
                }
                result.append(c);
            }

            if (currentDescriptor != null) {
                assert currentDescriptor.start == text.length();
                assert currentDescriptor.end == text.length();
                openDiagnosticsString(result, currentDescriptor, diagnosticToExpectedDiagnostic);
                opened.push(currentDescriptor);
            }

            while (!opened.isEmpty() && text.length() == opened.peek().end) {
                closeDiagnosticString(result);
                opened.pop();
            }

            assert opened.isEmpty() : "Stack is not empty: " + opened;
        }
        else {
            result.append(text);
        }
        return result;
    }

    private static void openDiagnosticsString(
            StringBuffer result,
            DiagnosticDescriptor currentDescriptor,
            Map<ActualDiagnostic, TextDiagnostic> diagnosticToExpectedDiagnostic
    ) {
        result.append("<!");
        for (Iterator<ActualDiagnostic> iterator = currentDescriptor.diagnostics.iterator(); iterator.hasNext(); ) {
            ActualDiagnostic diagnostic = iterator.next();
            TextDiagnostic expectedDiagnostic = diagnosticToExpectedDiagnostic.get(diagnostic);
            if (expectedDiagnostic != null) {
                TextDiagnostic actualTextDiagnostic = TextDiagnostic.asTextDiagnostic(diagnostic);
                if (compareTextDiagnostic(expectedDiagnostic, actualTextDiagnostic)) {
                    result.append(expectedDiagnostic.asString());
                }
                else {
                    result.append(actualTextDiagnostic.asString());
                }
            }
            else {
                if (diagnostic.platform != null) {
                    result.append(diagnostic.platform);
                    result.append(":");
                }
                result.append(diagnostic.getName());
            }
            if (iterator.hasNext()) {
                result.append(", ");
            }
        }
        result.append("!>");
    }

    private static void closeDiagnosticString(StringBuffer result) {
        result.append("<!>");
    }

    public static class AbstractDiagnosticForTests implements Diagnostic {
        private final PsiElement element;
        private final DiagnosticFactory<?> factory;

        public AbstractDiagnosticForTests(@NotNull PsiElement element, @NotNull DiagnosticFactory<?> factory) {
            this.element = element;
            this.factory = factory;
        }

        @NotNull
        @Override
        public DiagnosticFactory<?> getFactory() {
            return factory;
        }

        @NotNull
        @Override
        public Severity getSeverity() {
            return Severity.ERROR;
        }

        @NotNull
        @Override
        public PsiElement getPsiElement() {
            return element;
        }

        @NotNull
        @Override
        public List<TextRange> getTextRanges() {
            return Collections.singletonList(element.getTextRange());
        }

        @NotNull
        @Override
        public PsiFile getPsiFile() {
            return element.getContainingFile();
        }

        @Override
        public boolean isValid() {
            return true;
        }
    }

    public static class SyntaxErrorDiagnosticFactory extends DiagnosticFactory<SyntaxErrorDiagnostic> {
        public static final SyntaxErrorDiagnosticFactory INSTANCE = new SyntaxErrorDiagnosticFactory();

        private SyntaxErrorDiagnosticFactory() {
            super(Severity.ERROR);
        }

        @NotNull
        @Override
        public String getName() {
            return "SYNTAX";
        }
    }

    public static class SyntaxErrorDiagnostic extends AbstractDiagnosticForTests {
        public SyntaxErrorDiagnostic(@NotNull PsiErrorElement errorElement) {
            super(errorElement, SyntaxErrorDiagnosticFactory.INSTANCE);
        }
    }

    public static class DebugInfoDiagnosticFactory extends DiagnosticFactory<DebugInfoDiagnostic> {
        public static final DebugInfoDiagnosticFactory SMARTCAST = new DebugInfoDiagnosticFactory("SMARTCAST");
        public static final DebugInfoDiagnosticFactory IMPLICIT_RECEIVER_SMARTCAST = new DebugInfoDiagnosticFactory("IMPLICIT_RECEIVER_SMARTCAST");
        public static final DebugInfoDiagnosticFactory CONSTANT = new DebugInfoDiagnosticFactory("CONSTANT");
        public static final DebugInfoDiagnosticFactory LEAKING_THIS = new DebugInfoDiagnosticFactory("LEAKING_THIS");
        public static final DebugInfoDiagnosticFactory IMPLICIT_EXHAUSTIVE = new DebugInfoDiagnosticFactory("IMPLICIT_EXHAUSTIVE");
        public static final DebugInfoDiagnosticFactory ELEMENT_WITH_ERROR_TYPE = new DebugInfoDiagnosticFactory("ELEMENT_WITH_ERROR_TYPE");
        public static final DebugInfoDiagnosticFactory UNRESOLVED_WITH_TARGET = new DebugInfoDiagnosticFactory("UNRESOLVED_WITH_TARGET");
        public static final DebugInfoDiagnosticFactory MISSING_UNRESOLVED = new DebugInfoDiagnosticFactory("MISSING_UNRESOLVED");
        public static final DebugInfoDiagnosticFactory DYNAMIC = new DebugInfoDiagnosticFactory("DYNAMIC");

        private final String name;

        private DebugInfoDiagnosticFactory(String name, Severity severity) {
            super(severity);
            this.name = name;
        }

        private DebugInfoDiagnosticFactory(String name) {
            this(name, Severity.ERROR);
        }

        @NotNull
        @Override
        public String getName() {
            return "DEBUG_INFO_" + name;
        }
    }

    public static class DebugInfoDiagnostic extends AbstractDiagnosticForTests {
        public DebugInfoDiagnostic(@NotNull KtElement element, @NotNull DebugInfoDiagnosticFactory factory) {
            super(element, factory);
        }
    }

    @NotNull
    private static List<DiagnosticDescriptor> getSortedDiagnosticDescriptors(@NotNull Collection<ActualDiagnostic> diagnostics) {
        LinkedListMultimap<TextRange, ActualDiagnostic> diagnosticsGroupedByRanges = LinkedListMultimap.create();
        for (ActualDiagnostic actualDiagnostic : diagnostics) {
            Diagnostic diagnostic = actualDiagnostic.diagnostic;
            if (!diagnostic.isValid()) continue;
            for (TextRange textRange : diagnostic.getTextRanges()) {
                diagnosticsGroupedByRanges.put(textRange, actualDiagnostic);
            }
        }
        List<DiagnosticDescriptor> diagnosticDescriptors = Lists.newArrayList();
        for (TextRange range : diagnosticsGroupedByRanges.keySet()) {
            diagnosticDescriptors.add(
                    new DiagnosticDescriptor(range.getStartOffset(), range.getEndOffset(), diagnosticsGroupedByRanges.get(range)));
        }
        Collections.sort(diagnosticDescriptors, new Comparator<DiagnosticDescriptor>() {
            @Override
            public int compare(@NotNull DiagnosticDescriptor d1, @NotNull DiagnosticDescriptor d2) {
                // Start early -- go first; start at the same offset, the one who end later is the outer, i.e. goes first
                return (d1.start != d2.start) ? d1.start - d2.start : d2.end - d1.end;
            }
        });
        return diagnosticDescriptors;
    }

    private static class DiagnosticDescriptor {
        private final int start;
        private final int end;
        private final List<ActualDiagnostic> diagnostics;

        DiagnosticDescriptor(int start, int end, List<ActualDiagnostic> diagnostics) {
            this.start = start;
            this.end = end;
            this.diagnostics = diagnostics;
        }

        public Map<ActualDiagnostic, TextDiagnostic> getTextDiagnosticsMap() {
            Map<ActualDiagnostic, TextDiagnostic> diagnosticMap = new HashMap<ActualDiagnostic, TextDiagnostic>();
            for (ActualDiagnostic diagnostic : diagnostics) {
                diagnosticMap.put(diagnostic, TextDiagnostic.asTextDiagnostic(diagnostic));
            }
            return diagnosticMap;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public TextRange getTextRange() {
            return new TextRange(start, end);
        }
    }

    public static class ActualDiagnostic {
        public final Diagnostic diagnostic;
        public final String platform;

        ActualDiagnostic(@NotNull Diagnostic diagnostic, @Nullable String platform) {
            this.diagnostic = diagnostic;
            this.platform = platform;
        }

        @NotNull
        public String getName() {
            return diagnostic.getFactory().getName();
        }

        @NotNull
        public PsiFile getFile() {
            return diagnostic.getPsiFile();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ActualDiagnostic)) return false;

            ActualDiagnostic other = (ActualDiagnostic) obj;
            // '==' on diagnostics is intentional here
            return other.diagnostic == diagnostic &&
                   (other.platform == null ? platform == null : other.platform.equals(platform));
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(diagnostic) * 31 + (platform != null ? platform.hashCode() : 0);
        }

        @Override
        public String toString() {
            return (platform != null ? platform + ":" : "") + diagnostic.toString();
        }
    }

    public static class TextDiagnostic {
        @NotNull
        private static TextDiagnostic parseDiagnostic(String text) {
            Matcher matcher = INDIVIDUAL_DIAGNOSTIC_PATTERN.matcher(text);
            if (!matcher.find())
                throw new IllegalArgumentException("Could not parse diagnostic: " + text);

            String platformPrefix = matcher.group(1);
            assert platformPrefix == null || platformPrefix.endsWith(":") : platformPrefix;
            String platform = platformPrefix == null ? null : StringsKt.substringBeforeLast(platformPrefix, ":", platformPrefix);

            String name = matcher.group(2);
            String parameters = matcher.group(3);
            if (parameters == null) {
                return new TextDiagnostic(name, platform, null);
            }

            List<String> parsedParameters = new SmartList<String>();
            Matcher parametersMatcher = INDIVIDUAL_PARAMETER_PATTERN.matcher(parameters);
            while (parametersMatcher.find())
                parsedParameters.add(unescape(parametersMatcher.group().trim()));
            return new TextDiagnostic(name, platform, parsedParameters);
        }

        private static @NotNull String escape(@NotNull String s) {
            return s.replaceAll("([" + SHOULD_BE_ESCAPED + "])", "\\\\$1");
        }

        private static @NotNull String unescape(@NotNull String s) {
            return s.replaceAll("\\\\([" + SHOULD_BE_ESCAPED + "])", "$1");
        }

        @NotNull
        public static TextDiagnostic asTextDiagnostic(@NotNull ActualDiagnostic actualDiagnostic) {
            Diagnostic diagnostic = actualDiagnostic.diagnostic;
            //noinspection TestOnlyProblems
            DiagnosticRenderer renderer = DefaultErrorMessages.getRendererForDiagnostic(diagnostic);
            String diagnosticName = actualDiagnostic.getName();
            if (renderer instanceof AbstractDiagnosticWithParametersRenderer) {
                //noinspection unchecked
                Object[] renderParameters = ((AbstractDiagnosticWithParametersRenderer) renderer).renderParameters(diagnostic);
                List<String> parameters = ContainerUtil.map(renderParameters, new Function<Object, String>() {
                    @Override
                    public String fun(Object o) {
                        return o != null ? o.toString() : "null";
                    }
                });
                return new TextDiagnostic(diagnosticName, actualDiagnostic.platform, parameters);
            }
            return new TextDiagnostic(diagnosticName, actualDiagnostic.platform, null);
        }

        @NotNull
        private final String name;
        @Nullable
        private final String platform;
        @Nullable
        private final List<String> parameters;

        public TextDiagnostic(@NotNull String name, @Nullable String platform, @Nullable List<String> parameters) {
            this.name = name;
            this.platform = platform;
            this.parameters = parameters;
        }

        @Nullable
        public String getPlatform() {
            return platform;
        }

        @NotNull
        public String getDescription() {
            return (platform != null ? platform + ":" : "") + name;
        }

        @Nullable
        public List<String> getParameters() {
            return parameters;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TextDiagnostic that = (TextDiagnostic) o;

            if (!name.equals(that.name)) return false;
            if (platform != null ? !platform.equals(that.platform) : that.platform != null) return false;
            if (parameters != null ? !parameters.equals(that.parameters) : that.parameters != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + (platform != null ? platform.hashCode() : 0);
            result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
            return result;
        }

        @NotNull
        public String asString() {
            StringBuilder result = new StringBuilder();
            if (platform != null) {
                result.append(platform);
                result.append(":");
            }
            result.append(name);
            if (parameters != null) {
                result.append("(");
                result.append(StringUtil.join(parameters, new Function<String, String>() {
                    @Override
                    public String fun(String s) {
                        return escape(s);
                    }
                }, "; "));
                result.append(")");
            }
            return result.toString();
        }

        @Override
        public String toString() {
            return asString();
        }
    }

    public static class DiagnosedRange {
        private final int start;
        private int end;
        private final List<TextDiagnostic> diagnostics = ContainerUtil.newSmartList();

        protected DiagnosedRange(int start) {
            this.start = start;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public List<TextDiagnostic> getDiagnostics() {
            return diagnostics;
        }

        public void setEnd(int end) {
            this.end = end;
        }

        public void addDiagnostic(String diagnostic) {
            diagnostics.add(TextDiagnostic.parseDiagnostic(diagnostic));
        }
    }
}
