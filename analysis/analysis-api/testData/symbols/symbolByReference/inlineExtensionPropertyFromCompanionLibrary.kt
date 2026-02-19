// DO_NOT_CHECK_SYMBOL_RESTORE_K1
// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
class MyClass {
    companion object {
        inline val Int.extension: Int get() = 0
    }
}

// MODULE: main(library)
// FILE: main.kt
import MyClass.Companion.extension

fun usage() {
    1.extensi<caret>on
}
