// WITH_KOTLIN_JVM_ANNOTATIONS
// LANGUAGE:+DirectJavaActualization
// MODULE: m1-common
// FILE: common.kt
expect class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>KotlinModifiers<!> {
    operator fun plus(i: Int)
    inline fun foo()
    suspend fun bar()
    infix fun qux(i: Int)
}

expect class <!IMPLICIT_JVM_ACTUALIZATION{JVM}!>JavaModifiers<!> {
    fun foo()
    fun bar()
    fun qux()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: KotlinModifiers.java
@kotlin.annotations.jvm.KotlinActual
public class KotlinModifiers {
    @kotlin.annotations.jvm.KotlinActual
    public void plus(int i){}
    @kotlin.annotations.jvm.KotlinActual
    public void foo(){}
    @kotlin.annotations.jvm.KotlinActual
    public void bar(){}
    @kotlin.annotations.jvm.KotlinActual
    public void qux(int i){}
}

// FILE: JavaModifiers.java
@kotlin.annotations.jvm.KotlinActual
public class JavaModifiers {
    @kotlin.annotations.jvm.KotlinActual
    public native void foo();
    @kotlin.annotations.jvm.KotlinActual
    public synchronized void bar(){}
    @kotlin.annotations.jvm.KotlinActual
    public default void qux(){};
}
