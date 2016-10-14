// !API_VERSION: 1.0
// MODULE: m1
// FILE: a.kt

package p1

@SinceKotlin("1.1")
fun foo(s: Int): String = s.toString()

// MODULE: m2
// FILE: b.kt

package p2

fun foo(s: Int): Int = s

// MODULE: m3(m1, m2)
// FILE: severalStarImports.kt
import p1.*
import p2.*

fun test1(): Int {
    val r = foo(42)
    return r
}

// FILE: explicitlyImportP1.kt
import p1.foo  // TODO: consider reporting API_NOT_AVAILABLE here
import p2.*

fun test2(): Int {
    val r = foo(42)
    return r
}
