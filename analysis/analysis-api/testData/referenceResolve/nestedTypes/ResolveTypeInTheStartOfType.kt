package foo.bar.baz

class AA {
    class BB {
        class CC
    }
}

fun test(param: foo.bar.baz.<caret>AA.BB.CC) {}

