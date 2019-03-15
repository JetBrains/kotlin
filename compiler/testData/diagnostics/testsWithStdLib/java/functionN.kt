// !DIAGNOSTICS: -UNUSED_ANONYMOUS_PARAMETER

// FILE: KI.kt

interface KI {
    fun manyParams(x: (String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String, String) -> Unit)
}

// FILE: A.java

import kotlin.jvm.functions.FunctionN;

public class A implements KI {
    public FunctionN<?> field;

    public A(FunctionN<?> w) {}

    public void foo(FunctionN<String> x) {

    }

    public FunctionN<?> bar() {
        return null;
    }

    public void baz(java.util.List<FunctionN<?>> z) {

    }

    public void manyParams(FunctionN<Unit> x) {

    }
}

// FILE: main.kt

fun <T> any(): T = null!!

fun main() {
    val a: A = <!DEPRECATION_ERROR!>A<!>(null)

    a.<!DEPRECATION_ERROR!>field<!>.hashCode();
    a.<!DEPRECATION_ERROR!>field<!> = null;

    a.<!DEPRECATION_ERROR!>foo<!>(null)
    a.<!DEPRECATION_ERROR!>bar<!>()
    a.<!DEPRECATION_ERROR!>baz<!>(listOf())

    a.<!DEPRECATION_ERROR!>manyParams<!>(null)
    a.<!NONE_APPLICABLE!>manyParams<!>(any<kotlin.jvm.functions.FunctionN<Unit>>())

    // Potentially, this would have better to forbid calling manyParams, too.
    // But it might be complicated because we need to match that it is an override
    // Seems to be fine because `A::manyParams` is anyway an override in JVM and can be called with (a as K)
    a.manyParams {
            x1, x2, x3, x4, x5, x6, x7, x8, x9, x10, x11, x12, x13, x14, x15, x16, x17, x18, x19, x20, x21, x22, x23, x24, x25, x26, x27, x28, x29, x30, x31, x32 ->
    }
}
