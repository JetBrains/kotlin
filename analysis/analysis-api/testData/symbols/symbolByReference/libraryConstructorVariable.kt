// IGNORE_FE10
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: Lib.kt
package one

class SimpleClass(var constructorVariable: Boolean)

// MODULE: main(lib)
// FILE: usage.kt
fun usage(instance: one.SimpleClass) {
    instance.construc<caret>torVariable
}