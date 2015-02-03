// FILE: p/J.java
package p;

import org.jetbrains.annotations.*;

public class J {

    public interface DP {
        String get(Object a, Object b);
        String set(Object a, Object b, Object c);
    }

    @NotNull
    public static DP staticNN;
    @Nullable
    public static DP staticN;
    public static DP staticJ;
}

// FILE: k.kt

import p.*

var A by J.staticNN
var B by J.staticN
var C by J.staticJ