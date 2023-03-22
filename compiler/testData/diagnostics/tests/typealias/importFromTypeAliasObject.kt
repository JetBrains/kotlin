// IGNORE_REVERSED_RESOLVE
// FIR_IDENTICAL
// FILE: 1.kt
package bar

typealias HostAlias = Host

object Host {
    fun foo() {}
}

// FILE: 2.kt
import bar.HostAlias.foo

fun test() {
    foo()
}