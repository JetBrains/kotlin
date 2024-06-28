// IGNORE_FE10
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: Lib.kt
package one

var topLevelVariable: Boolean = true

// MODULE: main(lib)
// FILE: usage.kt
fun usage() {
    one.topLe<caret>velVariable
}