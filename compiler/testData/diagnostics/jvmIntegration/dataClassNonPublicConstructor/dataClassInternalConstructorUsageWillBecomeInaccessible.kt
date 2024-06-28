// MODULE: lib
// FILE: Lib.kt
@<!UNRESOLVED_REFERENCE!>ExposedCopyVisibility<!>
data class Foo private constructor(val x: Int) {
    companion object {
        fun new() = Foo(1)
    }
}

// MODULE: main(lib)
// KOTLINC_ARGS: -progressive
// FILE: main.kt
fun main() {
    <!UNRESOLVED_REFERENCE!>Foo<!>.new().copy()
}
