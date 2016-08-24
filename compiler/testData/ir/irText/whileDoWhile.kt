fun test() {
    var x = 0
    while (x < 5) x++
    while (x < 10) { x++ }
    do x++ while (x < 15)
    do { x ++ } while (x < 20)
}

fun testSmartcastInCondition() {
    val a: Any? = null
    if (a is Boolean) {
        while (a) {}
        do {} while (a)
    }
}