// !LANGUAGE: +ExpectedTypeFromCast

fun foo() = 1

fun <T> foo() = foo() <!UNCHECKED_CAST!>as T<!>

fun <T> foo2(): T = TODO()

val test = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo2<!>().<!DEBUG_INFO_MISSING_UNRESOLVED!>plus<!>("") as String

fun <T> T.bar() = this
val barTest = "".bar() <!CAST_NEVER_SUCCEEDS!>as<!> Number
