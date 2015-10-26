// IS_APPLICABLE: false
// WITH_RUNTIME
// ERROR: None of the following functions can be called with the arguments supplied: <br>public fun assert(value: kotlin.Boolean): kotlin.Unit defined in kotlin<br>public inline fun assert(value: kotlin.Boolean, lazyMessage: () -> kotlin.Any): kotlin.Unit defined in kotlin<br>@kotlin.Deprecated public fun assert(value: kotlin.Boolean, message: kotlin.Any = ...): kotlin.Unit defined in kotlin

fun foo() {
    <caret>assert()
}