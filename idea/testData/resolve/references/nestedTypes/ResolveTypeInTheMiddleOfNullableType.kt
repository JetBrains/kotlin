package foo.bar.baz

class AA {
    class BB {
        class CC
    }
}

fun test(param: foo.bar.baz.AA.<caret>BB.CC?) {}

// REF: (in foo.bar.baz.AA).BB
