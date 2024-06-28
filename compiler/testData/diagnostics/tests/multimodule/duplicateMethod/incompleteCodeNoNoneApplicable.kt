// FIR_IDENTICAL
// MODULE: m1
// FILE: a.kt
package p

fun f(s: String, t: String) = s + t

// MODULE: m2
// FILE: b.kt
package p

fun f(s: String, t: String) = t + s

// MODULE: m3(m1, m2)
// FILE: c.kt
import p.f

fun test() {
    // There should be no "none applicable" error here
    f(
<!SYNTAX!><!>}
