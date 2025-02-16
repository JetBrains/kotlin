// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-49035, KT-51201

@file:Suppress("INVISIBLE_MEMBER", <!ERROR_SUPPRESSION!>"INVISIBLE_REFERENCE"<!>)

fun <T> foo(it: @kotlin.internal.Exact T) {}

fun main() {
    foo<Any>(<!ARGUMENT_TYPE_MISMATCH("String; Any")!>""<!>)
}

interface I
class Foo : I
class Bar

fun <MY_TYPE_PARAM : I> myRun(action: () -> MY_TYPE_PARAM): MY_TYPE_PARAM = action()

val a = myRun<Foo> { <!RETURN_TYPE_MISMATCH!>Bar()<!> }
