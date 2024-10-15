// RUN_PIPELINE_TILL: BACKEND
// WITH_KOTLIN_JVM_ANNOTATIONS
// LANGUAGE:+DirectJavaActualization
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt
expect annotation class Foo(val foo: Int = 42)

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.annotations.jvm.KotlinActual public @interface Foo {
    @kotlin.annotations.jvm.KotlinActual int foo() default 42;
}
