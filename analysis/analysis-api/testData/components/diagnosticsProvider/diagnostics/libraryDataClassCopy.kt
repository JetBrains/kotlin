// IGNORE_FE10
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: MyDependency.kt
package one

data class MyDependency(val c: String) {
    val a: String get() = ""
}

// MODULE: main(lib)
// FILE: main.kt
import one.MyDependency

val fgs = MyDependency(c = "")
val dva = fgs.copy(c = "")
val dva2 = fgs.copy(a = "")
