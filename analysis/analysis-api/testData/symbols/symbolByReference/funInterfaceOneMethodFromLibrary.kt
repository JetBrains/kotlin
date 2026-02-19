// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: MyInterface.kt
fun interface MyInterface {
    fun execute()
}

// MODULE: main(library)
// FILE: main.kt
fun usage(i: <caret>MyInterface) {
}
