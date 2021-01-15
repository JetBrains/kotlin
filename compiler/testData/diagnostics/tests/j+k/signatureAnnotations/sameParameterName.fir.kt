
// ANDROID_ANNOTATIONS
// FILE: A.java
import kotlin.annotations.jvm.internal.*;

public class A {
    public void same(@ParameterName("ok") String first, @ParameterName("ok") String second) {
    }
}

// FILE: test.kt
fun main() {
    val test = A()
    test.same("hello", "world")
    test.<!INAPPLICABLE_CANDIDATE!>same<!>(ok = "hello", ok = <!UNRESOLVED_REFERENCE!>world<!>)
    test.<!INAPPLICABLE_CANDIDATE!>same<!>("hello", ok = "world")
}
