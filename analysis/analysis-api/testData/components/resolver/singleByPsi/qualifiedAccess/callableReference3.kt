package foo.bar.baz

class AA {
    companion object
}

fun AA.Companion.foo() {}

fun test() {
    A<caret>A::foo
}

