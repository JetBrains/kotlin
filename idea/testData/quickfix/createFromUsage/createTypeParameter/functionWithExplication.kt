// "Create type parameter 'X'" "true"
class A<T>

fun foo(x: A<<caret>X>) {

}

fun test() {
    foo(A())
    foo(A<Int>())
}