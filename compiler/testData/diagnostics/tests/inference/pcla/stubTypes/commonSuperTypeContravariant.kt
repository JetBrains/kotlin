// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +UnrestrictedBuilderInference
// DIAGNOSTICS: -UNUSED_PARAMETER -DEPRECATION -OPT_IN_IS_NOT_ENABLED -UNUSED_VARIABLE
// WITH_STDLIB
// ISSUE: KT-64802 (K2/PCLA difference)
// ISSUE: KT-65005 (performance problem)

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

fun test() {
    val ret = build {
        emit("1")
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>select1(get(), getIn())<!>
        select1(get(), Test.foo(getIn()))
        select1(Test.foo(get()), Test.foo(getIn()))
        select1(Test.foo(get()), getIn())
        select4(get(), getIn())
        select4(get(), Test.foo(getIn()))
        select4(Test.foo(get()), Test.foo(getIn()))
        select4(Test.foo(get()), getIn())

        select4(id(Test.foo(get())), getIn())

        build2 {
            emit(1)
            select1(this@build.get(), <!TYPE_MISMATCH("In<String>; In<Int!>")!>getIn()<!>)
            select1(get(), <!TYPE_MISMATCH!>Test.foo(this@build.getIn())<!>)
            select1(Test.foo(this@build.get()), <!TYPE_MISMATCH!>Test.foo(getIn())<!>)
            select1(Test.foo(get()), <!TYPE_MISMATCH!>this@build.getIn()<!>)
            select2(this@build.get(), <!TYPE_MISMATCH!>getIn()<!>)
            select2(get(), <!TYPE_MISMATCH!>Test.foo(this@build.getIn())<!>)
            select2(Test.foo(this@build.get()), <!TYPE_MISMATCH!>Test.foo(getIn())<!>)
            select2(Test.foo(get()), <!TYPE_MISMATCH!>this@build.getIn()<!>)
            select3(this@build.get(), <!TYPE_MISMATCH!>getIn()<!>)
            select3(get(), <!TYPE_MISMATCH!>Test.foo(this@build.getIn())<!>)
            select3(Test.foo(this@build.get()), <!TYPE_MISMATCH!>Test.foo(getIn())<!>)
            select3(Test.foo(get()), <!TYPE_MISMATCH!>this@build.getIn()<!>)
            select4(this@build.get(), <!TYPE_MISMATCH!>getIn()<!>)
            select4(get(), <!TYPE_MISMATCH!>Test.foo(this@build.getIn())<!>)
            select4(Test.foo(this@build.get()), <!TYPE_MISMATCH!>Test.foo(getIn())<!>)
            select4(Test.foo(get()), <!TYPE_MISMATCH!>this@build.getIn()<!>)

            select4(id(Test.foo(this@build.get())), <!TYPE_MISMATCH!>getIn()<!>)
            ""
        }
        ""
    }
    val ret2 = build {
        emit(if (true) "" else null)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>select2(get(), getIn())<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>select2(get(), Test.foo(getIn()))<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>select2(Test.foo(get()), Test.foo(getIn()))<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>select2(Test.foo(get()), getIn())<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>select3(get(), getIn())<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>select3(get(), Test.foo(getIn()))<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>select3(Test.foo(get()), Test.foo(getIn()))<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>select3(Test.foo(get()), getIn())<!>
        ""
    }
}
