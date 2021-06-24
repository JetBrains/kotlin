// IGNORE_FE10
package foo.bar.baz

class AA {
    companion object {
        fun foo() {}
    }
}

fun test() {
    A<caret>A::foo // FE1.0 won't resolve this
}

// REF: companion object of (foo.bar.baz).AA
