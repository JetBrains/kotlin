package org.jetbrains.jet.lang.resolve.java.kt;

import com.intellij.psi.PsiAnnotation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;

/**
 * @author Stepan Koltsov
 */
public abstract class JetMethodOrPropertyAnnotation extends PsiAnnotationWrapper {
    protected JetMethodOrPropertyAnnotation(@Nullable PsiAnnotation psiAnnotation) {
        super(psiAnnotation);
    }

    private String typeParameters;
    @NotNull
    public String typeParameters() {
        if (typeParameters == null) {
            typeParameters = getStringAttribute(JvmStdlibNames.JET_METHOD_TYPE_PARAMETERS_FIELD, "");
        }
        return typeParameters;
    }

}
