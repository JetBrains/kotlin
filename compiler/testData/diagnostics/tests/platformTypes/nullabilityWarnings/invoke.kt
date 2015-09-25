// FILE: J.java

import org.jetbrains.annotations.*;

public class J {
    public interface Invoke {
        void invoke();
    }

    @NotNull
    public static Invoke staticNN;
    @Nullable
    public static Invoke staticN;
    public static Invoke staticJ;
}

// FILE: k.kt

fun test() {
    J.<!OPERATOR_MODIFIER_REQUIRED!>staticNN<!>()
    J.<!UNSAFE_CALL, OPERATOR_MODIFIER_REQUIRED!>staticN<!>()
    J.<!OPERATOR_MODIFIER_REQUIRED!>staticJ<!>()
}