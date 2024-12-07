package foo.bar.baz

class AA {
    class BB {
        class CC
    }
}

fun test(param: foo.bar.baz.AA.BB.<caret>CC) {}

