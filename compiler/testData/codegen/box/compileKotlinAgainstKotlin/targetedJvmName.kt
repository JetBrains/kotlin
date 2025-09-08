// TARGET_BACKEND: JVM
// WITH_STDLIB

// JVM_ABI_K1_K2_DIFF: KT-63984, KT-69075

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
