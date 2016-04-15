// "Create type parameter 'X'" "true"
class A<T : List<T>>

interface I : List<I>

fun foo(x: A<<caret>X>) {

}

fun test() {
    foo(A())
    foo(A<I>())
}