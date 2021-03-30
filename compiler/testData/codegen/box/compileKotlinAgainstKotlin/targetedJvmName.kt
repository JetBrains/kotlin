// TARGET_BACKEND: JVM
// WITH_STDLIB

// MODULE: lib
// FILE: A.kt
package lib

@set:JvmName("renamedSetFoo")
@get:JvmName("renamedGetFoo")
var foo = "not set"

// MODULE: main(lib)
// FILE: B.kt
import lib.*

fun box(): String {
    foo = "OK"
    return foo
}
