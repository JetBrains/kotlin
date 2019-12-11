// !DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: a.kt
package a

class A {
    class B
}

// FILE: b.kt
package a

class D {
    class B
}

// FILE: c.kt
import a.A.B
import a.D.B

fun test(b: B) {
    B()
}

// FILE: d.kt
import a.A.*
import a.D.*

// todo ambiguvity here
fun test2(b: B) {
    B()
}