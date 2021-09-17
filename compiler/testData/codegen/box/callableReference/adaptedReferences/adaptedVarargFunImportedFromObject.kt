

import Host.foo

fun withO(fn: (String) -> String) = fn("O")

object Host {
    fun foo(vararg x: String) = x[0] + "K"
}

fun box() = withO(::foo)
