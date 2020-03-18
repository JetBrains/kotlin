// !DUMP_CFG
fun test_1(x: Any?) {
    x as String
    x.length
}

fun test_2(x: Any?) {
    if (x as Boolean) {
        x.not()
    }
    x.not()
}

fun test_3(b: Boolean, x: Any?) {
    if (b && x as Boolean) {
        x.not()
    }
    x.<!UNRESOLVED_REFERENCE!>not<!>()

    if (b && x as Boolean == true) {
        x.not()
    }
    x.<!UNRESOLVED_REFERENCE!>not<!>()

    if (b || x as Boolean) {
        x.<!UNRESOLVED_REFERENCE!>not<!>()
    }
    x.<!UNRESOLVED_REFERENCE!>not<!>()
}

fun test_4(b: Any) {
    if (b as? Boolean != null) {
        b.not()
    } else {
        b.<!UNRESOLVED_REFERENCE!>not<!>()
    }
    b.<!UNRESOLVED_REFERENCE!>not<!>()

    if (b as? Boolean == null) {
        b.<!UNRESOLVED_REFERENCE!>not<!>()
    } else {
        b.not()
    }
    b.<!UNRESOLVED_REFERENCE!>not<!>()
}