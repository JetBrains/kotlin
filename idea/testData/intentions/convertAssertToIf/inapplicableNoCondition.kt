// IS_APPLICABLE: false
// WITH_RUNTIME
// ERROR: None of the following functions can be called with the arguments supplied: <br>@kotlin.internal.InlineOnly public inline fun assert(value: kotlin.Boolean): kotlin.Unit defined in kotlin<br>@kotlin.internal.InlineOnly public inline fun assert(value: kotlin.Boolean, lazyMessage: () -> kotlin.Any): kotlin.Unit defined in kotlin

fun foo() {
    <caret>assert()
}