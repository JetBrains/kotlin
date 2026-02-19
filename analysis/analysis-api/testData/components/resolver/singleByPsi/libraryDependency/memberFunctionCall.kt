// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: Bar.kt
class Bar {
    fun foo(a: Int, b: String) {}
}

// MODULE: main(library)
// FILE: main.kt
fun call() {
    val bar = Bar()
    bar.<expr>foo(1, "foo")</expr>
}
