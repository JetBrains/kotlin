// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

object ContentTypeByExtension {
    inline fun processRecords(crossinline operation: (String) -> String) =
             {
                val ext = B("OK")
                operation(ext.toLowerCase())
            }()
}




inline fun A.toLowerCase(): String = (this as B).value

open class A

open class B(val value: String) : A()

// FILE: 2.kt

import test.*

fun box(): String {
    return ContentTypeByExtension.processRecords { ext -> ext }
}
