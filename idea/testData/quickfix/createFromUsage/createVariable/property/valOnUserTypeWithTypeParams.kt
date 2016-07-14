// "Create member property 'A.foo'" "true"
// ERROR: Property must be initialized or be abstract

class A<T>(val n: T)

fun <U> test(u: U) {
    val a: A<U> = A(u).<caret>foo
}
