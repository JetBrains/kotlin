// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: KT-63864

import kotlin.test.*

var log: String = ""

val dispatcher = hashMapOf<String, String>()

inline fun <T> runLogged(entry: String, action: () -> T): T {
    log += entry
    return action()
}

operator fun String.provideDelegate(host: Any?, p: Any): String  {
    dispatcher[this] = this
    return runLogged("tdf($this);") { this }
}

operator fun String.getValue(receiver: Any?, p: Any): String =
        runLogged("get(${dispatcher[this]});") { dispatcher[this]!! }

operator fun String.setValue(receiver: Any?, p: Any, newValue: String) {
    dispatcher[this] = newValue
    runLogged("set(${dispatcher[this]});") { dispatcher[this]!! }
}

var testO by runLogged("K;") { "K" }
var testK by runLogged("O;") { "O" }
val testOK = runLogged("OK;") {
    testO = "O"
    testK = "K"
    testO + testK
}

fun box(): String {
    assertEquals("K;tdf(K);O;tdf(O);OK;set(O);set(K);get(O);get(K);", log)
    return testOK
}
