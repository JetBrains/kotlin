/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.AbstractDiagnosticFactory;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.Severity;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author abreslav
 */
public class CheckerTestUtil {
    public static final Comparator<Diagnostic> DIAGNOSTIC_COMPARATOR = new Comparator<Diagnostic>() {
        @Override
        public int compare(Diagnostic o1, Diagnostic o2) {
            List<TextRange> ranges1 = o1.getTextRanges();
            List<TextRange> ranges2 = o2.getTextRanges();
            if (ranges1.size() != ranges2.size()) return ranges1.size() - ranges2.size();
            for (int i = 0; i < ranges1.size(); i++) {
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
            return 0;
        }
    };
    private static final Pattern RANGE_START_OR_END_PATTERN = Pattern.compile("(<!\\w+(,\\s*\\w+)*!>)|(<!>)");
    private static final Pattern INDIVIDUAL_DIAGNOSTIC_PATTERN = Pattern.compile("\\w+");

    public static List<Diagnostic> getDiagnosticsIncludingSyntaxErrors(BindingContext bindingContext, PsiElement root) {
        ArrayList<Diagnostic> diagnostics = new ArrayList<Diagnostic>();
        diagnostics.addAll(bindingContext.getDiagnostics());
        for (PsiErrorElement errorElement : AnalyzingUtils.getSyntaxErrorRanges(root)) {
            diagnostics.add(new SyntaxErrorDiagnostic(errorElement));
        }
        return diagnostics;
    }

    public interface DiagnosticDiffCallbacks {
        @NotNull PsiFile getFile();
        void missingDiagnostic(String type, int expectedStart, int expectedEnd);
        void unexpectedDiagnostic(String type, int actualStart, int actualEnd);
    }

    public static void diagnosticsDiff(List<DiagnosedRange> expected, Collection<Diagnostic> actual, DiagnosticDiffCallbacks callbacks) {
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
                        unexpectedDiagnostics(currentActual.getDiagnostics(), callbacks);
                        currentActual = safeAdvance(actualDiagnostics);
                    }
                    else if (expectedEnd > actualEnd) {
                        assert expectedStart == actualStart;
                        missingDiagnostics(callbacks, currentExpected);
                        currentExpected = safeAdvance(expectedDiagnostics);
                    }
                    else if (expectedEnd < actualEnd) {
                        assert expectedStart == actualStart;
                        unexpectedDiagnostics(currentActual.getDiagnostics(), callbacks);
                        currentActual = safeAdvance(actualDiagnostics);
                    }
                    else {
                        assert expectedStart == actualStart && expectedEnd == actualEnd;
                        Multiset<String> actualDiagnosticTypes = currentActual.getDiagnosticTypeStrings();
                        Multiset<String> expectedDiagnosticTypes = currentExpected.getDiagnostics();
                        if (!actualDiagnosticTypes.equals(expectedDiagnosticTypes)) {
                            Multiset<String> expectedCopy = HashMultiset.create(expectedDiagnosticTypes);
                            expectedCopy.removeAll(actualDiagnosticTypes);
                            Multiset<String> actualCopy = HashMultiset.create(actualDiagnosticTypes);
                            actualCopy.removeAll(expectedDiagnosticTypes);
                            
                            for (String type : expectedCopy) {
                                callbacks.missingDiagnostic(type, expectedStart, expectedEnd);
                            }
                            for (String type : actualCopy) {
                                callbacks.unexpectedDiagnostic(type, actualStart, actualEnd);
                            }
                        }
                        currentExpected = safeAdvance(expectedDiagnostics);
                        currentActual = safeAdvance(actualDiagnostics);
                    }

                }
            }
            else {
                if (currentActual != null) {
                    unexpectedDiagnostics(currentActual.getDiagnostics(), callbacks);
                    currentActual = safeAdvance(actualDiagnostics);
                }
                else {
                    break;
                }
            }

