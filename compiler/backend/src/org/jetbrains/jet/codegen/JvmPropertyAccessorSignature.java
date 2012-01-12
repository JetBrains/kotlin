package org.jetbrains.jet.codegen;

import org.jetbrains.annotations.NotNull;

/**
 * @author Stepan Koltsov
 */
public class JvmPropertyAccessorSignature {
    @NotNull
    private final JvmMethodSignature jvmMethodSignature;
    @NotNull
    private final String propertyTypeKotlinSignature;

    public JvmPropertyAccessorSignature(@NotNull JvmMethodSignature jvmMethodSignature, @NotNull String propertyTypeKotlinSignature) {
        this.jvmMethodSignature = jvmMethodSignature;
        this.propertyTypeKotlinSignature = propertyTypeKotlinSignature;
    }

    @NotNull
    public JvmMethodSignature getJvmMethodSignature() {
        return jvmMethodSignature;
    }

    @NotNull
    public String getPropertyTypeKotlinSignature() {
        return propertyTypeKotlinSignature;
    }
}
