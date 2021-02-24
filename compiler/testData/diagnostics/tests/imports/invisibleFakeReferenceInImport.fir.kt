// FILE: A.kt

import B.foo

fun test() {
    <!HIDDEN!>foo<!>
}

// FILE: B.kt
object B : C<String>()

// FILE: C.kt

open class C<T> {
    private var foo: String = "abc"
}
