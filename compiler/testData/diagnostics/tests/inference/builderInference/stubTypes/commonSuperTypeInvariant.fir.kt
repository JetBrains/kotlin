// !LANGUAGE: +UnrestrictedBuilderInference
// !DIAGNOSTICS: -UNUSED_PARAMETER -DEPRECATION -OPT_IN_IS_NOT_ENABLED -UNUSED_VARIABLE
// WITH_STDLIB
// ISSUE: KT-64802 (K2/PCLA difference)

// FILE: Test.java

class Test {
    static <T> T foo(T x) { return x; }
}

// FILE: main.kt
import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalTypeInference::class)
fun <R1> build(block: TestInterface<R1>.() -> Unit): R1 = TODO()

@OptIn(ExperimentalTypeInference::class)
fun <R2> build2(block: TestInterface<R2>.() -> Unit): R2 = TODO()

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

fun test() {
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

        build2 {
            emit(1)
            select1(this@build.get(), getInv())
            select1(get(), <!ARGUMENT_TYPE_MISMATCH!>Test.foo(this@build.getInv())<!>)
            select1(Test.foo(this@build.get()), Test.foo(getInv()))
            select1(Test.foo(get()), <!ARGUMENT_TYPE_MISMATCH!>this@build.getInv()<!>)
            select4(this@build.get(), getInv())
            select4(get(), <!ARGUMENT_TYPE_MISMATCH!>Test.foo(this@build.getInv())<!>)
            select4(Test.foo(this@build.get()), Test.foo(getInv()))
            select4(Test.foo(get()), <!ARGUMENT_TYPE_MISMATCH!>this@build.getInv()<!>)

            select4(id(Test.foo(this@build.get())), getInv())
            ""
        }
        ""
    }

    val ret2 = build {
        emit(if (true) "1" else null)
        select2(get(), getInv())
        select2(get(), Test.foo(getInv()))
        getInv()
        Test.foo(getInv())
        Test.foo(get())
        select2(Test.foo(get()), Test.foo(getInv()))
        select2(Test.foo(get()), getInv())
        select3(get(), getInv())
        select3(get(), Test.foo(getInv()))
        select3(Test.foo(get()), Test.foo(getInv()))
        select3(Test.foo(get()), getInv())

        build2 {
            emit(1)
            select2(this@build.get(), getInv()) // TODO
            select2(get(), <!ARGUMENT_TYPE_MISMATCH!>Test.foo(this@build.getInv())<!>)
            select2(Test.foo(this@build.get()), Test.foo(getInv())) // TODO
            select2(Test.foo(get()), <!ARGUMENT_TYPE_MISMATCH!>this@build.getInv()<!>)
            select3(this@build.get(), getInv()) // TODO
            select3(get(), <!ARGUMENT_TYPE_MISMATCH!>Test.foo(this@build.getInv())<!>)
            select3(Test.foo(this@build.get()), Test.foo(getInv())) // TODO
            select3(Test.foo(get()), <!ARGUMENT_TYPE_MISMATCH!>this@build.getInv()<!>)
            ""
        }
        ""
    }
}
