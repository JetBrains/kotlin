package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.Annotation;
import org.jetbrains.jet.lang.psi.JetAnnotationEntry;
import org.jetbrains.jet.lang.psi.JetModifierList;

import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class AnnotationResolver {
    public static final AnnotationResolver INSTANCE = new AnnotationResolver();

    private AnnotationResolver() {}

    @NotNull
    public List<Annotation> resolveAnnotations(@NotNull List<JetAnnotationEntry> annotationEntryElements) {
        return Collections.emptyList(); // TODO
//        if (annotationEntryElements.isEmpty()) {
//        }
//        throw new UnsupportedOperationException(); // TODO
    }

    @NotNull
    public List<Annotation> resolveAnnotations(@Nullable JetModifierList modifierList) {
        if (modifierList == null) {
            return Collections.emptyList();
        }
        return resolveAnnotations(modifierList.getAttributes());
    }
}
