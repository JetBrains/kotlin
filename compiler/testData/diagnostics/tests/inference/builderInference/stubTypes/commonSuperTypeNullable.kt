// !LANGUAGE: +UnrestrictedBuilderInference
// !DIAGNOSTICS: -UNUSED_PARAMETER -OPT_IN_IS_NOT_ENABLED -UNUSED_VARIABLE
// WITH_STDLIB

// FILE: Test.java

import org.jetbrains.annotations.*;

class Test {
    @Nullable
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

fun <U> id(x: U): U? = x
fun <E> select(vararg x: E): E? = x[0]

fun test() {
    val ret = build {
        emit("1")
        <!DEBUG_INFO_EXPRESSION_TYPE("TypeVariable(R)?")!>Test.foo(get())<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<TypeVariable(R)>?")!>Test.foo(getInv())<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>id(get())<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>select(get(), get())<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>select(Test.foo(get()), Test.foo(get()))<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>select(Test.foo(get()), get())<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.String>?")!>select(getInv(), getInv())<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.String>?")!>select(Test.foo(getInv()), Test.foo(getInv()))<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.String>?")!>select(Test.foo(getInv()), getInv())<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.String>?")!>select(getInv(), Test.foo(getInv()))<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>select(id(get()), id(get()))<!>
        <!INFERRED_INTO_DECLARED_UPPER_BOUNDS!>build2<!> {
            emit(1)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>select(this@build.get(), get())<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>select(Test.foo(this@build.get()), Test.foo(get()))<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Any?>?")!>select(this@build.getInv(), getInv())<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Any?>?")!>select(Test.foo(this@build.getInv()), Test.foo(getInv()))<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Any?>?")!>select(Test.foo(this@build.getInv()), getInv())<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>select(id(this@build.get()), id(get()))<!>
            ""
        }
        ""
    }
    val ret2 = build {
        emit("1")
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>select(get(), null)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>select(Test.<!IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION!>foo<!>(null), Test.foo(get()))<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>select(Test.foo(get()), null)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.String>?")!>select(null, getInv())<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.String>?")!>select(Test.foo(getInv()), Test.<!IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION!>foo<!>(null))<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.String>?")!>select(Test.<!IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION!>foo<!>(null), getInv())<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.String>?")!>select(getInv(), Test.<!IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION!>foo<!>(null))<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String?")!>select(<!IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION!>id<!>(null), id(get()))<!>
        <!INFERRED_INTO_DECLARED_UPPER_BOUNDS!>build2<!> {
            emit(1)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>select(this@build.get(), get(), null)<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>select(Test.foo(this@build.get()), Test.foo(get()), null)<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Any?>?")!>select(this@build.getInv(), getInv(), null)<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Any?>?")!>select(Test.foo(this@build.getInv()), Test.foo(getInv()), null)<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("Inv<out kotlin.Any?>?")!>select(Test.foo(this@build.getInv()), getInv(), null)<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>select(id(this@build.get()), id(get()), null)<!>
            ""
        }
        ""
    }
}
