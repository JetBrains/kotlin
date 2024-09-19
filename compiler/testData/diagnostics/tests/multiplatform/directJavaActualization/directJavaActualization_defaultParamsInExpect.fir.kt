// WITH_KOTLIN_JVM_ANNOTATIONS
// LANGUAGE:+DirectJavaActualization
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect<!> class Foo {
    fun foo(a: Int = 1)

    class <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>Nested<!> {
        <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>constructor(b: Int = 2)<!>
    }
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.annotations.jvm.KotlinActual public class Foo {
    @kotlin.annotations.jvm.KotlinActual public void foo(int a) {
    }

    @kotlin.annotations.jvm.KotlinActual public static class Nested {
        @kotlin.annotations.jvm.KotlinActual public Nested(int b) {}
    }
}
