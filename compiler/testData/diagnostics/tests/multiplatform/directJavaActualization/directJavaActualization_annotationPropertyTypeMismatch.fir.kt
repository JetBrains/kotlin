// RUN_PIPELINE_TILL: FIR2IR
// WITH_KOTLIN_JVM_ANNOTATIONS
// LANGUAGE:+DirectJavaActualization
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect<!> annotation class Foo<!EXPECT_ACTUAL_MISMATCH{JVM}!>(val <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>foo<!>: Int)<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.annotations.jvm.KotlinActual public @interface Foo {
    @kotlin.annotations.jvm.KotlinActual String foo();
}
