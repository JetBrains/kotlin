package foo.bar.baz

class AA {
    class BB {
        companion object
    }
}

fun test() {
    val b = f<caret>oo.bar.baz.AA.BB
}

// REF: foo
