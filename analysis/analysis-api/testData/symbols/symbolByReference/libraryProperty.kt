// IGNORE_FE10
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: Lib.kt
package one

val topLevelProperty: String = ""

// MODULE: main(lib)
// FILE: usage.kt
fun usage() {
    one.topLevelP<caret>roperty
}