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
    checkSubtype<C>(foo(2))
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><J.A>(J.<!AMBIGUITY!>foo<!>(2))
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><J.A>(J.<!AMBIGUITY!>foo<!>(i))
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><J.B>(J.<!AMBIGUITY!>foo<!>(ni))
    checkSubtype<C>(foo(ni))
    <!INAPPLICABLE_CANDIDATE!>checkSubtype<!><J.B>(J.<!AMBIGUITY!>foo<!>(ni))

    foo(J.getInteger())
    J.<!AMBIGUITY!>foo<!>(J.getInteger())
}

