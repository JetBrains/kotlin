// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

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

fun box(): String {
    val testO by runLogged("O;") { "O" }
    val testK by runLogged("K;") { "K" }
    val testOK = runLogged("OK;") { testO + testK }

    assertEquals("O;tdf(O);K;tdf(K);OK;get(O);get(K);", log)
    return testOK
}
