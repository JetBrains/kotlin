// FILE: 1.kt
package test

var global = "Fail0"

inline fun inlineMe1(crossinline x: () -> Unit, crossinline y: () -> Unit) =
    inlineMe2(y, x)

inline fun inlineMe2(crossinline x: () -> Unit, crossinline y1: () -> Unit) =
    object {
        inline fun run(y: (Int) -> String) { y1() }
    }.run({ _ -> "Fail2" } )

fun box(): String {
    inlineMe1({ global = "OK" }, { global = "Fail1" })
    return global
}
