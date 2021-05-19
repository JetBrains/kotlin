// FIR_IDENTICAL
// FIR_COMPARISON
fun foo() {
    myFor@
    for (i in 1..10) {
        while (x()) {
            fun localFun(a: Int) {
                if (a > 0) {
                    con<caret>
                }
            }
        }
    }
}

// NUMBER: 0
