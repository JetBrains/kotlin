// IGNORE_BACKEND: WASM
class Outer {
    enum class Nested {
        O,
        K
    }
}

fun box() = "${Outer.Nested.O}${Outer.Nested.K}"
