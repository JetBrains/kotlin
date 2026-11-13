// KT-91580
// WITH_STDLIB
// DO_NOT_REQUIRE_DISTINCT_NON_PSI_SYMBOL_POINTERS
package test

fun foo(vararg x: Int) {}

@JvmName("fooArray")
fun foo(x: IntArray) {}

fun foo(vararg x: Int, y: IntArray) {}

@JvmName("fooArrayAndVararg")
fun foo(x: IntArray, vararg y: Int) {}

class C {
    fun foo(vararg x: String) {}

    @JvmName("fooArray")
    fun foo(x: Array<out String>) {}

    fun foo(vararg x: String, y: Array<out String>) {}

    @JvmName("fooArrayAndVararg")
    fun foo(x: Array<out String>, vararg y: String) {}
}
