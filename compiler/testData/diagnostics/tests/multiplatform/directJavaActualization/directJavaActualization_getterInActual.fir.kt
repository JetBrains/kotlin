// WITH_KOTLIN_JVM_ANNOTATIONS
// LANGUAGE:+DirectJavaActualization
// ISSUES: KT-71809
// MODULE: m1-common
// FILE: common.kt
interface I {
    val foo: Int
}

expect class Foo {
    val <!KOTLIN_ACTUAL_ANNOTATION_MISSING{JVM}!>foo<!>: Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.annotations.jvm.KotlinActual
public class Foo implements I {
    @kotlin.annotations.jvm.KotlinActual
    @Override
    public int getFoo() {
        return 0;
    }
}