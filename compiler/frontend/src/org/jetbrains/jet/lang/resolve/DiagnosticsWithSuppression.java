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

package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.ImmutableSet;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ConcurrentWeakValueHashMap;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.FilteringIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.diagnostics.Severity;
import org.jetbrains.jet.lang.psi.JetAnnotated;
import org.jetbrains.jet.lang.psi.JetAnnotationEntry;
import org.jetbrains.jet.lang.psi.JetStubbedPsiUtil;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.codeFragmentUtil.CodeFragmentUtilPackage;
import org.jetbrains.jet.lang.resolve.constants.ArrayValue;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.constants.StringValue;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class DiagnosticsWithSuppression implements Diagnostics {

    private static final Logger LOG = Logger.getInstance(DiagnosticsWithSuppression.class);

    private final BindingContext context;
    private final Collection<Diagnostic> diagnostics;

    // The cache is weak: we're OK with losing it
    private final Map<JetAnnotated, Suppressor> suppressors = new ConcurrentWeakValueHashMap<JetAnnotated, Suppressor>();

    private final Condition<Diagnostic> filter = new Condition<Diagnostic>() {
        @Override
        public boolean value(Diagnostic diagnostic) {
            return !isSuppressed(diagnostic);
        }
    };
    private final DiagnosticsElementsCache elementsCache = new DiagnosticsElementsCache(this);

    public DiagnosticsWithSuppression(@NotNull BindingContext context, @NotNull Collection<Diagnostic> diagnostics) {
        this.context = context;
        this.diagnostics = diagnostics;
    }

    @NotNull
    @Override
    public Diagnostics noSuppression() {
        return new SimpleDiagnostics(diagnostics);
    }

    @NotNull
    @Override
    public Iterator<Diagnostic> iterator() {
        return new FilteringIterator<Diagnostic, Diagnostic>(diagnostics.iterator(), filter);
    }

    @NotNull
    @Override
    public Collection<Diagnostic> all() {
        return ContainerUtil.filter(diagnostics, filter);
    }

    @NotNull
    @Override
    public Collection<Diagnostic> forElement(@NotNull PsiElement psiElement) {
        return elementsCache.getDiagnostics(psiElement);
    }

    @Override
    public boolean isEmpty() {
        return all().isEmpty();
    }

    private boolean isSuppressed(@NotNull Diagnostic diagnostic) {
        PsiElement element = diagnostic.getPsiElement();

        if (isSuppressedForDebugger(diagnostic, element)) return true;

        JetAnnotated annotated = JetStubbedPsiUtil.getPsiOrStubParent(element, JetAnnotated.class, false);
        if (annotated == null) return false;

        return isSuppressedByAnnotated(diagnostic, annotated, 0);
    }

    private static boolean isSuppressedForDebugger(@NotNull Diagnostic diagnostic, @NotNull PsiElement element) {
        PsiFile containingFile = element.getContainingFile();
        if (containingFile instanceof JetFile && CodeFragmentUtilPackage.getSkipVisibilityCheck((JetFile) containingFile)) {
            DiagnosticFactory<?> diagnosticFactory = diagnostic.getFactory();
            return diagnosticFactory == Errors.INVISIBLE_MEMBER ||
                   diagnosticFactory == Errors.INVISIBLE_REFERENCE ||
                   diagnosticFactory == Errors.INVISIBLE_SETTER;
        }
        return false;
    }

    /*
       The cache is optimized for the case where no warnings are suppressed (most frequent one)

       trait Root {
         suppress("X")
         trait A {
           trait B {
             suppress("Y")
             trait C {
               fun foo() = warning
             }
           }
         }
       }

       Nothing is suppressed at foo, so we look above. While looking above we went up to the root (once) and propagated
       all the suppressors down, so now we have:

          foo  - suppress(Y) from C
          C    - suppress(Y) from C
          B    - suppress(X) from A
          A    - suppress(X) from A
          Root - suppress() from Root

       Next time we look up anything under foo, we try the Y-suppressor and then immediately the X-suppressor, then to the empty
       suppressor at the root. All the intermediate empty nodes are skipped, because every suppressor remembers its definition point.

       This way we need no more lookups than the number of suppress() annotations from here to the root.
     */
    private boolean isSuppressedByAnnotated(@NotNull Diagnostic diagnostic, @NotNull JetAnnotated annotated, int debugDepth) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Annotated: ", annotated.getName());
            LOG.debug("Depth: ", debugDepth);
            LOG.debug("Cache size: ", suppressors.size(), "\n");
        }

        Suppressor suppressor = getOrCreateSuppressor(annotated);
        if (suppressor.isSuppressed(diagnostic)) return true;

        JetAnnotated annotatedAbove = JetStubbedPsiUtil.getPsiOrStubParent(suppressor.getAnnotatedElement(), JetAnnotated.class, true);
        if (annotatedAbove == null) return false;

        boolean suppressed = isSuppressedByAnnotated(diagnostic, annotatedAbove, debugDepth + 1);
        Suppressor suppressorAbove = suppressors.get(annotatedAbove);
        if (suppressorAbove != null && suppressorAbove.dominates(suppressor)) {
            suppressors.put(annotated, suppressorAbove);
        }

        return suppressed;
    }

    @NotNull
    private Suppressor getOrCreateSuppressor(@NotNull JetAnnotated annotated) {
        Suppressor suppressor = suppressors.get(annotated);
        if (suppressor == null) {
            Set<String> strings = getSuppressingStrings(annotated);
            if (strings.isEmpty()) {
                suppressor = new EmptySuppressor(annotated);
            }
            else if (strings.size() == 1) {
                suppressor = new SingularSuppressor(annotated, strings.iterator().next());
            }
            else {
                suppressor = new MultiSuppressor(annotated, strings);
            }
            suppressors.put(annotated, suppressor);
        }
        return suppressor;
    }

    private Set<String> getSuppressingStrings(@NotNull JetAnnotated annotated) {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for (JetAnnotationEntry annotationEntry : annotated.getAnnotationEntries()) {
            AnnotationDescriptor annotationDescriptor = context.get(BindingContext.ANNOTATION, annotationEntry);
            if (annotationDescriptor == null) continue;

            if (!KotlinBuiltIns.getInstance().isSuppressAnnotation(annotationDescriptor)) continue;

            // We only add strings and skip other values to facilitate recovery in presence of erroneous code
            for (CompileTimeConstant<?> arrayValue : annotationDescriptor.getAllValueArguments().values()) {
                if ((arrayValue instanceof ArrayValue)) {
                    for (CompileTimeConstant<?> value : ((ArrayValue) arrayValue).getValue()) {
                        if (value instanceof StringValue) {
                            builder.add(String.valueOf(((StringValue) value).getValue()).toLowerCase());
                        }
                    }
                }
            }
        }
        return builder.build();
    }

    public static boolean isSuppressedByStrings(@NotNull Diagnostic diagnostic, @NotNull Set<String> strings) {
        if (strings.contains("warnings") && diagnostic.getSeverity() == Severity.WARNING) return true;

        return strings.contains(diagnostic.getFactory().getName().toLowerCase());
    }

    @NotNull
    @Override
    public ModificationTracker getModificationTracker() {
        throw new IllegalStateException("Trying to obtain modification tracker for readonly DiagnosticsWithSuppression.");
    }

    private static abstract class Suppressor {
        private final JetAnnotated annotated;

        protected Suppressor(@NotNull JetAnnotated annotated) {
            this.annotated = annotated;
        }

        @NotNull
        public JetAnnotated getAnnotatedElement() {
            return annotated;
        }

        public abstract boolean isSuppressed(@NotNull Diagnostic diagnostic);

        // true is \forall x. other.isSuppressed(x) -> this.isSuppressed(x)
        public abstract boolean dominates(@NotNull Suppressor other);
    }

    private static class EmptySuppressor extends Suppressor {

        private EmptySuppressor(@NotNull JetAnnotated annotated) {
            super(annotated);
        }

        @Override
        public boolean isSuppressed(@NotNull Diagnostic diagnostic) {
            return false;
        }

        @Override
        public boolean dominates(@NotNull Suppressor other) {
            return other instanceof EmptySuppressor;
        }
    }

    private static class SingularSuppressor extends Suppressor {
        private final String string;

        private SingularSuppressor(@NotNull JetAnnotated annotated, @NotNull String string) {
            super(annotated);
            this.string = string;
        }

        @Override
        public boolean isSuppressed(@NotNull Diagnostic diagnostic) {
            return isSuppressedByStrings(diagnostic, ImmutableSet.of(string));
        }

        @Override
        public boolean dominates(@NotNull Suppressor other) {
            return other instanceof EmptySuppressor
                   || (other instanceof SingularSuppressor && ((SingularSuppressor) other).string.equals(string));
        }
    }

    private static class MultiSuppressor extends Suppressor {
        private final Set<String> strings;

        private MultiSuppressor(@NotNull JetAnnotated annotated, @NotNull Set<String> strings) {
            super(annotated);
            this.strings = strings;
        }

        @Override
        public boolean isSuppressed(@NotNull Diagnostic diagnostic) {
            return isSuppressedByStrings(diagnostic, strings);
        }

        @Override
        public boolean dominates(@NotNull Suppressor other) {
            // it's too costly to check set inclusion
            return other instanceof EmptySuppressor;
        }
    }
}
