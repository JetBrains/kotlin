// MODULE: source
// MODULE_KIND: Source
// FILE: data.kt

fun bar() {
    <caret_context>
}

// MODULE: frarmgnet
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: source

// FILE: fragment.kt
// CODE_FRAGMENT_KIND: BLOCK
// CODE_FRAGMENT_RESOLUTION_MODE: IGNORE_SELF

data class Data(val aaa: Int)

fun foo(d: Data) {
    d.copy(a<caret>aa = 1)
}
