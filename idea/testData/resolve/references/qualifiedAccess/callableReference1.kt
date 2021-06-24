package foo.bar.baz

class AA {
    fun foo() {}
}

fun test() {
    A<caret>A::foo
}

// REF: (foo.bar.baz).AA
