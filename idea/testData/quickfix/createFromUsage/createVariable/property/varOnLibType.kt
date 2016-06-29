// "Create extension property 'Int.foo'" "true"
// ERROR: Property must be initialized
// WITH_RUNTIME

class A<T>(val n: T)

fun test() {
    2.<caret>foo = A("2")
}
