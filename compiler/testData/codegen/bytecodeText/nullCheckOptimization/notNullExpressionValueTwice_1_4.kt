// !API_VERSION: LATEST
// FILE: j/J.java

package j;

public class J {
    public static final String ok() { return "OK"; }
}

// FILE: foo.kt
fun foo(a: Any) {}

// FILE: k.kt
import j.J

fun test() {
    val a = J.ok()
    foo(a)
    foo(a)
}

// @KKt.class:
// 1 checkNotNull
