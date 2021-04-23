// FIR_IDENTICAL
// !DIAGNOSTICS: -UNCHECKED_CAST -USELESS_CAST
inline public fun reg(convertFunc: (Any) -> Any) {
    convertFunc("")
}

public inline fun <reified T : Any, reified R : Any> register(converter: (T) -> R) {
    <!USAGE_IS_NOT_INLINABLE!>converter<!> as (Any) -> Any
    reg(<!USAGE_IS_NOT_INLINABLE!>converter<!> as (Any) -> Any)
}
