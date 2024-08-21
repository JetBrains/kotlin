// LANGUAGE:+DirectJavaActualization
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt

expect class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>Foo<!>() {
    fun foo()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java

@kotlin.jvm.KotlinActual public class Foo {
    @kotlin.jvm.KotlinActual public Foo() {}
    @kotlin.jvm.KotlinActual public void foo() {
    }
}
