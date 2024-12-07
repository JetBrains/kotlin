// FIR_IDENTICAL
package test

inline fun foo(x: () -> Unit) {}

fun String.id(s: String = this, vararg xs: Int): String = s

fun test() {
    foo("Fail"::id)
}
