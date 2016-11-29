// "Create type parameter 'X' in class 'Foo'" "true"
class A<T : List<Int>>

open class Foo(x: A<<caret>X>)

class Bar : Foo(A())

fun test() {
    Foo(A())
    Foo(A<List<Int>>())

    object : Foo(A<List<Int>>()) {

    }
}