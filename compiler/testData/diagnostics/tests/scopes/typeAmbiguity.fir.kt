// FILE: foo/A.kt
package foo

interface A {
    fun fest() {}
}

// FILE: bar/A.kt
package bar

interface A {
    fun rest() {}
}

// FILE: main.kt
import foo.*
import bar.*

val a: <!AMBIGUOUS_TYPES!>A?<!> = null

fun test() {
    a?.<!UNRESOLVED_REFERENCE!>fest<!>()
    a?.<!UNRESOLVED_REFERENCE!>rest<!>()
}
