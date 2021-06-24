package foo.bar.baz

class AA {
    class BB {
        companion object
    }
}

fun test() {
    val b = foo.bar.baz.A<caret>A.BB
}

// REF: (foo.bar.baz).AA
