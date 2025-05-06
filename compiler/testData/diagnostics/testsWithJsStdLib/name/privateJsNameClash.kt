// FIR_DIFFERENCE
// This case is only relevant for the JS Legacy BE and is not applicable to the JS IR backend,
// as the IR BE can resolve such name collisions.

package foo

<!JS_NAME_CLASH!>@JsName("bar") private fun foo(x: Int)<!> = x

<!JS_NAME_CLASH!>fun bar()<!> = 42
