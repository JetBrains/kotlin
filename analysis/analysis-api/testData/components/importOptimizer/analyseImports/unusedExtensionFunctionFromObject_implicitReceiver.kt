// FILE: main.kt
package test

import dependency.Bar.extFun

fun usage() {
    with(dependency.Bar) {
        with(10) {
            extFun()
        }
    }
}

// FILE: dependency.kt
package dependency

object Bar {
    fun Int.extFun() {}
}
