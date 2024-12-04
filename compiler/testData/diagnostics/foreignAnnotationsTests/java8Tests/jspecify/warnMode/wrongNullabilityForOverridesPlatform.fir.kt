// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt
expect class A() {}

// MODULE: jvm()()(common)
// FILE: J.java
import org.jspecify.annotations.*;

public class J {
    @NonNull
    public String foo() { return ""; }

    @NonNull
    public String bar() { return ""; }

    public void baz(@NonNull String s) {}
}

// FILE: jvm.kt
actual class A : J() {
    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun foo() = null
    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun bar(): String? = ""
    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE!>override<!> fun baz(s: String?) {}
}
