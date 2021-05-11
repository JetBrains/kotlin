// FIR_COMPARISON
fun foo() {
    myFor@
    for (i in 1..10) {
        while (x()) {
            "abc".filter {
                br<caret>
            }
        }
    }
}

// NUMBER: 0
