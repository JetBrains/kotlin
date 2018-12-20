// FILE: 1.kt
// FULL_JDK
// WITH_REFLECT
// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
package test

import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty

var result = "fail"

inline fun <reified T : Any> crashMe(): ReadWriteProperty<Any?, Unit> {
    return Delegates.observable(Unit, { a, b, c -> result = T::class.java.simpleName })
}


// FILE: 2.kt
import test.*


class OK {
    var value by crashMe<OK>()
}

fun box(): String {
    OK().value = Unit
    return result
}