//            assert expectedDiagnostics.hasNext() || actualDiagnostics.hasNext();
        }
    }

    private static void unexpectedDiagnostics(List<Diagnostic> actual, DiagnosticDiffCallbacks callbacks) {
        for (Diagnostic diagnostic : actual) {
            if (!diagnostic.getPsiFile().equals(callbacks.getFile())) continue;
            List<TextRange> textRanges = diagnostic.getTextRanges();
            for (TextRange textRange : textRanges) {
                callbacks.unexpectedDiagnostic(diagnostic.getFactory().getName(), textRange.getStartOffset(), textRange.getEndOffset());
            }
        }
    }

    private static void missingDiagnostics(DiagnosticDiffCallbacks callbacks, DiagnosedRange currentExpected) {
        for (String type : currentExpected.getDiagnostics()) {
            if (!currentExpected.getFile().equals(callbacks.getFile())) return;
            callbacks.missingDiagnostic(type, currentExpected.getStart(), currentExpected.getEnd());
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
    
    public static StringBuffer addDiagnosticMarkersToText(PsiFile psiFile, BindingContext bindingContext, List<PsiErrorElement> syntaxErrors) {
        Collection<Diagnostic> diagnostics = new ArrayList<Diagnostic>();
        diagnostics.addAll(bindingContext.getDiagnostics());
        for (PsiErrorElement syntaxError : syntaxErrors) {
            diagnostics.add(new SyntaxErrorDiagnostic(syntaxError));
        }

        return addDiagnosticMarkersToText(psiFile, diagnostics);
    }

    public static StringBuffer addDiagnosticMarkersToText(@NotNull final PsiFile psiFile, Collection<Diagnostic> diagnostics) {
        StringBuffer result = new StringBuffer();
        String text = psiFile.getText();
        diagnostics = Collections2.filter(diagnostics, new Predicate<Diagnostic>() {
            @Override
            public boolean apply(@Nullable Diagnostic diagnostic) {
                if (diagnostic == null || !psiFile.equals(diagnostic.getPsiFile())) return false;
                return true;
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
                    openDiagnosticsString(result, currentDescriptor);
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
                openDiagnosticsString(result, currentDescriptor);
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

    private static void openDiagnosticsString(StringBuffer result, DiagnosticDescriptor currentDescriptor) {
        result.append("<!");
        for (Iterator<Diagnostic> iterator = currentDescriptor.diagnostics.iterator(); iterator.hasNext(); ) {
            Diagnostic diagnostic = iterator.next();
            result.append(diagnostic.getFactory().getName());
            if (iterator.hasNext()) {
                result.append(", ");
            }
        }
        result.append("!>");
    }

    private static void closeDiagnosticString(StringBuffer result) {
        result.append("<!>");
    }

    private static class SyntaxErrorDiagnosticFactory extends AbstractDiagnosticFactory {
        private static final SyntaxErrorDiagnosticFactory instance = new SyntaxErrorDiagnosticFactory();

        @NotNull
        @Override
        public String getName() {
            return "SYNTAX";
        }
    }

    public static class SyntaxErrorDiagnostic implements Diagnostic {
        private final PsiErrorElement errorElement;

        public SyntaxErrorDiagnostic(@NotNull PsiErrorElement errorElement) {
            this.errorElement = errorElement;
        }

        @NotNull
        @Override
        public AbstractDiagnosticFactory getFactory() {
            return SyntaxErrorDiagnosticFactory.instance;
        }

        @NotNull
        @Override
        public Severity getSeverity() {
            throw new IllegalStateException();
        }

        @NotNull
        @Override
        public PsiErrorElement getPsiElement() {
            return errorElement;
        }

        @NotNull
        @Override
        public List<TextRange> getTextRanges() {
            return Collections.singletonList(errorElement.getTextRange());
        }

        @NotNull
        @Override
        public PsiFile getPsiFile() {
            return errorElement.getContainingFile();
        }
    }
    
    private static List<DiagnosticDescriptor> getSortedDiagnosticDescriptors(Collection<Diagnostic> diagnostics) {
        List<Diagnostic> list = Lists.newArrayList(diagnostics);
        Collections.sort(list, DIAGNOSTIC_COMPARATOR);

        List<DiagnosticDescriptor> diagnosticDescriptors = Lists.newArrayList();
        DiagnosticDescriptor currentDiagnosticDescriptor = null;
        for (Diagnostic diagnostic : list) {
            List<TextRange> textRanges = diagnostic.getTextRanges();
            if (textRanges.isEmpty()) continue;

            TextRange textRange = textRanges.get(0);
            if (currentDiagnosticDescriptor != null && currentDiagnosticDescriptor.equalRange(textRange)) {
                currentDiagnosticDescriptor.diagnostics.add(diagnostic);
            }
            else {
                currentDiagnosticDescriptor = new DiagnosticDescriptor(textRange.getStartOffset(), textRange.getEndOffset(), diagnostic);
                diagnosticDescriptors.add(currentDiagnosticDescriptor);
            }
        }
        return diagnosticDescriptors;
    }

    private static class DiagnosticDescriptor {
        private final int start;
        private final int end;
        private final List<Diagnostic> diagnostics = Lists.newArrayList();

        DiagnosticDescriptor(int start, int end, Diagnostic diagnostic) {
            this.start = start;
            this.end = end;
            this.diagnostics.add(diagnostic);
        }

        public boolean equalRange(TextRange textRange) {
            return start == textRange.getStartOffset() && end == textRange.getEndOffset();
        }

        public Multiset<String> getDiagnosticTypeStrings() {
            Multiset<String> actualDiagnosticTypes = HashMultiset.create();
            for (Diagnostic diagnostic : diagnostics) {
                actualDiagnosticTypes.add(diagnostic.getFactory().getName());
            }
            return actualDiagnosticTypes;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public List<Diagnostic> getDiagnostics() {
            return diagnostics;
        }

        public TextRange getTextRange() {
            return new TextRange(start, end);
        }
    }

    public static class DiagnosedRange {
        private final int start;
        private int end;
        private final Multiset<String> diagnostics = HashMultiset.create();
        private PsiFile file;

        private DiagnosedRange(int start) {
            this.start = start;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public Multiset<String> getDiagnostics() {
            return diagnostics;
        }

        public void setEnd(int end) {
            this.end = end;
        }
        
        public void addDiagnostic(String diagnostic) {
            diagnostics.add(diagnostic);
        }

        public void setFile(@NotNull PsiFile file) {
            this.file = file;
        }

        @NotNull
        public PsiFile getFile() {
            return file;
        }
    }
}
