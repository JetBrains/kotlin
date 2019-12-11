// !DIAGNOSTICS: -UNUSED_EXPRESSION
inline public fun reg(converter: (Any) -> Any) {
    converter("")
}

public inline fun register(converter: (Any) -> Any) {
    reg(when(converter) {
        is (Any) -> Any -> converter
        else -> converter
    })
}