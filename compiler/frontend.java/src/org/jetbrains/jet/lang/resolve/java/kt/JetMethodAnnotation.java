package org.jetbrains.jet.lang.resolve.java.kt;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;

/**
 * @author Stepan Koltsov
 */
public class JetMethodAnnotation extends JetMethodOrPropertyAnnotation {

    public JetMethodAnnotation(@Nullable PsiAnnotation psiAnnotation) {
        super(psiAnnotation);
    }

    private String returnType;
    @NotNull
    public String returnType() {
        if (returnType == null) {
            returnType = getStringAttribute(JvmStdlibNames.JET_METHOD_RETURN_TYPE_FIELD, "");
        }
        return returnType;
    }

    private boolean returnTypeNullable;
    private boolean returnTypeNullableInitialized;
    @NotNull
    public boolean returnTypeNullable() {
        if (!returnTypeNullableInitialized) {
            returnTypeNullable = getBooleanAttribute(JvmStdlibNames.JET_METHOD_NULLABLE_RETURN_TYPE_FIELD, false);
            returnTypeNullableInitialized = true;
        }
        return returnTypeNullable;
    }
    
    public static JetMethodAnnotation get(PsiMethod psiMethod) {
        return new JetMethodAnnotation(psiMethod.getModifierList().findAnnotation(JvmStdlibNames.JET_METHOD.getFqName()));
    }
}
