// TARGET_BACKEND: JVM
// FILE: A.kt
package lib

@set:JvmName("renamedSetFoo")
@get:JvmName("renamedGetFoo")
var foo = "not set"

// FILE: B.kt
import lib.*

fun box(): String {
    foo = "OK"
    return foo
}