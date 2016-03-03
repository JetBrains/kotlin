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

fun test() {
    // @NotNull platform type
    val platformNN = J.staticNN
    // @Nullable platform type
    val platformN = J.staticN
    // platform type with no annotation
    val platformJ = J.staticJ

    for (x in platformNN) {}
    for (x in <!ITERATOR_ON_NULLABLE!>platformN<!>) {}
    for (x in platformJ) {}
}

