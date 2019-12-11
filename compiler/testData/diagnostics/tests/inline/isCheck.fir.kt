// !DIAGNOSTICS: -UNUSED_EXPRESSION
inline public fun reg(converter: (Any) -> Any, flag: Boolean) {
    flag
    converter("")
}

public inline fun register(converter: (Any) -> Any) {
    converter is (Any) -> Any
    reg(converter, converter is (Any) -> Any)
}