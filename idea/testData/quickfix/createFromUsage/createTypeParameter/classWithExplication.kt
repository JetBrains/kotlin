// "Create type parameter 'X'" "true"
class A<T>

open class Foo(x: A<<caret>X>)

class Bar : Foo(A())

fun test() {
    Foo(A())
    Foo(A<Int>())

    object : Foo(A<Int>()) {

    }
}