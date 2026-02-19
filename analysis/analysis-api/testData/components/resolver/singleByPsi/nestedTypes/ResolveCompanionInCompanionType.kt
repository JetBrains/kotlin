package foo.bar.baz

class AA {
    class BB {
        companion object
    }
}

fun test(param: foo.bar.baz.AA.BB.<caret>Companion) {}

