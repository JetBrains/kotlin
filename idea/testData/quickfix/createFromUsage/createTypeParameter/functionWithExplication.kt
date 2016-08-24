// "Create type parameter 'X' in function 'foo'" "true"
class A<T>

fun foo(x: A<<caret>X>) {

}

fun test() {
    foo(A())
    foo(A<Int>())
}