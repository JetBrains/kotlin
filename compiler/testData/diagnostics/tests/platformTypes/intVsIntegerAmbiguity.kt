// !CHECK_TYPE
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
    checkSubtype<J.A>(foo(2))
    checkSubtype<J.A>(foo(i))
    checkSubtype<J.B>(J.foo(ni))
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(ni)

    foo(J.getInteger())
    J.foo(J.getInteger())
}

