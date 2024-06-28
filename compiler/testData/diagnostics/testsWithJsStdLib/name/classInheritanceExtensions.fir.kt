// FIR_DIFFERENCE
// This case is only relevant for the JS Legacy BE and is not applicable to the JS IR backend,
// as the IR BE can resolve such name collisions.

open class Class {
    fun Int.test() {}
    val Int.test
        get() = 0
}

class MyClass1 : Class()
