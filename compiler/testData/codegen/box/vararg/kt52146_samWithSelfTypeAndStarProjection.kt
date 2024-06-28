// TARGET_BACKEND: JVM
// FULL_JDK
// JVM_TARGET: 1.8
// JVM_ABI_K1_K2_DIFF: KT-64954
// FILE: J.java

import java.util.function.*;

public class J<SELF extends J<SELF, ACTUAL>, ACTUAL> {
    public final SELF satisfies(Consumer<? super ACTUAL>... c) {
        return null;
    }
}

// FILE: box.kt

import java.util.function.*

fun test(j: J<*, *>) {
    j.satisfies(Consumer { it is CharSequence })
    j.satisfies({ it is CharSequence })
}

fun box(): String {
    class K : J<K, String>()
    test(K())
    return "OK"
}
