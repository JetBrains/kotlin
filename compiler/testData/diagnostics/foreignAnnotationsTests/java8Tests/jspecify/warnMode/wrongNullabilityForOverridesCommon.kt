// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt
expect open class <!NO_ACTUAL_FOR_EXPECT!>Base<!>()

interface I {
    fun foo(): String?
    fun bar(): String?
    fun baz(s: String?) {}
}

class A : Base(), I {
    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE{JVM}!>override<!> fun foo(): String? = null
    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE{JVM}!>override<!> fun bar(): String? = ""
    <!WRONG_NULLABILITY_FOR_JAVA_OVERRIDE{JVM}!>override<!> fun baz(s: String?) {}
}

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
actual typealias Base = J
