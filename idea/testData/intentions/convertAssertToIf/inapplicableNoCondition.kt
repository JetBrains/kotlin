// IS_APPLICABLE: false
// WITH_RUNTIME
// ERROR: None of the following functions can be called with the arguments supplied: <br>@InlineOnly public inline fun assert(value: Boolean): Unit defined in kotlin<br>@InlineOnly public inline fun assert(value: Boolean, lazyMessage: () -> Any): Unit defined in kotlin

fun foo() {
    <caret>assert()
}