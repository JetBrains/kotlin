// RUN_PIPELINE_TILL: FIR2IR
// WITH_KOTLIN_JVM_ANNOTATIONS
// LANGUAGE:+DirectJavaActualization
// MODULE: m1-common
// FILE: common.kt
expect class Case1<T> {
    fun <T> foo(a: T)
}

expect class Case2<T: Number>{
    fun foo(a: T)
}

expect class Case3<T> where T: Number , T: Comparable<T>{
    fun foo(a: T)
}

expect class Case4<out T>

expect class Case5<in T>

expect class Case6 {
    fun <T> foo(): T
    fun <T : Any> bar(): List<T>
    fun <S : Comparable<S>> baz(): List<S>
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Case1.java
@kotlin.annotations.jvm.KotlinActual
public class Case1<T> {
    @kotlin.annotations.jvm.KotlinActual
    public <T> void foo(T a){}
}

// FILE: Case2.java
@kotlin.annotations.jvm.KotlinActual
public class Case2<T extends Number> {
    @kotlin.annotations.jvm.KotlinActual
    public void foo(T a){}
}

// FILE: Case3.java
@kotlin.annotations.jvm.KotlinActual
public class Case3 <T extends Number&Comparable<T>> {
    @kotlin.annotations.jvm.KotlinActual
    public void foo(T a){}
}

// FILE: Case4.java
@kotlin.annotations.jvm.KotlinActual
public class Case4<T> { }

// FILE: Case5.java
@kotlin.annotations.jvm.KotlinActual
public class Case5<T> { }

// FILE: Case6.java
import java.util.List;

@kotlin.annotations.jvm.KotlinActual
public class Case6 {
    @kotlin.annotations.jvm.KotlinActual
    public <T> T foo() {}

    @kotlin.annotations.jvm.KotlinActual
    public <T> List<T> bar() { return null; }

    @kotlin.annotations.jvm.KotlinActual
    public <S extends Comparable<S>> List<S> baz() { return null; }
}