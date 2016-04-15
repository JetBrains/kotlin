// "Create type parameter 'X'" "true"
class A<T : List<T>>

interface I : List<I>

open class Foo(x: A<<caret>X>)

class Bar : Foo(A())

fun test() {
    Foo(A())
    Foo(A<I>())

    object : Foo(A<I>()) {

    }
}