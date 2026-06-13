// FILE: 1.kt
package test

var global = ""
var condition = true

inline fun inlineMe(crossinline y: () -> Unit) =
    object {
        inline fun run() { y() }
    }.run()


fun box(): String {
    if (condition) { // to check stack height after inline
        inlineMe { global = "OK" }
    }
    return global
}
