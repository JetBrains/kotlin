package foo.bar.baz

class AA {
    class BB {
        class CC
    }
}

fun test(param: foo.<caret>bar.baz.AA.BB.CC) {}

