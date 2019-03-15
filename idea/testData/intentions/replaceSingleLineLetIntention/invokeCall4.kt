// IS_APPLICABLE: false
// WITH_RUNTIME
class A {
    operator fun invoke(a: A, i: Int) {}
}

fun foo(a: A) {
    a.<caret>let { it(it, 1) }
}