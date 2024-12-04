// RUN_PIPELINE_TILL: FIR2IR
// WITH_KOTLIN_JVM_ANNOTATIONS
// LANGUAGE:+DirectJavaActualization
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt
expect class Foo {
    <!JAVA_DIRECT_ACTUALIZATION_DEFAULT_PARAMETERS_IN_ACTUAL_FUNCTION{JVM}!>fun foo(<!JAVA_DIRECT_ACTUALIZATION_DEFAULT_PARAMETERS_IN_EXPECT_FUNCTION{JVM}!>a: Int = 1<!>)<!>

    class Nested {
        <!JAVA_DIRECT_ACTUALIZATION_DEFAULT_PARAMETERS_IN_ACTUAL_FUNCTION{JVM}!>constructor(<!JAVA_DIRECT_ACTUALIZATION_DEFAULT_PARAMETERS_IN_EXPECT_FUNCTION{JVM}!>b: Int = 2<!>)<!>
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
