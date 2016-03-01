// FILE: Foo.java

import java.util.*;

public class Foo {
    public static <T extends Object & Comparable<? super T>> T max(Collection<? extends T> coll) {
        return Collections.max(coll);
    }
}

// FILE: 1.kt

fun box(): String {
    return Foo.max(java.util.Arrays.asList("AK", "OK", "EK"))!!
}
