package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.JetAnnotationEntry;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetModifierList;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class AnnotationResolver {

    private final BindingTrace trace;

    public AnnotationResolver(JetSemanticServices semanticServices, BindingTrace trace) {
        this.trace = trace;
    }

    @NotNull
    public List<AnnotationDescriptor> resolveAnnotations(@NotNull JetScope scope, @NotNull List<JetAnnotationEntry> annotationEntryElements) {
        if (annotationEntryElements.isEmpty()) return Collections.emptyList();
        List<AnnotationDescriptor> result = Lists.newArrayList();
//        for (JetAnnotationEntry entryElement : annotationEntryElements) {
//            JetType jetType = typeInferrer.checkTypeInitializerCall(scope, entryElement.getTypeReference(), entryElement);
//            AnnotationDescriptor descriptor = new AnnotationDescriptor();
//            descriptor.setAnnotationType(jetType);
//            result.add(descriptor);
//        }
        return result;
    }

    @NotNull
    public List<AnnotationDescriptor> resolveAnnotations(@NotNull JetScope scope, @Nullable JetModifierList modifierList) {
        if (modifierList == null) {
            return Collections.emptyList();
        }
        return resolveAnnotations(scope, modifierList.getAnnotationEntries());
    }

    public CompileTimeConstant<?> resolveAnnotationArgument(@NotNull JetExpression expression, @NotNull JetType expectedType) {
        final CompileTimeConstant<?>[] result = new CompileTimeConstant<?>[1];
//        expression.accept(new JetVisitorVoid() {
//            @Override
//            public void visitConstantExpression(JetConstantExpression expression) {
//                JetType type = typeInferrer.getType(JetScope.EMPTY, expression, false, expectedType);
//                if (type != null) {
//                    Object value = expression.getValue();
//
//                }
//            }
//
//            @Override
//            public void visitAnnotation(JetAnnotation annotation) {
//                super.visitAnnotation(annotation); // TODO
//            }
//
//            @Override
//            public void visitAnnotationEntry(JetAnnotationEntry annotationEntry) {
//                super.visitAnnotationEntry(annotationEntry); // TODO
//            }
//
//            @Override
//            public void visitParenthesizedExpression(JetParenthesizedExpression expression) {
//                expression.getExpression().accept(this);
//            }
//
//            @Override
//            public void visitJetElement(JetElement element) {
//                trace.getErrorHandler().genericError(element.getNode(), "Not allowed as an annotation argument");
//            }
//        });
        return result[0];
    }

    @NotNull
    public List<AnnotationDescriptor> createAnnotationStubs(List<JetAnnotationEntry> annotations) {
        List<AnnotationDescriptor> result = Lists.newArrayList();
        for (JetAnnotationEntry annotation : annotations) {
            AnnotationDescriptor annotationDescriptor = new AnnotationDescriptor();
            result.add(annotationDescriptor);
            trace.recordAnnotationResolution(annotation, annotationDescriptor);
        }
        return result;
    }

    @NotNull
    public List<AnnotationDescriptor> createAnnotationStubs(@Nullable JetModifierList modifierList) {
        if (modifierList == null) {
            return Collections.emptyList();
        }
        return createAnnotationStubs(modifierList.getAnnotationEntries());
    }
}