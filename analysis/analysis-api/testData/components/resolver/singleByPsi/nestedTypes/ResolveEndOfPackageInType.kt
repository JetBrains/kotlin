package foo.bar.baz

class AA {
    class BB {
        class CC
    }
}

fun test(param: foo.bar.<caret>baz.AA.BB.CC) {}

