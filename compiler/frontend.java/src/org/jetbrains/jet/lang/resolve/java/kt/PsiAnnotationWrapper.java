package org.jetbrains.jet.lang.resolve.java.kt;

import com.intellij.psi.PsiAnnotation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Stepan Koltsov
 */
public abstract class PsiAnnotationWrapper {

    @Nullable
    private PsiAnnotation psiAnnotation;

    protected PsiAnnotationWrapper(@Nullable PsiAnnotation psiAnnotation) {
        this.psiAnnotation = psiAnnotation;
    }

    @Nullable
    public PsiAnnotation getPsiAnnotation() {
        return psiAnnotation;
    }

    public boolean isDefined() {
        return psiAnnotation != null;
    }
    
    @NotNull
    protected String getStringAttribute(String name, String defaultValue) {
        return PsiAnnotationUtils.getStringAttribute(psiAnnotation, name, defaultValue);
    }
    
    protected boolean getBooleanAttribute(String name, boolean defaultValue) {
        return PsiAnnotationUtils.getBooleanAttribute(psiAnnotation, name, defaultValue);
    }

    protected int getIntAttribute(String name, int defaultValue) {
        return PsiAnnotationUtils.getIntAttribute(psiAnnotation, name, defaultValue);
    }

}
