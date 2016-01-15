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
    J.staticNN()
    J.<!UNSAFE_IMPLICIT_INVOKE_CALL!>staticN<!>()
    J.staticJ()
}