// "Create member function 'A.foo'" "true"

class A<T>(val n: T) {

}

fun test() {
    val a: A<Int> = A(1).<caret>foo { p: Int -> p + 1 }
}