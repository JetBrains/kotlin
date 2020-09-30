// !WITH_NEW_INFERENCE
// !LANGUAGE: +ExpectedTypeFromCast

fun foo() = 1

fun <T> foo() = foo() as T

fun <T> foo2(): T = TODO()

// TODO: "not enough information" should be reported on foo2() instead
val test = foo2().<!UNRESOLVED_REFERENCE!>plus<!>("") as String

fun <T> T.bar() = this
val barTest = "".bar() as Number