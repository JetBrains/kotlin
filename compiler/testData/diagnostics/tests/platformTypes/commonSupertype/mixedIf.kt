// !DIAGNOSTICS: -UNUSED_VARIABLE
// !CHECK_TYPE
// FILE: p/J.java

package p;

public class J<G> {
    public static J<String> j() { return null; }
}

// FILE: k.kt

import p.*

fun foo(j: J<String>) {
    val v = if (true) j else J.j()
    val js: J<String> = v
    // TODO: fix with dominance
//    v checkType { it : _<J<String>>}
//    v checkType { it : _<J<String?>>}
}