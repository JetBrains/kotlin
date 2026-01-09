// WITH_STDLIB

// FILE: lib.kt
inline fun <L> runLogged(entry: String, action: () -> L): L {
    log += entry
    return action()
}

var log: String = ""

// FILE: main.kt
import kotlin.test.*

open class MyClass(val value: String) {
    override fun toString(): String {
        return value
    }
}

operator fun <P: MyClass> P.provideDelegate(host: Any?, p: Any): P =
        runLogged("tdf(${this.value});") { this }

operator fun <V> V.getValue(receiver: Any?, p: Any): V =
        runLogged("get($this);") { this }

val testO by runLogged("O;") { MyClass("O") }
val testK by runLogged("K;") { "K" }
val testOK = runLogged("OK;") { testO.value + testK }

fun box(): String {
    assertEquals("O;tdf(O);K;OK;get(O);get(K);", log)
    return testOK
}
