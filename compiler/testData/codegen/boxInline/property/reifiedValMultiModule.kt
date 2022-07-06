// WITH_STDLIB
// WITH_REFLECT
// MODULE: lib
// FILE: lib.kt
package lib

inline val <reified T: Any> T.value: String
    get() = T::class.simpleName!!

// MODULE: main(lib)
// FILE: box.kt
import lib.*

class OK

fun box(): String {
    return OK().value ?: "fail"
}
