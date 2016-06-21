// !DIAGNOSTICS: -UNUSED_VARIABLE
// KT-10968 Callable reference: type inference by function return type

fun <T> getT(): T = null!!

fun getString() = ""

fun test() {
    val a : () -> String = ::getString
    val b : () -> String = ::getT
}
