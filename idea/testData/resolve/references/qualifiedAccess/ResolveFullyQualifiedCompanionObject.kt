// IGNORE_FIR
package foo.bar.baz

class AA {
    class BB {
        companion object
    }
}

fun test() {
    val b = foo.bar.baz.AA.B<caret>B
}

// REF: companion object of (in foo.bar.baz.AA).BB
