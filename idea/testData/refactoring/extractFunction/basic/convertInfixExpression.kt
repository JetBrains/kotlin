// PARAM_DESCRIPTOR: value-parameter x: kotlin.String.(kotlin.String) -> kotlin.Unit defined in foo
// PARAM_TYPES: kotlin.String.(kotlin.String) -> kotlin.Unit

fun foo(x : String.(String) -> Unit) {
    "A" <selection>x</selection> "B"
}