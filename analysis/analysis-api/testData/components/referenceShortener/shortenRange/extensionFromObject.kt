// FILE: main.kt
package test

import dependency.Bar.extFun

fun usage(str: String) {
    <expr>str.extFun()</expr>
}

// FILE: dependency.kt
package dependency

object Bar {
    fun String.extFun() {}
}
