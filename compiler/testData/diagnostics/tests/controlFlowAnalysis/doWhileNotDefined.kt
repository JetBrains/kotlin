fun test(cond1: Boolean) {
    do {
        if (cond1) continue
        val cond2 = false
    } while (<!UNINITIALIZED_VARIABLE!>cond2<!>) // cond2 may be not defined here
}
