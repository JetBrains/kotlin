// !DIAGNOSTICS: -UNUSED_EXPRESSION
inline public fun reg(converter: (Any) -> Any) {
    converter("")
}

public inline fun register(converter: (Any) -> Any) {
    reg(when(<!USAGE_IS_NOT_INLINABLE!>converter<!>) {
        is (Any) -> Any -> <!USAGE_IS_NOT_INLINABLE!>converter<!>
        else -> <!USAGE_IS_NOT_INLINABLE!>converter<!>
    })
}
