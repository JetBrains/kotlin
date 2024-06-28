// IGNORE_FE10
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: MyDependency.kt
package one

data class MyDependency(val a: String) {
    val c: String get() = ""
}

// MODULE: main(lib)
// FILE: main.kt
import one.MyDependency

val fgs = MyDependency(a = "")
val dva = fgs.copy(a = "")
val dva2 = fgs.copy(c = "")
