// FILE: j/J.java

package j;

public class J {
    public static final String ok() { return "OK"; }
}

// FILE: k.kt
import j.J

fun foo(a: Any) {}

fun test() {
    val a = J.ok()
    a!!
    foo(a)
    if (a == null) foo("NULL-1")
}

// @KKt.class:
// 0 IFNULL
// 1 checkNotNull \(Ljava/lang/Object;\)V
// 0 NULL-1