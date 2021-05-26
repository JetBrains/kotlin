// !LANGUAGE: +ExpectedTypeFromCast

fun foo() = 1

fun <T> foo() = foo() as T

fun <T> foo2(): T = TODO()

val test = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo2<!>().plus("") <!USELESS_CAST!>as String<!>

fun <T> T.bar() = this
val barTest = "".bar() as Number
