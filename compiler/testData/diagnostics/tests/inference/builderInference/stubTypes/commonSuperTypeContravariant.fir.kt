// !LANGUAGE: +UnrestrictedBuilderInference
// !DIAGNOSTICS: -UNUSED_PARAMETER -DEPRECATION -EXPERIMENTAL_IS_NOT_ENABLED -UNUSED_VARIABLE
// WITH_RUNTIME

// FILE: Test.java

class Test {
    static <T> T foo(T x) { return x; }
}

// FILE: main.kt
import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalTypeInference::class)
fun <R1> build(@BuilderInference block: TestInterface<R1>.() -> Unit): R1 = TODO()

@OptIn(ExperimentalTypeInference::class)
fun <R2> build2(@BuilderInference block: TestInterface<R2>.() -> Unit): R2 = TODO()

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
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>select1(get(), Test.foo(getIn()))<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>select1(Test.foo(get()), Test.foo(getIn()))<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>select1(Test.foo(get()), getIn())<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>select4(get(), getIn())<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>select4(get(), Test.foo(getIn()))<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>select4(Test.foo(get()), Test.foo(getIn()))<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>select4(Test.foo(get()), getIn())<!>

        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>select4(id(Test.foo(get())), getIn())<!>

        build2 {
            emit(1)
            select1(this@build.get(), getIn())
            select1(get(), Test.foo(this@build.getIn()))
            select1(Test.foo(this@build.get()), Test.foo(getIn()))
            select1(Test.foo(get()), this@build.getIn())
            select2(this@build.get(), getIn())
            select2(get(), Test.foo(this@build.getIn()))
            select2(Test.foo(this@build.get()), Test.foo(getIn()))
            select2(Test.foo(get()), this@build.getIn())
            select3(this@build.get(), getIn())
            select3(get(), Test.foo(this@build.getIn()))
            select3(Test.foo(this@build.get()), Test.foo(getIn()))
            select3(Test.foo(get()), this@build.getIn())
            select4(this@build.get(), getIn())
            select4(get(), Test.foo(this@build.getIn()))
            select4(Test.foo(this@build.get()), Test.foo(getIn()))
            select4(Test.foo(get()), this@build.getIn())

            select4(id(Test.foo(this@build.get())), getIn())
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
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>select3(get(), getIn())<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>select3(get(), Test.foo(getIn()))<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>select3(Test.foo(get()), Test.foo(getIn()))<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>select3(Test.foo(get()), getIn())<!>
        ""
    }
}
