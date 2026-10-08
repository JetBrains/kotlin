// MODULE: library
// MODULE_KIND: LibraryBinary
// JVM_DEFAULT_MODE: disable
// FILE: Foo.kt
interface Foo {
    fun foo() {}
}

// MODULE: main(library)
// JVM_DEFAULT_MODE: enable
// FILE: main.kt
fun usage(value: <expr>Foo</expr>) {}
