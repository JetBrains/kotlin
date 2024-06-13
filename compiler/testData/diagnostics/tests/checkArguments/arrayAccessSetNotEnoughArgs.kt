// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

class A {
    operator fun set(x: String, y: Boolean, value: Int) {}

    fun d(x: Int) {
        <!NO_VALUE_FOR_PARAMETER("y")!>this[""]<!> = 1
    }
}
