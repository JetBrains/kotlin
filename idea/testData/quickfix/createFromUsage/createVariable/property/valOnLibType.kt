// "Create extension property 'Int.foo'" "true"
// ERROR: Property must be initialized
// WITH_RUNTIME

class A<T>(val n: T)

fun test() {
    val a: A<Int> = 2.<caret>foo
}
