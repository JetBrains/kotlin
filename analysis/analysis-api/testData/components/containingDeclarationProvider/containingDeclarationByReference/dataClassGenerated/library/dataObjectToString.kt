// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: data.kt

data object Data

// MODULE: source(lib)
// FILE: usesite.kt

fun foo(d: Data) {
    d.to<caret>String()
}