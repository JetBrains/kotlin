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
    x.<!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>not<!>()<!>

    if (b && x as Boolean == true) {
        x.not()
    }
    x.<!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>not<!>()<!>

    if (b || x as Boolean) {
        x.<!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>not<!>()<!>
    }
    x.<!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>not<!>()<!>
}

fun test_4(b: Any) {
    if (b as? Boolean != null) {
        b.not()
    } else {
        b.<!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>not<!>()<!>
    }
    b.<!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>not<!>()<!>

    if (b as? Boolean == null) {
        b.<!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>not<!>()<!>
    } else {
        b.not()
    }
    b.<!UNRESOLVED_REFERENCE{LT}!><!UNRESOLVED_REFERENCE{PSI}!>not<!>()<!>
}
