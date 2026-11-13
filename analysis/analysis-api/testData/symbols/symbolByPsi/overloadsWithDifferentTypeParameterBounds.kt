// KT-91552
// DO_NOT_REQUIRE_DISTINCT_NON_PSI_SYMBOL_POINTERS
package test

interface A
interface B

fun <T : A> foo(x: T) {}
fun <T : B> foo(x: T) {}

fun <T : A, U : B> bar(x: T, y: U) {}
fun <U : A, T : B> bar(x: T, y: U) {}

val <T : A> T.prop: Int get() = 0
val <T : B> T.prop: Int get() = 0

class C {
    fun <T : A> foo(x: T) {}
    fun <T : B> foo(x: T) {}

    val <T : A> T.prop: Int get() = 0
    val <T : B> T.prop: Int get() = 0
}
