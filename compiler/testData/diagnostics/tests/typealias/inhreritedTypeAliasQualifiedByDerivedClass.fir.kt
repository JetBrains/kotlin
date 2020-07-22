// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

open class Base {
    typealias Nested = String
}

class Derived : Base()

fun test(x: <!OTHER_ERROR, OTHER_ERROR, OTHER_ERROR, OTHER_ERROR!>Derived.Nested<!>) = x

fun Base.testWithImplicitReceiver(x: <!OTHER_ERROR, OTHER_ERROR!>Nested<!>) {
    val y: <!OTHER_ERROR!>Nested<!> = x
}