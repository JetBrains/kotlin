// FIR_DIFFERENCE
// This case is only relevant for the JS Legacy BE and is not applicable to the JS IR backend,
// as the IR BE can resolve such name collisions.

package foo

class <!JS_NAME_CLASH!>A(val x: Int)<!>

<!JS_NAME_CLASH!>fun A()<!> {}
