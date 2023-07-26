// FIR_IDENTICAL
// SKIP_KT_DUMP
import Host.foo

fun withO(fn: (String) -> String) = fn("O")

object Host {
    fun foo(vararg x: String) = "K"
}

fun test1() = withO(::foo)
fun test2() = withO(Host::foo)
