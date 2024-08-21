// LANGUAGE:+DirectJavaActualization
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt
expect fun interface <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>Foo<!> {
    fun foo()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.jvm.KotlinActual public interface Foo extends Base {
    @kotlin.jvm.KotlinActual public void foo();
}

// FILE: Base.java
public interface Base {
    void bar();
}
