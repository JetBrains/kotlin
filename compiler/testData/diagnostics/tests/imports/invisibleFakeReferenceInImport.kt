// FILE: A.kt

import B.<!INVISIBLE_REFERENCE!>foo<!>

fun test() {
    <!INVISIBLE_MEMBER!>foo<!>
}

// FILE: B.kt
object B : C<String>()

// FILE: C.kt

open class C<T> {
    private var foo: String = "abc"
}