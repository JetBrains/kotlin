// DO_NOT_CHECK_SYMBOL_RESTORE_K1
// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
class MyClass {
    companion object {
        inline val property: Int get() = 0
    }
}

// MODULE: main(library)
// FILE: main.kt
import MyClass.Companion.property

fun usage() {
    prope<caret>rty
}