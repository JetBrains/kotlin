// "Create property 'foo' from usage" "true"
// ERROR: Property must be initialized or be abstract

class A<T>(val n: T)

fun test<U>(u: U) {
    val a: A<U> = A(u).<caret>foo
}
