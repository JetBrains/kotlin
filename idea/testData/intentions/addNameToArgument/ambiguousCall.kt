// IS_APPLICABLE: false
// ERROR: None of the following functions can be called with the arguments supplied: <br>public fun foo(s: kotlin.String, b: kotlin.Boolean, c: kotlin.Char): kotlin.Unit defined in root package<br>public fun foo(s: kotlin.String, b: kotlin.Boolean, p: kotlin.Int): kotlin.Unit defined in root package
fun foo(s: String, b: Boolean, p: Int){}
fun foo(s: String, b: Boolean, c: Char){}

fun bar() {
    foo("", <caret>true)
}