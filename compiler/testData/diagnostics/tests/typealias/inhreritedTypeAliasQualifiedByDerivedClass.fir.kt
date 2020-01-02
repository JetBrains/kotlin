// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

open class Base {
    typealias Nested = String
}

class Derived : Base()

fun test(x: Derived.Nested) = x

fun Base.testWithImplicitReceiver(x: Nested) {
    val y: Nested = x
}