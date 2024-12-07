// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +UnrestrictedBuilderInference
// DIAGNOSTICS: -UNUSED_PARAMETER -OPT_IN_IS_NOT_ENABLED -UNUSED_VARIABLE
// WITH_STDLIB

// FILE: Test.java

class Test {
    static <T> T foo(T x) { return x; }
}

// FILE: main.kt
import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalTypeInference::class)
fun <R> build(block: TestInterface<R>.() -> Unit): R = TODO()

@OptIn(ExperimentalTypeInference::class)
fun <R> build2(block: TestInterface<R>.() -> Unit): R = TODO()

class Inv<K>

interface TestInterface<R> {
    fun emit(r: R)
    fun get(): R
    fun getInv(): Inv<R>
}

fun <U> id(x: U) = x
fun <E> select(vararg x: E) = x[0]

fun test() {
    val ret = build {
        emit("1")
        Test.foo(get())
        Test.foo(getInv())
        id(get())
        select(get(), get())
        select(Test.foo(get()), Test.foo(get()))
        select(Test.foo(get()), get())
        select(getInv(), getInv())
        select(Test.foo(getInv()), Test.foo(getInv()))
        select(Test.foo(getInv()), getInv())
        select(getInv(), Test.foo(getInv()))
        select(id(get()), id(get()))
        build2 {
            emit(1)
            select(this@build.get(), get())
            select(Test.foo(this@build.get()), Test.foo(get()))
            select(this@build.getInv(), getInv())
            select(Test.foo(this@build.getInv()), Test.foo(getInv()))
            select(Test.foo(this@build.getInv()), getInv())
            select(id(this@build.get()), id(get()))
            ""
        }
        ""
    }
}
