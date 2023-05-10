// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: A.kt
class A(@Deprecated("") val s: String) {

    constructor(i: Int) : this(i.toString()) {

    }
}

// FILE: use.kt
fun use() {
    A("").<!DEPRECATION!>s<!>
    A(42).<!DEPRECATION!>s<!>
}
