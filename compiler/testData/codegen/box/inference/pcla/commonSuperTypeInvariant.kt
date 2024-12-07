// LANGUAGE: +UnrestrictedBuilderInference
// DIAGNOSTICS: -UNUSED_PARAMETER -OPT_IN_IS_NOT_ENABLED -UNUSED_VARIABLE
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
fun <R1> build(block: TestInterface<R1>.() -> Unit) {}

@OptIn(ExperimentalTypeInference::class)
fun <R2> build2(block: TestInterface<R2>.() -> Unit) {}

class Inv<K>

interface TestInterface<R> {
    fun emit(r: R)
    fun get(): R
    fun getInv(): Inv<R>
}

fun <U> id(x: U): U? = x
fun <E> select1(x: E, y: Inv<E>): E? = x
fun <E> select2(x: E, y: Inv<E?>): E = x
fun <E> select3(x: E?, y: Inv<E?>): E = x!!
fun <E> select4(x: E?, y: Inv<E>): E = x!!

fun box(): String {
    val ret1 = build {
        emit("1")
        select1(get(), getInv())
        select1(get(), Test.foo(getInv()))
        select1(Test.foo(get()), Test.foo(getInv()))
        select1(Test.foo(get()), getInv())
        select4(get(), getInv())
        select4(get(), Test.foo(getInv()))
        select4(Test.foo(get()), Test.foo(getInv()))
        select4(Test.foo(get()), getInv())

        select4(id(Test.foo(get())), getInv())
        ""
    }

    val ret2 = build {
        emit(if (true) "1" else null)
        select2(get(), getInv())
        select2(get(), Test.foo(getInv()))
        select2(Test.foo(get()), Test.foo(getInv()))
        select2(Test.foo(get()), getInv())
        select3(get(), getInv())
        select3(get(), Test.foo(getInv()))
        select3(Test.foo(get()), Test.foo(getInv()))
        select3(Test.foo(get()), getInv())
        ""
    }

    return "OK"
}
