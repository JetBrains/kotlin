// TARGET_BACKEND: JS_IR, JS_IR_ES6
// SPLIT_PER_CLASS
// ES_MODULES

// FILE: a.kt
package sample

@JsExport
fun exported(): String = "exported"

class export {
    fun name() = "class-export"
}

class eager {
    fun name() = "class-eager"
}

class Plain {
    fun name() = "class-plain"
}

// FILE: main.kt
import sample.*

fun box(): String {
    if (exported() != "exported") return "fail: exported()"
    if (export().name() != "class-export") return "fail: export"
    if (eager().name() != "class-eager") return "fail: eager"
    if (Plain().name() != "class-plain") return "fail: Plain"
    return "OK"
}
