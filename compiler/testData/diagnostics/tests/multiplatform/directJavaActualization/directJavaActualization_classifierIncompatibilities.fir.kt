// LANGUAGE:+DirectJavaActualization
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt
interface I

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect class A<!>
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect value class B(val x: Int)<!>
expect fun interface C1 { fun foo() }
expect fun interface C2 { fun foo() }
expect class D1 : I
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect class D2 : I<!>
expect enum class E1 { ONE, TWO }
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect enum class E2 { ONE, <!NO_ACTUAL_FOR_EXPECT{JVM}!>TWO<!> }<!>
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect class Outer {
    class F1
    inner class F2
    <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>inner class F3<!>
    <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>class F4<!>
}<!>

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
