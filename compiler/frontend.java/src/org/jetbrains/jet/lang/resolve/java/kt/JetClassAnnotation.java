package org.jetbrains.jet.lang.resolve.java.kt;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;

/**
 * @author Stepan Koltsov
 */
public class JetClassAnnotation extends PsiAnnotationWrapper {

    public JetClassAnnotation(@Nullable PsiAnnotation psiAnnotation) {
        super(psiAnnotation);
    }
    
    private String signature;
    public String signature() {
        if (signature == null) {
            signature = getStringAttribute(JvmStdlibNames.JET_CLASS_SIGNATURE, "");
        }
        return signature;
    }
    
    @NotNull
    public static JetClassAnnotation get(PsiClass psiClass) {
        return new JetClassAnnotation(psiClass.getModifierList().findAnnotation(JvmStdlibNames.JET_CLASS.getFqName()));
    }
}
