// FILE: main.kt
package test

import dependency.MyClass
import dependency.Holder

fun usage(holder: Holder) {
    val (one, _: MyClass) = holder
}

// FILE: dependency.kt
package dependency

class MyClass

data class Holder(val one: MyClass, val two: MyClass)
