class C(a: Int, b: Int, c: Int, d: Int, <!UNUSED_PARAMETER!>e<!>: Int = d, val f: String) {
    init {
        a + a
    }

    val g = b

    init {
        c + c
    }
}

fun f(a: Int, b: Int, <!UNUSED_PARAMETER!>c<!>: Int = b) {
    a + a
}

fun Any.getValue(thisRef: Any?, prop: PropertyMetadata): String = ":)"
fun Any.setValue(thisRef: Any?, prop: PropertyMetadata, value: String) {
}

fun Any.propertyDelegated(prop: PropertyMetadata) {
}

fun get(<!UNUSED_PARAMETER!>p<!>: Any) {
}

fun set(<!UNUSED_PARAMETER!>p<!>: Any) {
}

fun foo(s: String) {
    s.<!UNRESOLVED_REFERENCE!>xxx<!> = 1
}