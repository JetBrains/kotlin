// FIR_DIFFERENCE
// This case is only relevant for the JS Legacy BE and is not applicable to the JS IR backend,
// as the IR BE can resolve such name collisions.

package foo

open class Super {
    fun foo() = 23
}

class Sub : Super() {
    @JsName("foo") fun bar() = 42
}
