// JSR305_GLOBAL_REPORT: warn
// FULL_JDK
// FILE: J.java
import java.util.*;

public class J {
    @MyNonnull
    public static List<String> staticNN;
    @MyNullable
    public static List<String> staticN;
    public static List<String> staticJ;
}

// FILE: k.kt
<!NO_OVERRIDE_FOR_DELEGATE_WITH_DEFAULT_METHOD!>class A<!> : List<String> by J.staticNN
<!NO_OVERRIDE_FOR_DELEGATE_WITH_DEFAULT_METHOD!>class B<!> : List<String> by J.staticN
<!NO_OVERRIDE_FOR_DELEGATE_WITH_DEFAULT_METHOD!>class C<!> : List<String> by J.staticJ
