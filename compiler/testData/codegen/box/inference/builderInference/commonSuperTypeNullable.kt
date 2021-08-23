// !LANGUAGE: +UnrestrictedBuilderInference
// !DIAGNOSTICS: -UNUSED_PARAMETER -DEPRECATION -EXPERIMENTAL_IS_NOT_ENABLED -UNUSED_VARIABLE
// WITH_RUNTIME
// TARGET_BACKEND: JVM
// DONT_TARGET_EXACT_BACKEND: WASM

// FILE: Test.java

import org.jetbrains.annotations.*;

class Test {
    @Nullable
    static <T> T foo(T x) { return x; }
}

// FILE: main.kt
import kotlin.experimental.ExperimentalTypeInference

@UseExperimental(ExperimentalTypeInference::class)
fun <R> build(@BuilderInference block: TestInterface<R>.() -> Unit) {}

@UseExperimental(ExperimentalTypeInference::class)
fun <R> build2(@BuilderInference block: TestInterface<R>.() -> Unit) {}

class Inv<K>

interface TestInterface<R> {
    fun emit(r: R)
    fun get(): R
    fun getInv(): Inv<R>
}

fun <U> id(x: U): U? = x
fun <E> select(vararg x: E): E? = x[0]

fun box(): String {
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
    val ret2 = build {
        emit("1")
        select(get(), null)
        select(Test.foo(null), Test.foo(get()))
        select(Test.foo(get()), null)
        select(null, getInv())
        select(Test.foo(getInv()), Test.foo(null))
        select(Test.foo(null), getInv())
        select(getInv(), Test.foo(null))
        select(id(null), id(get()))
        build2 {
            emit(1)
            select(this@build.get(), get(), null)
            select(Test.foo(this@build.get()), Test.foo(get()), null)
            select(this@build.getInv(), getInv(), null)
            select(Test.foo(this@build.getInv()), Test.foo(getInv()), null)
            select(Test.foo(this@build.getInv()), getInv(), null)
            select(id(this@build.get()), id(get()), null)
            ""
        }
        ""
    }
    return "OK"
}
