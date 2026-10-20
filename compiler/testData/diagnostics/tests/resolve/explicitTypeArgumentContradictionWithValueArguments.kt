// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76718

open class Foo<T>(value: Int)
open class Empty<T>

class Bar1 : Foo<<!UNRESOLVED_REFERENCE!>Wrong<!>>(1)
class Bar2 : Foo<<!UNRESOLVED_REFERENCE!>Wrong<!>>("")
class Bar3 : Empty<<!UNRESOLVED_REFERENCE!>Wrong<!>>()

class Bar4 : Foo<<!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>Wrong<!>> {
    constructor() : super(1)
}

fun <T> foo(value: Int) {}
fun <T> bar() {}

fun testUnresolved() {
    foo<<!UNRESOLVED_REFERENCE!>Wrong<!>>(1)
    bar<<!UNRESOLVED_REFERENCE!>Wrong<!>>()
}

open class Num<T : Number>(value: Int)
open class EmptyNum<T : Number>

class Baz1 : Num<<!UPPER_BOUND_VIOLATED!>String<!>>(1)
class Baz2 : EmptyNum<<!UPPER_BOUND_VIOLATED!>String<!>>()

fun <T : Number> fooNum(value: Int) {}
fun <T : Number> barNum() {}

fun testUpperBound() {
    fooNum<<!UPPER_BOUND_VIOLATED!>String<!>>(1)
    barNum<<!UPPER_BOUND_VIOLATED!>String<!>>()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, nullableType, primaryConstructor,
secondaryConstructor, stringLiteral, typeConstraint, typeParameter */
