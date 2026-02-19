// WITH_STDLIB

// FILE: lib.kt
inline fun <T> runLogged(entry: String, action: () -> T): T {
    log += entry
    return action()
}

var log: String = ""

// FILE: main.kt
import kotlin.test.*

operator fun String.provideDelegate(host: Any?, p: Any): String =
        runLogged("tdf($this);") { this }

operator fun String.getValue(receiver: Any?, p: Any): String =
        runLogged("get($this);") { this }

val testO by runLogged("O;") { "O" }

fun box(): String {
    return "OK"
}
