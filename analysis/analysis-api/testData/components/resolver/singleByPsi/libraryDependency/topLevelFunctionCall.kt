// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: foo.kt
fun foo(a: Int, b: String) {}

// MODULE: main(library)
// FILE: main.kt
fun call() {
    <expr>foo(1, "foo")</expr>
}
