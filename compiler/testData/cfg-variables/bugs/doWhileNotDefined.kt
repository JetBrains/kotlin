fun test(cond1: Boolean) {
    do {
        if (cond1) continue
        val cond2 = false
    } while (cond2)
}
