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

fun Any.get(thisRef: Any?, prop: PropertyMetadata): String = ":)"
fun Any.set(thisRef: Any?, prop: PropertyMetadata, value: String) {
}

fun Any.propertyDelegated(prop: PropertyMetadata) {
}

fun get(p: Any) {
}

fun set(p: Any) {
}
