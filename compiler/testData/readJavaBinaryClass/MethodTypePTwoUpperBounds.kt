package test

trait Foo : java.lang.Object
trait Bar : java.lang.Object

open class MethodTypePTwoUpperBounds() : java.lang.Object() {
    open fun <T> foo(): Unit
        where T : Foo?, T : Bar?
    {}
}
