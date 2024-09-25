// TARGET_BACKEND: JVM
// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND_K1: JVM_IR
// REASON: K1 bug: @[FlexibleNullability] annotation is missing on IrVarargImpl.varargElementType
// FULL_JDK
// JVM_TARGET: 1.8
// Directive "JVM_ABI_K1_K2_DIFF: KT-64954" should be here, however due to "// IGNORE_BACKEND_K1: JVM_IR", the following happens: "No K1/K2 JVM ABI comparison done for this test. Please remove JVM_ABI_K1_K2_DIFF directive."
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
