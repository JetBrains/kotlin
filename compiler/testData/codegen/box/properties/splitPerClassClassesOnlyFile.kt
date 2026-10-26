// TARGET_BACKEND: JS_IR, JS_IR_ES6
// SPLIT_PER_CLASS
// ES_MODULES

// FILE: a.kt
package sample

class OnlyClassesA {
    fun name() = "A"
}

class OnlyClassesB {
    fun name() = "B"
}

// FILE: main.kt
import sample.*

fun box(): String {
    if (OnlyClassesA().name() != "A") return "fail: OnlyClassesA"
    if (OnlyClassesB().name() != "B") return "fail: OnlyClassesB"
    return "OK"
}
