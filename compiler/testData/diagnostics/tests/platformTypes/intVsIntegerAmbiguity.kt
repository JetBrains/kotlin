// !DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: p/J.java

package p;

public class J {
    public interface A {}
    public static A foo(int s);

    public interface B {}
    public static B foo(Integer s);

    public static Integer getInteger();
}

// FILE: k.kt

import p.*
import p.J.*

class C

fun foo(i: Int?) : C = null!!

fun test(i: Int, ni: Int?) {
    foo(2) : J.A
    foo(i) : J.A
    J.foo(ni) : J.B
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(ni)

    foo(J.getInteger())
    J.foo(J.getInteger())
}

