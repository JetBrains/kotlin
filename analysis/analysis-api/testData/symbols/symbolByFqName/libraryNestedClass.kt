// class: one/Producer.NestedClass
// RENDER_IS_PUBLIC_API
// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: Producer.kt
package one

internal open class Producer() {
    class NestedClass() : Producer()
}

// MODULE: main(library)
// FILE: main.kt
fun main() {
}

