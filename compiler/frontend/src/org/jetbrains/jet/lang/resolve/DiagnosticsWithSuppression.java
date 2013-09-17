package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.ImmutableSet;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.Severity;
import org.jetbrains.jet.lang.psi.JetAnnotationEntry;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetModifierList;
import org.jetbrains.jet.lang.resolve.constants.*;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.*;

public class DiagnosticsWithSuppression implements Diagnostics {
    private final BindingContext context;
    private final Collection<Diagnostic> diagnostics;

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
        return all().iterator();
    }

    @NotNull
    @Override
    public Collection<Diagnostic> all() {
        return ContainerUtil.filter(diagnostics, new Condition<Diagnostic>() {
            @Override
            public boolean value(Diagnostic diagnostic) {
                return !isSuppressed(diagnostic);
            }
        });
    }

    @Override
    public boolean isEmpty() {
        return all().isEmpty();
    }

    private boolean isSuppressed(@NotNull Diagnostic diagnostic) {
        PsiElement element = diagnostic.getPsiElement();
        if (element instanceof JetDeclaration && isSuppressedByDeclaration(diagnostic, (JetDeclaration) element)) return true;

        while (true) {
            JetDeclaration declaration = PsiTreeUtil.getParentOfType(element, JetDeclaration.class, true);
            if (declaration == null) return false;

            if (isSuppressedByDeclaration(diagnostic, declaration)) return true;

            if (element == declaration) {
                element = declaration.getParent();
            }
            else {
                element = declaration;
            }
        }
    }

    private boolean isSuppressedByDeclaration(@NotNull Diagnostic diagnostic, @NotNull JetDeclaration declaration) {
        JetModifierList modifierList = declaration.getModifierList();
        if (modifierList == null) return false;

        ClassDescriptor suppress = KotlinBuiltIns.getInstance().getSuppressAnnotationClass();
        ConstructorDescriptor primaryConstructor = suppress.getUnsubstitutedPrimaryConstructor();
        assert primaryConstructor != null : "No primary constructor in " + suppress;
        ValueParameterDescriptor parameter = primaryConstructor.getValueParameters().get(0);

        for (JetAnnotationEntry annotationEntry : modifierList.getAnnotationEntries()) {
            AnnotationDescriptor annotationDescriptor = context.get(BindingContext.ANNOTATION, annotationEntry);
            if (annotationDescriptor == null) {
                continue;
            }

            if (!suppress.equals(annotationDescriptor.getType().getConstructor().getDeclarationDescriptor())) continue;

            Map<ValueParameterDescriptor, CompileTimeConstant<?>> arguments = annotationDescriptor.getAllValueArguments();
            CompileTimeConstant<?> value = arguments.get(parameter);
            if (value instanceof ArrayValue) {
                ArrayValue arrayValue = (ArrayValue) value;
                List<CompileTimeConstant<?>> values = arrayValue.getValue();

                if (isSuppressedByStrings(diagnostic, strings(values))) {
                    return true;
                }
            }

        }
        return false;
    }

    public static boolean isSuppressedByStrings(@NotNull Diagnostic diagnostic, @NotNull Set<String> strings) {
        if (strings.contains("warnings") && diagnostic.getSeverity() == Severity.WARNING) return true;

        return strings.contains(diagnostic.getFactory().getName().toLowerCase());
    }

    // We only add strings and skip other values to facilitate recovery in presence of erroneous code
    private static Set<String> strings(List<CompileTimeConstant<?>> values) {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for (CompileTimeConstant<?> value : values) {
            if (value instanceof StringValue) {
                StringValue stringValue = (StringValue) value;
                builder.add(stringValue.getValue().toLowerCase());
            }
        }
        return builder.build();
    }
}
