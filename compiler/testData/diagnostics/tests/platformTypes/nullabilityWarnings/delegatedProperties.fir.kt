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

var A by J.staticNN
var B by <!UNSAFE_CALL!>J.staticN<!>
var C by J.staticJ