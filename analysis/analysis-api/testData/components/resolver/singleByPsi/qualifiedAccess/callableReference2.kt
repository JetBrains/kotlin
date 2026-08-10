package foo.bar.baz

class AA {
    companion object {
        fun foo() {}
    }
}

fun test() {
    A<caret>A::foo
}

