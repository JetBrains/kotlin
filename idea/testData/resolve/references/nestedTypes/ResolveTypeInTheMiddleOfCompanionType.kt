package foo.bar.baz

class AA {
    class BB {
        companion object
    }
}

fun test(param: foo.bar.baz.AA.<caret>BB.Companion) {}

// REF: (in foo.bar.baz.AA).BB
