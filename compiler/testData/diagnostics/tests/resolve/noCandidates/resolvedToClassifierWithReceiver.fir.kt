// !DIAGNOSTICS: -UNUSED_VARIABLE

object X {
    interface A

    object B
    class C
}

fun testX() {
    val interface_as_fun = X.<!RESOLUTION_TO_CLASSIFIER!>A<!>()
    val interface_as_val = X.<!NO_COMPANION_OBJECT!>A<!>

    val object_as_fun = X.<!INVISIBLE_REFERENCE!>B<!>()
    val class_as_val = X.<!NO_COMPANION_OBJECT!>C<!>
}

class Y {
    interface A

    object B
    class C
}

fun testY() {
    val interface_as_fun = Y.<!RESOLUTION_TO_CLASSIFIER!>A<!>()
    val interface_as_val = Y.<!NO_COMPANION_OBJECT!>A<!>

    val object_as_fun = Y.<!INVISIBLE_REFERENCE!>B<!>()
    val class_as_val = Y.<!NO_COMPANION_OBJECT!>C<!>
}

fun test(x: X) {
    val interface_as_fun = x.<!UNRESOLVED_REFERENCE!>A<!>()
    val interface_as_val = x.<!UNRESOLVED_REFERENCE!>A<!>

    val object_as_fun = x.<!UNRESOLVED_REFERENCE!>B<!>()
    val class_as_val = x.<!UNRESOLVED_REFERENCE!>C<!>
}
