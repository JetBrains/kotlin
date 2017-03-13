// WITH_RUNTIME

import kotlin.test.*

var log: String = ""

open class MyClass(val value: String) {
    override fun toString(): String {
        return value
    }
}

inline fun <T> runLogged(entry: String, action: () -> T): T {
    log += entry
    return action()
}

operator fun <T: MyClass> T.provideDelegate(host: Any?, p: Any): T =
        runLogged("tdf(${this.value});") { this }

operator fun <T> T.getValue(receiver: Any?, p: Any): T =
        runLogged("get($this);") { this }

val testO by runLogged("O;") { MyClass("O") }
val testK by runLogged("K;") { "K" }
val testOK = runLogged("OK;") { testO.value + testK }

fun box(): String {
    assertEquals("O;tdf(O);K;OK;get(O);get(K);", log)
    return testOK
}
