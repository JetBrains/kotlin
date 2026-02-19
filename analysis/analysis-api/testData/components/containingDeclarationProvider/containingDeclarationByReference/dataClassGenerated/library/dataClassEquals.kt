// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: data.kt


data class Data(val aaa: Int)

// MODULE: source(lib)
// FILE: usesite.kt

fun foo(d: Data) {
    d.equal<caret>s(d)
}