// ISSUE: KT-29767

fun test(a: MutableList<out Int?>?) {
    if (a != null) {
        val b = a[0] // no SMARTCAST diagnostic
        if (b != null) {
            b.inc()
        }
    }
}
