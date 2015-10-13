// FILE: A.java
import org.jetbrains.annotations.*;

public interface A {
    void foo(@Nullable String x);
}

// FILE: B.java
import org.jetbrains.annotations.*;

public interface B {
    void foo(@NotNull String x);
}

// FILE: C.kt

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C1<!> : A, B {
    <!ACCIDENTAL_OVERRIDE!>override fun foo(x: String)<!> {}
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C2<!> : A, B {
    <!ACCIDENTAL_OVERRIDE!>override fun foo(x: String?)<!> {}
}

interface <!CONFLICTING_INHERITED_JVM_DECLARATIONS!>I<!> : A, B

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C3<!> : I {
    <!ACCIDENTAL_OVERRIDE!>override fun foo(x: String)<!> {}
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C4<!> : I {
    <!ACCIDENTAL_OVERRIDE!>override fun foo(x: String?)<!> {}
}