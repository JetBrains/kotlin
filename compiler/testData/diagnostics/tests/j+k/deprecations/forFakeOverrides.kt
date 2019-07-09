// FILE: J.java
public interface J {
    @Deprecated
    public void foo();

    @Deprecated
    public String bar();

    @Deprecated
    public CharSequence baz();
}
// FILE: J2.java
public interface J2 extends J, K {
}

// FILE: main.kt
interface K {
    fun bar(): CharSequence
    fun baz(): String
}

interface A : J, K

fun main(j: J, j2: J2, a: A) {
    j.<!DEPRECATION!>foo<!>()
    j2.<!DEPRECATION!>foo<!>()
    a.<!DEPRECATION!>foo<!>()

    j.<!DEPRECATION!>bar<!>()
    j2.<!DEPRECATION!>bar<!>()
    a.<!DEPRECATION!>bar<!>()

    j.<!DEPRECATION!>baz<!>()
    j2.baz()
    a.baz()
}
