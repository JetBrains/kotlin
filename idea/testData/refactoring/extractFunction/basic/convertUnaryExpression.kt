// PARAM_DESCRIPTOR: value-parameter val plus: kotlin.String.() -> kotlin.Unit defined in foo
// PARAM_TYPES: kotlin.String.() -> kotlin.Unit

fun foo(plus: String.() -> Unit) {
    <selection>+</selection> "A"
}