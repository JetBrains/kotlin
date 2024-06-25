// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt

expect annotation class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>Foo<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.RequiresOptIn
@kotlin.jvm.KotlinActual
public @interface Foo {}
