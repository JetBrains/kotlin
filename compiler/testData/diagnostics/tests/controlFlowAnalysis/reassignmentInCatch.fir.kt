// ISSUE: KT-42995

fun test() {
    val a: Int
    try {
        run {
            a = 1
            a.inc()
            throw Exception("hmm")
        }
    } catch (e: Exception) {
        <!VAL_REASSIGNMENT!>a<!> = 2
        a.inc()
    }
}
