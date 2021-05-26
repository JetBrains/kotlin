// FIR_IDENTICAL
// FILE: J.java

import org.jetbrains.annotations.*;

public class J {

    public interface DP {
        String getValue(Object a, Object b);
        String setValue(Object a, Object b, Object c);
    }

    @NotNull
    public static DP staticNN;
    @Nullable
    public static DP staticN;
    public static DP staticJ;
}

// FILE: k.kt

var A by <!NEW_INFERENCE_ERROR, NEW_INFERENCE_ERROR!>J.staticNN<!>
var B by <!DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE, DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE!>J.staticN<!>
var C by <!NEW_INFERENCE_ERROR, NEW_INFERENCE_ERROR!>J.staticJ<!>
