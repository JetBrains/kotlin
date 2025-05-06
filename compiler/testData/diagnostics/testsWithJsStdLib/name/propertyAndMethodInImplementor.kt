// FIR_DIFFERENCE
// This case is only relevant for the JS Legacy BE and is not applicable to the JS IR backend,
// as the IR BE can resolve such name collisions.

package foo

interface I {
    <!JS_NAME_CLASH!>fun foo()<!> = 23
}

class Sub : I {
    <!JS_NAME_CLASH!>var foo<!> = 42
}
