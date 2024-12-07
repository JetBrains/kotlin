// IGNORE_FE10
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: Lib.kt
package one

class SimpleClass {
    var memberVariable: Boolean = true
}

// MODULE: main(lib)
// FILE: usage.kt
fun usage(instance: one.SimpleClass) {
    instance.memberV<caret>ariable
}