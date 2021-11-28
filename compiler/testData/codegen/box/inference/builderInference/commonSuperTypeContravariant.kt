// !LANGUAGE: +UnrestrictedBuilderInference
// WITH_STDLIB
// IGNORE_BACKEND: WASM
// TARGET_BACKEND: JVM

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

fun <U> id(x: U): U? = x
fun <E> select1(x: E, y: In<E>): E? = x
fun <E> select2(x: E, y: In<E?>): E = x
fun <E> select3(x: E?, y: In<E?>): E = x!!
fun <E> select4(x: E?, y: In<E>): E = x!!

fun box(): String {
    val ret = build {
        emit("1")
        select1(get(), getIn())
        select1(get(), Test.foo(getIn()))
        select1(Test.foo(get()), Test.foo(getIn()))
        select1(Test.foo(get()), getIn())
        select4(get(), getIn())
        select4(get(), Test.foo(getIn()))
        select4(Test.foo(get()), Test.foo(getIn()))
        select4(Test.foo(get()), getIn())

        select4(id(Test.foo(get())), getIn())
        ""
    }
    val ret2 = build {
        emit(if (true) "" else null)
        select2(get(), getIn())
        select2(get(), Test.foo(getIn()))
        select2(Test.foo(get()), Test.foo(getIn()))
        select2(Test.foo(get()), getIn())
        select3(get(), getIn())
        select3(get(), Test.foo(getIn()))
        select3(Test.foo(get()), Test.foo(getIn()))
        select3(Test.foo(get()), getIn())
        ""
    }

    return "OK"
}
