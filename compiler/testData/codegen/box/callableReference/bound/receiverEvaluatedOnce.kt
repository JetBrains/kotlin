// WITH_STDLIB

import kotlin.reflect.KFunction1

var x = 0
var hits = 0

class A {
    fun f() = if (x == 1) "OK" else "Fail $x"
}

class Prefixer(private val prefix: String) {
    fun plus(s: String): String = prefix + s
}

fun provider(): Prefixer {
    hits++
    return Prefixer("O")
}

fun callTwice(f: () -> String): String {
    f()
    return f()
}

fun box(): String {
    val ordinaryResult = callTwice(({ x++; A() }())::f)
    if (ordinaryResult != "OK") return ordinaryResult

    val k: KFunction1<String, String> = provider()::plus
    val kResult = k("K")
    if (kResult != "OK") return "Fail KFunction call: $kResult"
    if (hits != 1) return "Fail hits: $hits"
    if (k.name != "plus") return "Fail name: ${k.name}"

    return "OK"
}
