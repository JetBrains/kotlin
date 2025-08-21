// MODULE: library
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
