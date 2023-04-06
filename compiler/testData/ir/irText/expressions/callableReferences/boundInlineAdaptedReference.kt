// MUTE_SIGNATURE_COMPARISON_K2: JVM_IR
// ^ KT-57433

package test

inline fun foo(x: () -> Unit) {}

fun String.id(s: String = this, vararg xs: Int): String = s

fun test() {
    foo("Fail"::id)
}
