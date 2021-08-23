// !LANGUAGE: +UnrestrictedBuilderInference
// !DIAGNOSTICS: -UNUSED_PARAMETER -DEPRECATION -EXPERIMENTAL_IS_NOT_ENABLED -UNUSED_VARIABLE
// WITH_RUNTIME

// FILE: Test.java

class Test {
    static <T> T foo(T x) { return x; }
}

// FILE: main.kt
import kotlin.experimental.ExperimentalTypeInference

@UseExperimental(ExperimentalTypeInference::class)
fun <R1> build(@<!EXPERIMENTAL_API_USAGE_ERROR!>BuilderInference<!> block: TestInterface<R1>.() -> Unit): R1 = TODO()

@UseExperimental(ExperimentalTypeInference::class)
fun <R2> build2(@<!EXPERIMENTAL_API_USAGE_ERROR!>BuilderInference<!> block: TestInterface<R2>.() -> Unit): R2 = TODO()

class Out<out K>

interface TestInterface<R> {
    fun emit(r: R)
    fun get(): R
    fun getOut(): Out<R>
}

fun <U> id(x: U): U? = x
fun <E> select1(x: E, y: Out<E>): E? = x
fun <E> select2(x: E, y: Out<E?>): E = x
fun <E> select3(x: E?, y: Out<E?>): E = x!!
fun <E> select4(x: E?, y: Out<E>): E = x!!

fun test() {
    val ret = build {
        emit("1")
        select1(get(), getOut())
        select1(get(), Test.foo(getOut()))
        select1(Test.foo(get()), Test.foo(getOut()))
        select1(Test.foo(get()), getOut())
        select2(get(), getOut())
        select2(get(), Test.foo(getOut()))
        select2(Test.foo(get()), Test.foo(getOut()))
        select2(Test.foo(get()), getOut())
        select3(get(), getOut())
        select3(get(), Test.foo(getOut()))
        select3(Test.foo(get()), Test.foo(getOut()))
        select3(Test.foo(get()), getOut())
        select4(get(), getOut())
        select4(get(), Test.foo(getOut()))
        select4(Test.foo(get()), Test.foo(getOut()))
        select4(Test.foo(get()), getOut())

        select4(id(Test.foo(get())), getOut())

        build2 {
            emit(1)
            select1(this@build.get(), getOut())
            select1(get(), Test.foo(this@build.getOut()))
            select1(Test.foo(this@build.get()), Test.foo(getOut()))
            select1(Test.foo(get()), this@build.getOut())
            select2(this@build.get(), getOut())
            select2(get(), Test.foo(this@build.getOut()))
            select2(Test.foo(this@build.get()), Test.foo(getOut()))
            select2(Test.foo(get()), this@build.getOut())
            select3(this@build.get(), getOut())
            select3(get(), Test.foo(this@build.getOut()))
            select3(Test.foo(this@build.get()), Test.foo(getOut()))
            select3(Test.foo(get()), this@build.getOut())
            select4(this@build.get(), getOut())
            select4(get(), Test.foo(this@build.getOut()))
            select4(Test.foo(this@build.get()), Test.foo(getOut()))
            select4(Test.foo(get()), this@build.getOut())

            select4(id(Test.foo(this@build.get())), getOut())
            ""
        }
        ""
    }
}
