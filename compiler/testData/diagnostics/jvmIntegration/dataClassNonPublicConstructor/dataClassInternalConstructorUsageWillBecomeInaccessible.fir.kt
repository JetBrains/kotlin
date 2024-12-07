// MODULE: lib
// FILE: Lib.kt
@ExposedCopyVisibility
data class Foo private constructor(val x: Int) {
    companion object {
        fun new() = Foo(1)
    }
}

// MODULE: main(lib)
// KOTLINC_ARGS: -progressive
// FILE: main.kt
fun main() {
    Foo.new().<!DATA_CLASS_INVISIBLE_COPY_USAGE_ERROR!>copy<!>()
}
