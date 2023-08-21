// !LANGUAGE: +UnrestrictedBuilderInference
// !DIAGNOSTICS: -UNUSED_PARAMETER -OPT_IN_IS_NOT_ENABLED -UNUSED_VARIABLE
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
fun <R1> build(block: TestInterface<R1>.() -> Unit) {}

@OptIn(ExperimentalTypeInference::class)
fun <R2> build2(block: TestInterface<R2>.() -> Unit) {}

class Out<out K>

interface TestInterface<R> {
    fun emit(r: R)
    fun get(): R
    fun getOut(): Out<R>
}

fun <U> id(x: U): U? = x
fun <F> id1(x: F): F? = x
fun <E> select1(x: E, y: Out<E>): E? = x
fun <E> select2(x: E, y: Out<E?>): E = x
fun <E> select3(x: E?, y: Out<E?>): E = x!!
fun <E> select4(x: E?): E = x!!

fun box(): String {
    val ret = build {
        emit("1")


        select4(id(id1(get())))

        ""
    }

    return "OK"
}
