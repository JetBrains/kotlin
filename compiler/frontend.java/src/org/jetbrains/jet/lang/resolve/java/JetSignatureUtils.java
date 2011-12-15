package org.jetbrains.jet.lang.resolve.java;

import jet.typeinfo.TypeInfoVariance;
import org.jetbrains.jet.lang.types.Variance;

/**
 * @author Stepan Koltsov
 */
public class JetSignatureUtils {
    
    static {
        if (Variance.INVARIANT.ordinal() != TypeInfoVariance.INVARIANT.ordinal())
            throw new IllegalStateException();
        if (Variance.IN_VARIANCE.ordinal() != TypeInfoVariance.IN.ordinal())
            throw new IllegalStateException();
        if (Variance.OUT_VARIANCE.ordinal() != TypeInfoVariance.OUT.ordinal())
            throw new IllegalStateException();
    }

    public static TypeInfoVariance translateVariance(Variance variance) {
        return TypeInfoVariance.values()[variance.ordinal()];
    }

    public static Variance translateVariance(TypeInfoVariance variance) {
        return Variance.values()[variance.ordinal()];
    }

}
