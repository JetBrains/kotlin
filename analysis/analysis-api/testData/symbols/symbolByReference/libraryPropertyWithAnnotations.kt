// IGNORE_FE10
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: Lib.kt
package one

@Anno
@get:Anno
val topLevelPropertyWithAnnotations: Short = 0

annotation class Anno

// MODULE: main(lib)
// FILE: usage.kt
fun usage() {
    one.topLev<caret>elPropertyWithAnnotations
}