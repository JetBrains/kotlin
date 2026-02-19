// IGNORE_FE10
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: Lib.kt
package one

@Anno
@get:Anno
@set:Anno
@setparam:Anno
var topLevelVariableWithAnnotations: Long = 0L

annotation class Anno

// MODULE: main(lib)
// FILE: usage.kt
fun usage() {
    one.topLevelV<caret>ariableWithAnnotations
}