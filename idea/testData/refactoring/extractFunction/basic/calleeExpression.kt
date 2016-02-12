// PARAM_DESCRIPTOR: value-parameter y: kotlin.String.() -> kotlin.Unit defined in foo
// PARAM_TYPES: kotlin.String.() -> kotlin.Unit

fun foo(x : String, y : String.() -> Unit) {
    x.<selection>y</selection>()
}