// DIAGNOSTICS: -UNUSED_EXPRESSION
inline public fun reg(converter: (Any) -> Any) {
    converter("")
}

public inline fun register(converter: (Any) -> Any) {
    reg(when(<!USAGE_IS_NOT_INLINABLE!>converter<!>) {
        <!USELESS_IS_CHECK!>is (Any) -> Any<!> -> <!USAGE_IS_NOT_INLINABLE!>converter<!>
        <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> <!USAGE_IS_NOT_INLINABLE!>converter<!>
    })
}