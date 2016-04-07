// KT-11809 Assertion error when delegated property has getter

class A {
    val p1 by this
        get

    var p2 by this
        <!ACCESSOR_FOR_DELEGATED_PROPERTY!>get() = ""<!>

    operator fun getValue(a: Any?, p: Any?) = ""
    operator fun setValue(a: Any?, p: Any?, v: Any?) {}
}
