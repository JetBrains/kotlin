// !DUMP_CFG
fun test_1() {
    var x: Any = 1
    x = ""
    x.length
}

fun test_2() {
    var x: String? = null
    if (x == null) {
        x = ""
    }
    x.length
}

fun test_3() {
    var x: String? = null
    x = ""
    x.length
    x = null
    x.<!UNSAFE_CALL!>length<!>
}
