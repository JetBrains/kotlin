// PARAM_DESCRIPTOR: value-parameter a: kotlin.String defined in foo
// PARAM_TYPES: kotlin.String

fun String.invoke() {

}

fun foo(a: String) {
    <selection>a()</selection>
}