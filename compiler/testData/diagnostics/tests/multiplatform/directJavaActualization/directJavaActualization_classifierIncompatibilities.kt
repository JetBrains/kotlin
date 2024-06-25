// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt
interface I

expect class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>A<!>
expect value class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>B<!>(val x: Int)
expect fun interface <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>C1<!> { fun foo() }
expect fun interface <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>C2<!> { fun foo() }
expect class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>D1<!> : I
expect class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>D2<!> : I
expect enum class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>E1<!> { ONE, TWO }
expect enum class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>E2<!> { ONE, TWO }
expect class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>Outer<!> {
    class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>F1<!>
    inner class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>F2<!>
    inner class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>F3<!>
    class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>F4<!>
}

// MODULE: m2-jvm()()(m1-common)
// FILE: A.java
@kotlin.jvm.KotlinActual public interface A {}
// FILE: B.java
@kotlin.jvm.KotlinActual public class B {}
// FILE: C1.java
@kotlin.jvm.KotlinActual public interface C1 { @kotlin.jvm.KotlinActual public void foo(); }
// FILE: C2.java
@kotlin.jvm.KotlinActual public interface C2 { @kotlin.jvm.KotlinActual public void foo(); public void bar(); }
// FILE: D1.java
@kotlin.jvm.KotlinActual public class D1 implements I {}
// FILE: D2.java
@kotlin.jvm.KotlinActual public class D2 {}
// FILE: E1.java
@kotlin.jvm.KotlinActual public enum E1 { ONE, TWO }
// FILE: E2.java
@kotlin.jvm.KotlinActual public enum E2 { ONE }
// FILE: Outer.java
@kotlin.jvm.KotlinActual public class Outer {
    @kotlin.jvm.KotlinActual public static class F1 {}
    @kotlin.jvm.KotlinActual public class F2 {}
    @kotlin.jvm.KotlinActual public static class F3 {}
    @kotlin.jvm.KotlinActual public class F4 {}
}
