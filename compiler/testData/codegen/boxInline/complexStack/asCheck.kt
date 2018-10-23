// FILE: 1.kt
// WITH_RUNTIME
package test

object ContentTypeByExtension {
    inline fun processRecords(crossinline operation: (String) -> String) =
            listOf("O", "K").map {
                val ext = B(it)
                operation(ext.toLowerCase())
            }.joinToString("")
}




inline fun A.toLowerCase(): String = (this as B).value

open class A

open class B(val value: String) : A()

// FILE: 2.kt

import test.*

fun box(): String {
    return ContentTypeByExtension.processRecords { ext -> ext }
}
