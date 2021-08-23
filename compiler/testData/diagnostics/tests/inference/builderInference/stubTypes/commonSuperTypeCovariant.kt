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
fun <R1> build(@BuilderInference block: TestInterface<R1>.() -> Unit): R1 = TODO()

@UseExperimental(ExperimentalTypeInference::class)
fun <R2> build2(@BuilderInference block: TestInterface<R2>.() -> Unit): R2 = TODO()

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
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>select1(get(), getOut())<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>select1(get(), Test.foo(getOut()))<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>select1(Test.foo(get()), Test.foo(getOut()))<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>select1(Test.foo(get()), getOut())<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>select2(get(), getOut())<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>select2(get(), Test.foo(getOut()))<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.String..kotlin.String?)")!>select2(Test.foo(get()), Test.foo(getOut()))<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.String..kotlin.String?)")!>select2(Test.foo(get()), getOut())<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>select3(get(), getOut())<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>select3(get(), Test.foo(getOut()))<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>select3(Test.foo(get()), Test.foo(getOut()))<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>select3(Test.foo(get()), getOut())<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>select4(get(), getOut())<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>select4(get(), Test.foo(getOut()))<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>select4(Test.foo(get()), Test.foo(getOut()))<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>select4(Test.foo(get()), getOut())<!>

        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>select4(id(Test.foo(get())), getOut())<!>

        build2 {
            emit(1)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>select1(this@build.get(), getOut())<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>select1(get(), Test.foo(this@build.getOut()))<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>select1(Test.foo(this@build.get()), Test.foo(getOut()))<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>select1(Test.foo(get()), this@build.getOut())<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>select2(this@build.get(), getOut())<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>select2(get(), Test.foo(this@build.getOut()))<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>select2(Test.foo(this@build.get()), Test.foo(getOut()))<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>select2(Test.foo(get()), this@build.getOut())<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>select3(this@build.get(), getOut())<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>select3(get(), Test.foo(this@build.getOut()))<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>select3(Test.foo(this@build.get()), Test.foo(getOut()))<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>select3(Test.foo(get()), this@build.getOut())<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>select4(this@build.get(), getOut())<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>select4(get(), Test.foo(this@build.getOut()))<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>select4(Test.foo(this@build.get()), Test.foo(getOut()))<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>select4(Test.foo(get()), this@build.getOut())<!>

            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>select4(id(Test.foo(this@build.get())), getOut())<!>
            ""
        }
        ""
    }
}
