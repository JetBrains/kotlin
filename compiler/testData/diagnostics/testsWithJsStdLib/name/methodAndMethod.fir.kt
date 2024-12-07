// FIR_DIFFERENCE
// This case is only relevant for the JS Legacy BE and is not applicable to the JS IR backend,
// as the IR BE can resolve such name collisions. Furthermore, the IR BE mangles names differently.
package foo

fun bar(x: Int) = x

fun `bar_za3lpa$`() = 42
