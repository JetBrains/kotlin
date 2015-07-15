// FILE: A.java
import org.jetbrains.annotations.*;

interface A {
    void foo(@Nullable String x);
}

// FILE: B.java
import org.jetbrains.annotations.*;

interface B {
    void foo(@NotNull String x);
}

// FILE: C.kt

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C1<!> : A, B {
    <!ACCIDENTAL_OVERRIDE!>override fun foo(x: String)<!> {}
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C2<!> : A, B {
    <!ACCIDENTAL_OVERRIDE!>override fun foo(x: String?)<!> {}
}

interface <!ACCIDENTAL_OVERRIDE!>I<!> : A, B

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C3<!> : I {
    <!ACCIDENTAL_OVERRIDE!>override fun foo(x: String)<!> {}
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C4<!> : I {
    <!ACCIDENTAL_OVERRIDE!>override fun foo(x: String?)<!> {}
}
