// !LANGUAGE: +UnrestrictedBuilderInference
// WITH_STDLIB
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: WASM

// FILE: Test.java

class Test {
    static <T> T foo(T x) { return x; }
}

// FILE: main.kt
import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalTypeInference::class)
fun <R1> build(@BuilderInference block: TestInterface<R1>.() -> Unit) {}

@OptIn(ExperimentalTypeInference::class)
fun <R2> build2(@BuilderInference block: TestInterface<R2>.() -> Unit) {}

class In<in K>

interface TestInterface<R> {
    fun emit(r: R)
    fun get(): R
    fun getIn(): In<R>
}

fun <U> id(x: U) = x
fun <E> intersect(vararg x: In<E>): E = null as E

fun box(): String {
    val ret = build {
        emit("1")
        intersect(getIn(), getIn())
        intersect(getIn(), Test.foo(getIn()))
        intersect(Test.foo(getIn()), Test.foo(getIn()))
        intersect(Test.foo(getIn()), getIn())
        ""
    }
    return "OK"
}
