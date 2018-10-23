// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME

import kotlin.test.*

var log: String = ""

class MyClass(val value: String)

fun runLogged(entry: String, action: () -> String): String {
    log += entry
    return action()
}

fun runLogged2(entry: String, action: () -> MyClass): MyClass {
    log += entry
    return action()
}

operator fun MyClass.provideDelegate(host: Any?, p: Any): String =
        runLogged("tdf(${this.value});") { this.value }

operator fun String.getValue(receiver: Any?, p: Any): String =
        runLogged("get($this);") { this }


fun box(): String {
    val testO by runLogged2("O;") { MyClass("O") }
    val testK by runLogged("K;") { "K" }
    val testOK = runLogged("OK;") { testO + testK }

    assertEquals("O;tdf(O);K;OK;get(O);get(K);", log)
    return testOK
}
