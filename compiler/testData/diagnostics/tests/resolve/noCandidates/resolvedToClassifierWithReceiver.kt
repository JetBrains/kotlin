// !DIAGNOSTICS: -UNUSED_VARIABLE

object X {
    interface A

    object B
    class C
}

fun testX() {
    val interface_as_fun = X.<!RESOLUTION_TO_CLASSIFIER!>A<!>()
    val interface_as_val = X.<!NO_COMPANION_OBJECT!>A<!>

    val object_as_fun = X.<!FUNCTION_EXPECTED!>B<!>()
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

    val object_as_fun = Y.<!FUNCTION_EXPECTED!>B<!>()
    val class_as_val = Y.<!NO_COMPANION_OBJECT!>C<!>
}

fun test(x: X) {
    val interface_as_fun = x.<!RESOLUTION_TO_CLASSIFIER!>A<!>()
    val interface_as_val = x.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE, NO_COMPANION_OBJECT!>A<!>

    val object_as_fun = x.<!RESOLUTION_TO_CLASSIFIER!>B<!>()
    val class_as_val = x.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE, NO_COMPANION_OBJECT!>C<!>
}