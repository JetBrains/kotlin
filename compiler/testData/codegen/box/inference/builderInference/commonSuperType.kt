// !LANGUAGE: +UnrestrictedBuilderInference
// !DIAGNOSTICS: -UNUSED_PARAMETER -OPT_IN_IS_NOT_ENABLED -UNUSED_VARIABLE
// WITH_STDLIB
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

fun <U> id(x: U) = x
fun <E> select(vararg x: E): E = TODO()

fun box(): String {
    val ret = build {
        emit("1")
        //Test.foo(get())
        build2 {
            emit(1)
            this@build.get()

            //select(, get())
            ""
        }
        ""
    }

    return "OK"
}
