// "Create extension function 'A.Companion.foo'" "true"

class A<T>(val n: T)

fun test() {
    val a: Int = A.<caret>foo(2)
}