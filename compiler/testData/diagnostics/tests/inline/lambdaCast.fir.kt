// !DIAGNOSTICS: -UNCHECKED_CAST -USELESS_CAST
inline public fun reg(convertFunc: (Any) -> Any) {
    convertFunc("")
}

public inline fun <reified T : Any, reified R : Any> register(converter: (T) -> R) {
    converter as (Any) -> Any
    reg(converter as (Any) -> Any)
}