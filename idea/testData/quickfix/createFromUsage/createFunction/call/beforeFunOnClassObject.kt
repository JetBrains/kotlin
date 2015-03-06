// "Create function 'foo'" "true"

class A<T>(val n: T) {
    default object {

    }
}

fun test() {
    val a: Int = A.<caret>foo(2)
}