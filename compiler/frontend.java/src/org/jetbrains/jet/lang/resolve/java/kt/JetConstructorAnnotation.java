package org.jetbrains.jet.lang.resolve.java.kt;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;

/**
 * @author Stepan Koltsov
 */
public class JetConstructorAnnotation extends PsiAnnotationWrapper {

    public JetConstructorAnnotation(@Nullable PsiAnnotation psiAnnotation) {
        super(psiAnnotation);
    }

    private boolean hidden;
    private boolean hiddenInitialized = false;
    /** @deprecated */
    public boolean hidden() {
        if (!hiddenInitialized) {
            hidden = getBooleanAttribute(JvmStdlibNames.JET_CONSTRUCTOR_HIDDEN_FIELD, false);
            hiddenInitialized = true;
        }
        return hidden;
    }
    
    public static JetConstructorAnnotation get(PsiMethod constructor) {
        return new JetConstructorAnnotation(constructor.getModifierList().findAnnotation(JvmStdlibNames.JET_CONSTRUCTOR.getFqName()));
    }
}
