// LANGUAGE: +NestedTypeAliases

class Pair<X, Y>(val x: X, val y: Y)

class C {
    typealias P<X, Y> = Pair<X, Y>
}

fun test() {
    C.P<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String><!>("str0", 0)
    C.P<Int, String>(<!ARGUMENT_TYPE_MISMATCH!>"str0"<!>, <!ARGUMENT_TYPE_MISMATCH!>0<!>)
    C.P<String, Int>("str0", 0) // OK
    C.P("str0", 0) // OK
    C.P(0, "str0") // OK
}
