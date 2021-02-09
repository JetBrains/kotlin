// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: Foo.java

import java.util.*;

public class Foo {
    public static <T extends Object & Comparable<? super T>> T max(Collection<? extends T> coll) {
        return Collections.max(coll);
    }
}

// MODULE: main(lib)
// FILE: 1.kt

fun box(): String {
    return Foo.max(java.util.Arrays.asList("AK", "OK", "EK"))!!
}
