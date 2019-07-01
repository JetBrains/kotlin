// WITH_RUNTIME
class A {
    operator fun invoke() {}
}

fun foo(a: A) {
    a.<caret>let { it() }
}