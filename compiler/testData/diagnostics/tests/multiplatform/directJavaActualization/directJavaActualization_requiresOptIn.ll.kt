// WITH_KOTLIN_JVM_ANNOTATIONS
// MODULE: m1-common
// FILE: common.kt

expect annotation class Foo

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.RequiresOptIn
@kotlin.annotations.jvm.KotlinActual
public @interface Foo {}
