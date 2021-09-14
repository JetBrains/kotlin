package foo.bar.baz

class AA {
    class BB {
        class CC
    }
}

fun test(param: <caret>foo.bar.baz.AA.BB.CC) {}

