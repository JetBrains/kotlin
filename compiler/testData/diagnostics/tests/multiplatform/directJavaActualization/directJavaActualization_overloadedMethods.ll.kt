// WITH_KOTLIN_JVM_ANNOTATIONS
// LANGUAGE:+DirectJavaActualization
// MODULE: m1-common
// FILE: common.kt
expect class Foo(name: String, age: Int) {
    constructor(name: String)
    fun foo(a: Int, b: Int): Int
    fun foo(a: Double, b: Double): Double
    fun bar(a: Int, b: Int): Int
    fun bar(a: Double, b: Double): Double
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.annotations.jvm.KotlinActual
public class Foo {
    @kotlin.annotations.jvm.KotlinActual
    public  Foo(String name, int age) { }

    @kotlin.annotations.jvm.KotlinActual
    public  Foo(String name) { }

    @kotlin.annotations.jvm.KotlinActual
    public int foo(int a, int b) {
        return a + b;
    }

    @kotlin.annotations.jvm.KotlinActual
    public double foo(double a, double b) {
        return a + b;
    }

    @kotlin.annotations.jvm.KotlinActual
    public double bar(double a, double b) {
        return a + b;
    }
}