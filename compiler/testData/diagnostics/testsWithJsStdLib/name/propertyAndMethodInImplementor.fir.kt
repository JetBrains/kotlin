// FIR_DIFFERENCE
// This case is only relevant for the JS Legacy BE and is not applicable to the JS IR backend,
// as the IR BE can resolve such name collisions.

package foo

interface I {
    fun foo() = 23
}

class Sub : I {
    var foo = 42
}
