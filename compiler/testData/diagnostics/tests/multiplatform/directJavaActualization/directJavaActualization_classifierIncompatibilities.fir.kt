// WITH_KOTLIN_JVM_ANNOTATIONS
// MODULE: m1-common
// FILE: common.kt
interface I

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect class A<!>
<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect value class B(val x: Int)<!>
<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect fun interface C1 { fun foo() }<!>
<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect fun interface C2 { fun foo() }<!>
<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect class D1 : I<!>
<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect class D2 : I<!>
<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect enum class E1 { ONE, TWO }<!>
<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect enum class E2 { ONE, TWO }<!>
<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect class Outer {
    class F1
    inner class F2
    inner class F3
    class F4
}<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: A.java
@kotlin.annotations.jvm.KotlinActual public interface A {}
// FILE: B.java
@kotlin.annotations.jvm.KotlinActual public class B {}
// FILE: C1.java
@kotlin.annotations.jvm.KotlinActual public interface C1 { @kotlin.annotations.jvm.KotlinActual public void foo(); }
// FILE: C2.java
@kotlin.annotations.jvm.KotlinActual public interface C2 { @kotlin.annotations.jvm.KotlinActual public void foo(); public void bar(); }
// FILE: D1.java
@kotlin.annotations.jvm.KotlinActual public class D1 implements I {}
// FILE: D2.java
@kotlin.annotations.jvm.KotlinActual public class D2 {}
// FILE: E1.java
@kotlin.annotations.jvm.KotlinActual public enum E1 { ONE, TWO }
// FILE: E2.java
@kotlin.annotations.jvm.KotlinActual public enum E2 { ONE }
// FILE: Outer.java
@kotlin.annotations.jvm.KotlinActual public class Outer {
    @kotlin.annotations.jvm.KotlinActual public static class F1 {}
    @kotlin.annotations.jvm.KotlinActual public class F2 {}
    @kotlin.annotations.jvm.KotlinActual public static class F3 {}
    @kotlin.annotations.jvm.KotlinActual public class F4 {}
}
