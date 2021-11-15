// WITH_STDLIB
// TARGET_BACKEND: JVM

import kotlin.test.*

var log: String = ""

inline fun <T> runLogged(entry: String, action: () -> T): T {
    log += entry
    return action()
}

operator fun String.provideDelegate(host: Any?, p: Any): String =
        runLogged("tdf($this);") { this }

operator fun String.getValue(receiver: Any?, p: Any): String =
        runLogged("get($this);") { this }

object Test {
    fun foo() {}
    @JvmStatic val testO by runLogged("O;") { "O" }
    @JvmStatic val testK by runLogged("K;") { "K" }
    @JvmStatic val testOK = runLogged("OK;") { testO + testK }
}

fun box(): String {
    assertEquals("", log)
    Test.foo()
    assertEquals("O;tdf(O);K;tdf(K);OK;get(O);get(K);", log)
    return Test.testOK
}
