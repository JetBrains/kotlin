// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

import kotlin.test.*

var log: String = ""

class MyClass(val value: String)

inline fun <T> runLogged(entry: String, action: () -> T): T {
    log += entry
    return action()
}

operator fun MyClass.provideDelegate(host: Any?, p: Any): String =
        runLogged("tdf(${this.value});") { this.value }

operator fun String.getValue(receiver: Any?, p: Any): String =
        runLogged("get($this);") { this }

val testO by runLogged("O;") { MyClass("O") }
val testK by runLogged("K;") { "K" }
val testOK = runLogged("OK;") { testO + testK }

fun box(): String {
    assertEquals("O;tdf(O);K;OK;get(O);get(K);", log)
    return testOK
}
