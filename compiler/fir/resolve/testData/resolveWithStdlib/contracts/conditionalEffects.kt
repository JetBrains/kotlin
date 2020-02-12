fun test_1(x: Any) {
    require(x is Int)
    x.inc()
}

fun test_2(x: String?) {
    requireNotNull(x)
    x.length
}

fun test_3(x: String?) {
    require(x != null)
    x.length
}

fun test_4(x: Any, y: String?) {
    require(x is String && y != null)
    x.length
    y.length
}

fun test_5(x: Any, b: Boolean) {
    if (b) {
        require(x is String)
        x.length
    } else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    x.<!UNRESOLVED_REFERENCE!>length<!>
}

fun test_6(x: Any, b: Boolean) {
    if (b) {
        require(x is String)
        x.length
    } else {
        require(x is String)
        x.length
    }
    x.length
}