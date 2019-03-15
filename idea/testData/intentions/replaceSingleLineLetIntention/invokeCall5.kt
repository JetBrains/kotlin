// IS_APPLICABLE: false
// WITH_RUNTIME
class A {
    operator fun invoke() {}
}

fun foo(a: A) {
    (1 to a).<caret>let { (i, b) -> b() }
}