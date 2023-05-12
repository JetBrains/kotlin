// FIR_IDENTICAL
// FILE: J.java

import org.jetbrains.annotations.*;
import java.util.*;

public class J {
    @NotNull
    public static List<String> staticNN;
    @Nullable
    public static List<String> staticN;
    public static List<String> staticJ;
}

// FILE: k.kt

class A : List<String> by J.staticNN
class B : List<String> by <!TYPE_MISMATCH!>J.staticN<!>
class C : List<String> by J.staticJ