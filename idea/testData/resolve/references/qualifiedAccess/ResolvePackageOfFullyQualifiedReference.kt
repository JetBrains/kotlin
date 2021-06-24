package foo.bar.baz

class AA {
    class BB {
        companion object
    }
}

fun test() {
    val b = foo.bar.b<caret>az.AA.BB
}

// REF: baz
