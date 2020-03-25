// !DUMP_CFG
fun test_1(x: Any) {
    if (x is String) {
        x.length
    }
    x.<!UNRESOLVED_REFERENCE!>length<!>
}

fun test_2(x: Any) {
    val b = x is String
    if (b) {
        x.length
    }
    x.<!UNRESOLVED_REFERENCE!>length<!>
}

fun test_3(x: Any) {
    when {
        x !is String -> {}
        x !is Int -> {}
        else -> {
            x.length
            x.inc()
        }
    }
}