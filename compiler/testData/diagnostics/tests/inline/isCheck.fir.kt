// !DIAGNOSTICS: -UNUSED_EXPRESSION
inline public fun reg(converter: (Any) -> Any, flag: Boolean) {
    flag
    converter("")
}

public inline fun register(converter: (Any) -> Any) {
    <!USAGE_IS_NOT_INLINABLE!>converter<!> is (Any) -> Any
    reg(converter, <!USAGE_IS_NOT_INLINABLE!>converter<!> is (Any) -> Any)
}
