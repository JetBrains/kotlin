// LANGUAGE:+DirectJavaActualization
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt
expect class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>Foo<!>

expect class Bar {
    fun typealiasOnly()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.jvm.KotlinActual public class Foo {
    @kotlin.jvm.KotlinActual public void typealiasOnly() {}
}

// FILE: jvm.kt
actual typealias Bar = Foo
