// FIR_DIFFERENCE
// This case is only relevant for the JS Legacy BE and is not applicable to the JS IR backend,
// as the IR BE can resolve such name collisions.

package foo

open class Super {
    <!JS_NAME_CLASH!>val foo<!> = 23
}

class Sub : Super() {
    <!JS_NAME_CLASH!>fun foo()<!> = 42
}
