// RUN_PIPELINE_TILL: FIR2IR
// WITH_KOTLIN_JVM_ANNOTATIONS
// LANGUAGE:+DirectJavaActualization
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt
expect annotation class Foo(val <!KOTLIN_ACTUAL_ANNOTATION_MISSING{JVM}!>foo<!>: Int)

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.annotations.jvm.KotlinActual public @interface Foo {
    int foo();
}
