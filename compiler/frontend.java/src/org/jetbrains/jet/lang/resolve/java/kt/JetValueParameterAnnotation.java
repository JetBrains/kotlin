package org.jetbrains.jet.lang.resolve.java.kt;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;

/**
 * @author Stepan Koltsov
 */
public class JetValueParameterAnnotation extends PsiAnnotationWrapper {
    
    public JetValueParameterAnnotation(@Nullable PsiAnnotation psiAnnotation) {
        super(psiAnnotation);
    }
    
    private String name;
    @NotNull
    public String name() {
        if (name == null) {
            name = getStringAttribute(JvmStdlibNames.JET_VALUE_PARAMETER_NAME_FIELD, "");
        }
        return name;
    }
    
    private String type;
    @NotNull
    public String type() {
        if (type == null) {
            type = getStringAttribute(JvmStdlibNames.JET_VALUE_PARAMETER_TYPE_FIELD, "");
        }
        return type;
    }

    private boolean nullable;
    private boolean nullableInitialized = false;
    public boolean nullable() {
        if (!nullableInitialized) {
            nullable = getBooleanAttribute(JvmStdlibNames.JET_VALUE_PARAMETER_NULLABLE_FIELD, false);
            nullableInitialized = true;
        }
        return nullable;
    }

    private boolean receiver;
    private boolean receiverInitialized = false;
    public boolean receiver() {
        if (!receiverInitialized) {
            receiver = getBooleanAttribute(JvmStdlibNames.JET_VALUE_PARAMETER_RECEIVER_FIELD, false);
            receiverInitialized = true;
        }
        return receiver;
    }
    
    private boolean hasDefaultValue;
    private boolean hasDefaultValueInitialized = false;
    public boolean hasDefaultValue() {
        if (!hasDefaultValueInitialized) {
            hasDefaultValue = getBooleanAttribute(JvmStdlibNames.JET_VALUE_PARAMETER_HAS_DEFAULT_VALUE_FIELD, false);
            hasDefaultValueInitialized = true;
        }
        return hasDefaultValue;
    }
    
    public static JetValueParameterAnnotation get(PsiParameter psiParameter) {
        return new JetValueParameterAnnotation(psiParameter.getModifierList().findAnnotation(JvmStdlibNames.JET_VALUE_PARAMETER.getFqName()));
    }
    
}
