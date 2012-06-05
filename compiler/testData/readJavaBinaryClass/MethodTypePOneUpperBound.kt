package test

trait Foo : java.lang.Object

open class MethodTypePOneUpperBound() : java.lang.Object() {
    open fun <T : Foo?> bar() = #()
}
