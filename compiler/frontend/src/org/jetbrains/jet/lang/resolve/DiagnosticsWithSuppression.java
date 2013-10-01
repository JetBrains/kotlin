package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.ImmutableSet;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ConcurrentWeakValueHashMap;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.FilteringIterator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.Severity;
import org.jetbrains.jet.lang.psi.JetAnnotated;
import org.jetbrains.jet.lang.psi.JetAnnotationEntry;
import org.jetbrains.jet.lang.resolve.constants.ArrayValue;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.constants.StringValue;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.*;

public class DiagnosticsWithSuppression implements Diagnostics {

    private static final Logger LOG = Logger.getInstance(DiagnosticsWithSuppression.class);

    private final BindingContext context;
    private final Collection<Diagnostic> diagnostics;

    // The cache is weak: we're OK with losing it
    private final Map<JetAnnotated, Suppressor> suppressors = new ConcurrentWeakValueHashMap<JetAnnotated, Suppressor>();

    // Caching frequently used values:
    private final ClassDescriptor suppressClass;
    private final ValueParameterDescriptor suppressParameter;
    private final Condition<Diagnostic> filter = new Condition<Diagnostic>() {
        @Override
        public boolean value(Diagnostic diagnostic) {
            return !isSuppressed(diagnostic);
        }
    };

    public DiagnosticsWithSuppression(@NotNull BindingContext context, @NotNull Collection<Diagnostic> diagnostics) {
        this.context = context;
        this.diagnostics = diagnostics;

        this.suppressClass = KotlinBuiltIns.getInstance().getSuppressAnnotationClass();
        ConstructorDescriptor primaryConstructor = suppressClass.getUnsubstitutedPrimaryConstructor();
        assert primaryConstructor != null : "No primary constructor in " + suppressClass;
        this.suppressParameter = primaryConstructor.getValueParameters().get(0);
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

    @Override
    public boolean isEmpty() {
        return all().isEmpty();
    }

    private boolean isSuppressed(@NotNull Diagnostic diagnostic) {
        PsiElement element = diagnostic.getPsiElement();

        JetAnnotated annotated = PsiTreeUtil.getParentOfType(element, JetAnnotated.class, false);
        if (annotated == null) return false;

        return isSuppressedByAnnotated(diagnostic, annotated, 0);
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

        JetAnnotated annotatedAbove = PsiTreeUtil.getParentOfType(suppressor.getAnnotatedElement(), JetAnnotated.class, true);
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
            if (annotationDescriptor == null) {
                continue;
            }

            if (!suppressClass.equals(annotationDescriptor.getType().getConstructor().getDeclarationDescriptor())) continue;

            Map<ValueParameterDescriptor, CompileTimeConstant<?>> arguments = annotationDescriptor.getAllValueArguments();
            CompileTimeConstant<?> value = arguments.get(suppressParameter);
            if (value instanceof ArrayValue) {
                ArrayValue arrayValue = (ArrayValue) value;
                List<CompileTimeConstant<?>> values = arrayValue.getValue();

                addStrings(builder, values);
            }

        }
        return builder.build();
    }

    public static boolean isSuppressedByStrings(@NotNull Diagnostic diagnostic, @NotNull Set<String> strings) {
        if (strings.contains("warnings") && diagnostic.getSeverity() == Severity.WARNING) return true;

        return strings.contains(diagnostic.getFactory().getName().toLowerCase());
    }

    // We only add strings and skip other values to facilitate recovery in presence of erroneous code
    private static void addStrings(ImmutableSet.Builder<String> builder, List<CompileTimeConstant<?>> values) {
        for (CompileTimeConstant<?> value : values) {
            if (value instanceof StringValue) {
                StringValue stringValue = (StringValue) value;
                builder.add(stringValue.getValue().toLowerCase());
            }
        }
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
