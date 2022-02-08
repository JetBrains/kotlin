// WITH_STDLIB

import kotlin.test.*
import kotlin.reflect.KProperty

var log: String = ""

inline fun <T> runLogged(entry: String, action: () -> T): T {
    log += entry
    return action()
}

operator fun String.provideDelegate(host: Any?, p: KProperty<*>): String =
        if (p.name == this) runLogged("tdf($this);") { this } else "fail 1"

operator fun String.getValue(receiver: Any?, p: KProperty<*>): String =
        if (p.name == this) runLogged("get($this);") { this } else "fail 2"

val O by runLogged("O;") { "O" }
val K by runLogged("K;") { "K" }
val OK = runLogged("OK;") { O + K }

fun box(): String {
    assertEquals("O;tdf(O);K;tdf(K);OK;get(O);get(K);", log)
    return OK
}
