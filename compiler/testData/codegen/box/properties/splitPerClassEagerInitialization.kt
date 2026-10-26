// TARGET_BACKEND: JS_IR, JS_IR_ES6
// SPLIT_PER_CLASS
// ES_MODULES

// FILE: a.kt
package sample

@OptIn(kotlin.ExperimentalStdlibApi::class)
@EagerInitialization
var eagerCounter = "unset"

class Greeter {
    fun greet() = "hello"
}

@OptIn(kotlin.ExperimentalStdlibApi::class)
@EagerInitialization
val eagerGreeting = Greeter().greet()

fun readEager(): String = "$eagerCounter $eagerGreeting"

// FILE: main.kt
import sample.*

fun box(): String {
    if (eagerCounter != "unset") return "fail: eagerCounter = $eagerCounter"
    if (eagerGreeting != "hello") return "fail: eagerGreeting = $eagerGreeting"
    if (readEager() != "unset hello") return "fail: readEager = ${readEager()}"
    return "OK"
}
