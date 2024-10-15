// RUN_PIPELINE_TILL: FIR2IR
// WITH_KOTLIN_JVM_ANNOTATIONS
// LANGUAGE:+DirectJavaActualization

// MODULE: m1-common
// FILE: common.kt
expect class Foo() {
    fun foo()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.annotations.jvm.KotlinActual public class Foo {
    @kotlin.annotations.jvm.KotlinActual public Foo() { }
    @kotlin.annotations.jvm.KotlinActual public static void foo() { }
}
