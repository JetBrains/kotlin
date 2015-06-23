// PARAM_DESCRIPTOR: value-parameter val a: kotlin.String defined in foo
// PARAM_DESCRIPTOR: value-parameter val invoke: kotlin.String.() -> kotlin.Unit defined in foo
// PARAM_TYPES: kotlin.String
// PARAM_TYPES: kotlin.String.() -> kotlin.Unit

fun foo(a: String, invoke: String.() -> Unit) {
    <selection>a.invoke()</selection>
}