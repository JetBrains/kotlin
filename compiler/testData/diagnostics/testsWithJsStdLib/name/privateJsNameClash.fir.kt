// FIR_DIFFERENCE
// This case is only relevant for the JS Legacy BE and is not applicable to the JS IR backend,
// as the IR BE can resolve such name collisions.

package foo

@JsName("bar") private fun foo(x: Int) = x

fun bar() = 42
