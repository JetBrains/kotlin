// FILE: 1.kt
package test

var global = ""
var condition = true

inline fun inlineMe(crossinline y: () -> Unit) =
    object {
        inline fun run() { y() }
    }.run()

var globalLambda: () -> Unit = {
    global = "OK"
}

fun box(): String {
    if (condition) {
        inlineMe(globalLambda)
    }
    return global
}
